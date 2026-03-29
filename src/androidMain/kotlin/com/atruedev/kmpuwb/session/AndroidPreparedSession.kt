package com.atruedev.kmpuwb.session

import android.content.Context
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import com.atruedev.kmpuwb.config.RangingConfig
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

internal class AndroidPreparedSession(
    override val config: RangingConfig,
    private val context: Context,
    private val sessionScope: UwbClientSessionScope,
) : PreparedSession {
    private var consumed: Boolean = false

    override val localParams: SessionParams by lazy {
        encodeLocalParams()
    }

    override suspend fun startRanging(remoteParams: SessionParams): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true

        val decoded = decodeRemoteParams(remoteParams)
        val rangingParameters = buildRangingParameters(decoded)
        return createRangingSession(rangingParameters)
    }

    override fun close() {
        consumed = true
    }

    private fun encodeLocalParams(): SessionParams {
        val localAddress = sessionScope.localAddress.address
        val complexChannel = resolveComplexChannel()
        val sessionKey = config.sessionKey ?: byteArrayOf()

        val buffer = ByteBuffer.allocate(2 + 1 + 1 + 4 + sessionKey.size)
        buffer.put(localAddress)
        buffer.put(complexChannel.channel.toByte())
        buffer.put(complexChannel.preambleIndex.toByte())
        buffer.putInt(config.sessionId)
        buffer.put(sessionKey)

        return SessionParams(buffer.array())
    }

    private fun decodeRemoteParams(params: SessionParams): Peer {
        val bytes = params.toByteArray()
        require(bytes.size >= 4) { "Remote SessionParams too short: ${bytes.size} bytes" }

        val peerAddress = PeerAddress(bytes.copyOfRange(0, 2))
        return Peer(address = peerAddress)
    }

    private fun resolveComplexChannel(): UwbComplexChannel =
        if (sessionScope is UwbControllerSessionScope) {
            sessionScope.uwbComplexChannel
        } else {
            UwbComplexChannel(config.channel, 0)
        }

    private fun buildRangingParameters(peer: Peer): RangingParameters =
        RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = config.sessionId,
            sessionKeyInfo = config.sessionKey ?: byteArrayOf(),
            complexChannel = resolveComplexChannel(),
            peerDevices = listOf(UwbDevice(UwbAddress(peer.address.toByteArray()))),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
            subSessionId = 0,
            subSessionKeyInfo = null,
        )

    private fun createRangingSession(rangingParameters: RangingParameters): RangingSession {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
        val state = MutableStateFlow<RangingState>(RangingState.Starting.Negotiating)
        val resultChannel =
            Channel<RangingResult>(
                capacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        scope.launch {
            try {
                state.value = RangingState.Starting.Initializing
                state.value = RangingState.Active.Ranging

                sessionScope.prepareSession(rangingParameters).collect { platformResult ->
                    val result = platformResult.toRangingResult()
                    if (result != null) {
                        resultChannel.trySend(result)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.value =
                    RangingState.Stopped.ByError(
                        SessionLost(
                            message = "Android ranging session failed: ${e.message}",
                            cause = e,
                        ),
                    )
                resultChannel.close()
            }
        }

        return PreparedAndroidRangingSession(
            config = config,
            scope = scope,
            _state = state,
            resultChannel = resultChannel,
        )
    }
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

private fun androidx.core.uwb.RangingPosition.toMeasurement(): RangingMeasurement =
    RangingMeasurement(
        distance = Distance.meters(this.distance?.value?.toDouble() ?: 0.0),
        azimuth = this.azimuth?.let { Angle.degrees(it.value.toDouble()) },
        elevation = this.elevation?.let { Angle.degrees(it.value.toDouble()) },
    )

/**
 * RangingSession created from a PreparedSession — owns its own scope and channels.
 * Avoids mutating an externally-created AndroidRangingSession's internal state.
 */
private class PreparedAndroidRangingSession(
    override val config: RangingConfig,
    private val scope: CoroutineScope,
    private val _state: MutableStateFlow<RangingState>,
    private val resultChannel: Channel<RangingResult>,
) : RangingSession {
    override val state: StateFlow<RangingState> = _state.asStateFlow()
    override val rangingResults = resultChannel.receiveAsFlow()

    override suspend fun start(peer: Peer): Unit = error("PreparedSession-created sessions are already started")

    override fun close() {
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        resultChannel.close()
        scope.cancel()
    }
}
