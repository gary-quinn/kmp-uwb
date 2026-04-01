@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Angle
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.Vector128
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import platform.Foundation.create
import platform.NearbyInteraction.NIAlgorithmConvergence
import platform.NearbyInteraction.NIDiscoveryToken
import platform.NearbyInteraction.NINearbyObject
import platform.NearbyInteraction.NINearbyObjectRemovalReason
import platform.NearbyInteraction.NINearbyPeerConfiguration
import platform.NearbyInteraction.NISession
import platform.NearbyInteraction.NISessionDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosRangingSession(
    override val config: RangingConfig,
    private val existingSession: NISession,
) : RangingSession {
    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default.limitedParallelism(1) + CoroutineName("UwbRanging"),
        )

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Ready)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    private val resultChannel =
        Channel<RangingResult>(
            capacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()

    private var niSession: NISession? = existingSession
    private val delegate = SessionDelegate()
    private val tokenCache = DiscoveryTokenCache()

    internal suspend fun startRanging(remoteParams: SessionParams) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }

        _state.value = RangingState.Starting.Negotiating

        val session = niSession ?: error("NISession not initialized")

        _state.value = RangingState.Starting.Initializing

        val peerToken = deserializeDiscoveryToken(remoteParams.toByteArray())
        val configuration = NINearbyPeerConfiguration(peerToken)

        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { session.invalidate() }
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                try {
                    session.delegate = delegate
                    session.runWithConfiguration(configuration)
                    cont.resume(Unit)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    override fun close() {
        val session = niSession
        niSession = null
        if (session != null) {
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                session.invalidate()
            }
        }
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        resultChannel.close()
        scope.cancel()
    }

    private inner class SessionDelegate :
        NSObject(),
        NISessionDelegateProtocol {
        override fun session(
            session: NISession,
            didUpdateNearbyObjects: List<*>,
        ) {
            val nearbyObjects = didUpdateNearbyObjects.filterIsInstance<NINearbyObject>()
            for (obj in nearbyObjects) {
                val rawDistance = obj.distance.toDouble()
                val measurement =
                    RangingMeasurement(
                        distance = if (rawDistance >= 0.0) Distance.meters(rawDistance) else null,
                        azimuth = extractAzimuth(obj.direction),
                        elevation = extractElevation(obj.direction),
                    )

                val peer = Peer(address = tokenCache.resolve(obj.discoveryToken))

                resultChannel.trySend(RangingResult.Position(peer, measurement))
            }

            scope.launch {
                if (_state.value !is RangingState.Active.Ranging) {
                    _state.value = RangingState.Active.Ranging
                }
            }
        }

        override fun session(
            session: NISession,
            didRemoveNearbyObjects: List<*>,
            withReason: NINearbyObjectRemovalReason,
        ) {
            val removedObjects = didRemoveNearbyObjects.filterIsInstance<NINearbyObject>()
            for (obj in removedObjects) {
                val peer = Peer(address = tokenCache.resolve(obj.discoveryToken))
                resultChannel.trySend(RangingResult.PeerLost(peer))
            }

            scope.launch { _state.value = RangingState.Active.PeerLost }
        }

        override fun sessionWasSuspended(session: NISession) {
            scope.launch { _state.value = RangingState.Active.Suspended }
        }

        override fun sessionSuspensionEnded(session: NISession) {
            scope.launch { _state.value = RangingState.Active.Ranging }
        }

        override fun session(
            session: NISession,
            didInvalidateWithError: platform.Foundation.NSError,
        ) {
            val error =
                SessionLost(
                    message = "NearbyInteraction session invalidated: ${didInvalidateWithError.localizedDescription}",
                )
            scope.launch { _state.value = RangingState.Stopped.ByError(error) }
            resultChannel.close()
        }

        override fun session(
            session: NISession,
            didUpdateAlgorithmConvergence: NIAlgorithmConvergence,
            forObject: NINearbyObject?,
        ) = Unit
    }
}

/**
 * Caches NIDiscoveryToken → PeerAddress mappings for the session's lifetime.
 *
 * NSKeyedArchiver serialization is expensive (~0.1ms). Without caching,
 * it runs on every didUpdateNearbyObjects callback (~5Hz per peer).
 * The token-to-address mapping is stable — the same token always produces
 * the same bytes — so caching is safe.
 *
 * Thread safety: all delegate callbacks run on Apple's dispatch queue,
 * and this cache is only accessed from those callbacks. No synchronization needed.
 */
private class DiscoveryTokenCache {
    private val cache = mutableMapOf<NIDiscoveryToken, PeerAddress>()

    fun resolve(token: NIDiscoveryToken): PeerAddress =
        cache.getOrPut(token) {
            PeerAddress(serializeDiscoveryToken(token))
        }
}

/**
 * NINearbyObject.direction is a simd_float3 mapped to [Vector128] in K/N.
 * Coordinate system: x (index 0) = left/right, y (index 1) = up/down, z (index 2) = forward.
 * Returns null when the direction vector is zero (device outside U1/U2 field of view).
 */
private fun extractAzimuth(direction: Vector128): Angle? {
    val x = direction.getFloatAt(0)
    val z = direction.getFloatAt(2)
    if (x == 0f && z == 0f) return null
    return Angle.degrees(
        kotlin.math.atan2(x.toDouble(), z.toDouble()) * (180.0 / kotlin.math.PI),
    )
}

private fun extractElevation(direction: Vector128): Angle? {
    val x = direction.getFloatAt(0)
    val y = direction.getFloatAt(1)
    val z = direction.getFloatAt(2)
    val horizontalDistance = kotlin.math.sqrt((x * x + z * z).toDouble())
    if (horizontalDistance == 0.0 && y == 0f) return null
    return Angle.degrees(
        kotlin.math.atan2(y.toDouble(), horizontalDistance) * (180.0 / kotlin.math.PI),
    )
}

private const val VERSION_IOS: Byte = 0x81.toByte()

internal fun serializeDiscoveryToken(token: NIDiscoveryToken): ByteArray {
    val tokenData: NSData =
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val data =
                NSKeyedArchiver.archivedDataWithRootObject(
                    `object` = token,
                    requiringSecureCoding = true,
                    error = error.ptr,
                )
            if (data == null) {
                val desc = error.value?.localizedDescription ?: "unknown error"
                error("NSKeyedArchiver failed to serialize NIDiscoveryToken: $desc")
            }
            data
        }
    val tokenBytes = tokenData.toByteArray()
    return byteArrayOf(VERSION_IOS) + tokenBytes
}

internal fun deserializeDiscoveryToken(bytes: ByteArray): NIDiscoveryToken {
    require(bytes.isNotEmpty()) { "SessionParams is empty" }
    require(bytes[0] == VERSION_IOS) {
        "Incompatible SessionParams version: 0x${bytes[0].toUByte().toString(16)} (expected iOS 0x81)"
    }
    val tokenBytes = bytes.copyOfRange(1, bytes.size)
    val data = tokenBytes.toNSData()
    val token: NIDiscoveryToken =
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val obj =
                NSKeyedUnarchiver.unarchivedObjectOfClass(
                    cls = NIDiscoveryToken,
                    fromData = data,
                    error = error.ptr,
                )
            if (obj == null) {
                val desc = error.value?.localizedDescription ?: "unknown error"
                error(
                    "Failed to deserialize NIDiscoveryToken from ${tokenBytes.size} bytes: $desc — " +
                        "data may be corrupted during OOB transfer",
                )
            }
            obj as NIDiscoveryToken
        }
    return token
}

internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return byteArrayOf()
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
