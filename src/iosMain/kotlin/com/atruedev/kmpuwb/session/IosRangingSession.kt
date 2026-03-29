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
import kotlinx.cinterop.Vector128
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
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
import platform.Foundation.NSData
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

internal class IosRangingSession(
    override val config: RangingConfig,
) : RangingSession {
    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
        )

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Ready)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    private val resultChannel =
        Channel<RangingResult>(
            capacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()

    internal var niSession: NISession? = null
    private val delegate = SessionDelegate()

    override suspend fun start(peer: Peer) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }

        _state.value = RangingState.Starting.Negotiating

        scope.launch {
            val session = niSession ?: NISession()
            session.delegate = delegate
            niSession = session

            _state.value = RangingState.Starting.Initializing
        }
    }

    /**
     * Start ranging with a remote peer's discovery token.
     * Decodes the token from [SessionParams] and calls [NISession.runWithConfiguration].
     */
    internal fun startWithRemoteToken(remoteParams: SessionParams) {
        val session = niSession ?: error("NISession not initialized")
        val peerToken = deserializeDiscoveryToken(remoteParams.toByteArray())
        val configuration = NINearbyPeerConfiguration(peerToken)
        session.runWithConfiguration(configuration)
    }

    override fun close() {
        niSession?.invalidate()
        niSession = null
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        resultChannel.close()
        scope.cancel()
    }

    private inner class SessionDelegate : NSObject(), NISessionDelegateProtocol {
        override fun session(
            session: NISession,
            didUpdateNearbyObjects: List<*>,
        ) {
            val nearbyObjects = didUpdateNearbyObjects.filterIsInstance<NINearbyObject>()
            for (obj in nearbyObjects) {
                val measurement =
                    RangingMeasurement(
                        distance = Distance.meters(obj.distance.toDouble()),
                        azimuth = extractAzimuth(obj.direction),
                        elevation = extractElevation(obj.direction),
                    )

                val peer =
                    Peer(
                        address = PeerAddress.fromDiscoveryToken(obj.discoveryToken),
                    )

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
                val peer =
                    Peer(
                        address = PeerAddress.fromDiscoveryToken(obj.discoveryToken),
                    )
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

internal fun serializeDiscoveryToken(token: NIDiscoveryToken): ByteArray {
    val data: NSData = NSKeyedArchiver.archivedDataWithRootObject(token)
    return data.toByteArray()
}

internal fun deserializeDiscoveryToken(bytes: ByteArray): NIDiscoveryToken {
    val data = bytes.toNSData()
    return NSKeyedUnarchiver.unarchiveObjectWithData(data) as NIDiscoveryToken
}

private fun PeerAddress.Companion.fromDiscoveryToken(token: NIDiscoveryToken): PeerAddress = PeerAddress(serializeDiscoveryToken(token))

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

public actual fun RangingSession(config: RangingConfig): RangingSession = IosRangingSession(config)
