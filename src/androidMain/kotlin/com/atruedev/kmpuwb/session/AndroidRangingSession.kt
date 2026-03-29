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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

internal class AndroidRangingSession(
    override val config: RangingConfig,
    private val context: Context,
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

    private var rangingJob: Job? = null

    override suspend fun start(peer: Peer) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }

        _state.value = RangingState.Starting.Negotiating

        rangingJob =
            scope.launch {
                startRangingWithPeer(peer)
            }
    }

    override fun close() {
        rangingJob?.cancel()
        rangingJob = null
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        resultChannel.close()
        scope.cancel()
    }

    private suspend fun startRangingWithPeer(peer: Peer) {
        try {
            val uwbManager = UwbManager.createInstance(context)

            val sessionScope =
                when (config.role) {
                    RangingRole.CONTROLLER -> uwbManager.controllerSessionScope()
                    RangingRole.CONTROLEE -> uwbManager.controleeSessionScope()
                }

            _state.value = RangingState.Starting.Initializing

            val rangingParameters = buildRangingParameters(sessionScope, peer)

            _state.value = RangingState.Active.Ranging

            sessionScope.prepareSession(rangingParameters).collect { platformResult ->
                val result = platformResult.toRangingResult()
                if (result != null) {
                    resultChannel.trySend(result)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error =
                SessionLost(
                    message = "Android ranging session failed: ${e.message}",
                    cause = e,
                )
            _state.value = RangingState.Stopped.ByError(error)
            resultChannel.close()
        }
    }

    private fun buildRangingParameters(
        sessionScope: androidx.core.uwb.UwbClientSessionScope,
        peer: Peer,
    ): RangingParameters {
        val peerDevices = listOf(UwbDevice(UwbAddress(peer.address.toByteArray())))

        val complexChannel =
            if (sessionScope is UwbControllerSessionScope) {
                sessionScope.uwbComplexChannel
            } else {
                UwbComplexChannel(config.channel, 0)
            }

        return RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = config.sessionId,
            sessionKeyInfo = config.sessionKey ?: byteArrayOf(),
            complexChannel = complexChannel,
            peerDevices = peerDevices,
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
            subSessionId = 0,
            subSessionKeyInfo = null,
        )
    }

    private fun androidx.core.uwb.RangingResult.toRangingResult(): RangingResult? =
        when (this) {
            is androidx.core.uwb.RangingResult.RangingResultPosition ->
                RangingResult.Position(
                    peer = Peer(address = PeerAddress(this.device.address.address)),
                    measurement = this.position.toMeasurement(),
                )
            is androidx.core.uwb.RangingResult.RangingResultPeerDisconnected ->
                RangingResult.PeerLost(
                    peer = Peer(address = PeerAddress(this.device.address.address)),
                )
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
