package com.atruedev.kmpuwb.session

import android.content.Context
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingPosition
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import androidx.core.uwb.UwbManager
import com.atruedev.kmpuwb.adapter.KmpUwb
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Angle
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow

internal class AndroidRangingSession(
    override val config: RangingConfig,
    private val context: Context,
) : RangingSession {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Ready)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    override suspend fun start(peer: Peer) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }
        _state.value = RangingState.Starting.Negotiating
    }

    override val rangingResults: Flow<RangingResult> = channelFlow {
        val uwbManager = UwbManager.createInstance(context)

        try {
            val sessionScope = when (config.role) {
                RangingRole.CONTROLLER -> uwbManager.controllerSessionScope()
                RangingRole.CONTROLEE -> uwbManager.controleeSessionScope()
            }

            _state.value = RangingState.Starting.Initializing

            val peerDevices = listOf(UwbDevice(UwbAddress(byteArrayOf(0, 0))))

            val complexChannel = if (sessionScope is UwbControllerSessionScope) {
                sessionScope.uwbComplexChannel
            } else {
                UwbComplexChannel(config.channel, 0)
            }

            val rangingParameters = RangingParameters(
                uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
                sessionId = config.sessionId,
                sessionKeyInfo = config.sessionKey ?: byteArrayOf(),
                complexChannel = complexChannel,
                peerDevices = peerDevices,
                updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
                subSessionId = 0,
                subSessionKeyInfo = null,
            )

            _state.value = RangingState.Active.Ranging

            sessionScope.prepareSession(rangingParameters).collect { rangingResult ->
                val result = rangingResult.toRangingResult()
                if (result != null) {
                    send(result)
                }
            }
        } catch (e: Exception) {
            val error = SessionLost(
                message = "Android ranging session failed: ${e.message}",
                cause = e,
            )
            _state.value = RangingState.Stopped.ByError(error)
        }
    }

    override fun close() {
        _state.value = RangingState.Stopped.ByRequest
        scope.cancel()
    }

    private fun androidx.core.uwb.RangingResult.toRangingResult(): RangingResult? =
        when (this) {
            is androidx.core.uwb.RangingResult.RangingResultPosition -> {
                val position = this.position
                RangingResult.Position(
                    peer = Peer(address = PeerAddress(this.device.address.address)),
                    measurement = position.toMeasurement(),
                )
            }
            is androidx.core.uwb.RangingResult.RangingResultPeerDisconnected -> {
                RangingResult.PeerLost(
                    peer = Peer(address = PeerAddress(this.device.address.address)),
                )
            }
            else -> null
        }

    private fun RangingPosition.toMeasurement(): RangingMeasurement =
        RangingMeasurement(
            distance = Distance.meters(this.distance?.value?.toDouble() ?: 0.0),
            azimuth = this.azimuth?.let { Angle.degrees(it.value.toDouble()) },
            elevation = this.elevation?.let { Angle.degrees(it.value.toDouble()) },
        )
}

public actual fun RangingSession(config: RangingConfig): RangingSession {
    val context = KmpUwb.requireContext()
    return AndroidRangingSession(config, context)
}
