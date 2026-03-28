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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import platform.NearbyInteraction.NIAlgorithmConvergence
import platform.NearbyInteraction.NINearbyObject
import platform.NearbyInteraction.NINearbyObjectRemovalReason
import platform.NearbyInteraction.NISession
import platform.NearbyInteraction.NISessionDelegateProtocol
import platform.darwin.NSObject

internal class IosRangingSession(
    override val config: RangingConfig,
) : RangingSession {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Ready)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    private val resultChannel = Channel<RangingResult>(capacity = Channel.BUFFERED)
    override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()

    private var niSession: NISession? = null
    private val delegate = SessionDelegate()

    override suspend fun start(peer: Peer) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }

        _state.value = RangingState.Starting.Negotiating

        niSession = NISession().apply {
            this.delegate = this@IosRangingSession.delegate
        }

        _state.value = RangingState.Starting.Initializing
    }

    override fun close() {
        niSession?.invalidate()
        niSession = null
        _state.value = RangingState.Stopped.ByRequest
        resultChannel.close()
        scope.cancel()
    }

    @Suppress("CONFLICTING_OVERLOADS")
    private inner class SessionDelegate : NSObject(), NISessionDelegateProtocol {

        override fun session(session: NISession, didUpdateNearbyObjects: List<*>) {
            val nearbyObjects = didUpdateNearbyObjects.filterIsInstance<NINearbyObject>()
            for (obj in nearbyObjects) {
                val measurement = RangingMeasurement(
                    distance = Distance.meters(obj.distance.toDouble()),
                    azimuth = extractAzimuth(obj.direction),
                    elevation = extractElevation(obj.direction),
                )

                val peer = Peer(
                    address = PeerAddress(byteArrayOf()),
                )

                resultChannel.trySend(RangingResult.Position(peer, measurement))
            }

            if (_state.value !is RangingState.Active.Ranging) {
                _state.value = RangingState.Active.Ranging
            }
        }

        override fun session(
            session: NISession,
            didRemoveNearbyObjects: List<*>,
            withReason: NINearbyObjectRemovalReason,
        ) {
            val removedObjects = didRemoveNearbyObjects.filterIsInstance<NINearbyObject>()
            for (obj in removedObjects) {
                val peer = Peer(address = PeerAddress(byteArrayOf()))
                resultChannel.trySend(RangingResult.PeerLost(peer))
            }

            _state.value = RangingState.Active.PeerLost
        }

        override fun sessionWasSuspended(session: NISession) {
            _state.value = RangingState.Active.Suspended
        }

        override fun sessionSuspensionEnded(session: NISession) {
            _state.value = RangingState.Active.Ranging
        }

        override fun session(
            session: NISession,
            didInvalidateWithError: platform.Foundation.NSError,
        ) {
            val error = SessionLost(
                message = "NearbyInteraction session invalidated: ${didInvalidateWithError.localizedDescription}",
            )
            _state.value = RangingState.Stopped.ByError(error)
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
 * Extracts azimuth (horizontal angle) from a NearbyInteraction direction vector.
 *
 * NINearbyObject.direction is a simd_float3 mapped to [Vector128] in K/N.
 * Components: x (index 0) = left/right, y (index 1) = up/down, z (index 2) = forward.
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

/**
 * Extracts elevation (vertical angle) from a NearbyInteraction direction vector.
 *
 * Returns null when the direction vector is zero.
 */
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

public actual fun RangingSession(config: RangingConfig): RangingSession =
    IosRangingSession(config)
