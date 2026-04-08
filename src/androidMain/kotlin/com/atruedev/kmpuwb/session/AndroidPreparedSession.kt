package com.atruedev.kmpuwb.session

import android.content.Context
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import com.atruedev.kmpuwb.config.BackpressureStrategy
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidPreparedSession(
    override val config: RangingConfig,
    private val context: Context,
    private val sessionScope: UwbClientSessionScope,
) : PreparedSession {
    private val consumed = AtomicBoolean(false)

    override val localParams: SessionParams by lazy {
        SessionParamsCodec.encode(
            address = sessionScope.localAddress,
            complexChannel = resolveComplexChannel(),
            sessionId = config.sessionId,
        )
    }

    override suspend fun startRanging(
        remoteParams: SessionParams,
        backpressureStrategy: BackpressureStrategy,
    ): RangingSession {
        check(consumed.compareAndSet(false, true)) {
            "PreparedSession has already been consumed"
        }
        val decoded = SessionParamsCodec.decode(remoteParams)
        val rangingParameters = buildRangingParameters(decoded)
        return createRangingSession(rangingParameters, backpressureStrategy)
    }

    override fun close() {
        consumed.set(true)
    }

    private fun resolveComplexChannel(): UwbComplexChannel =
        if (sessionScope is UwbControllerSessionScope) {
            sessionScope.uwbComplexChannel
        } else {
            UwbComplexChannel(config.channel, 0)
        }

    private fun buildRangingParameters(decoded: SessionParamsCodec.DecodedParams): RangingParameters =
        RangingParameters(
            uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId = config.sessionId,
            sessionKeyInfo = config.sessionKey ?: byteArrayOf(),
            complexChannel = UwbComplexChannel(decoded.channel, decoded.preambleIndex),
            peerDevices = listOf(UwbDevice(UwbAddress(decoded.address))),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
            subSessionId = 0,
            subSessionKeyInfo = null,
        )

    private fun createRangingSession(
        rangingParameters: RangingParameters,
        backpressureStrategy: BackpressureStrategy,
    ): RangingSession {
        val sessionScope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.Default.limitedParallelism(1) + CoroutineName("UwbRanging"),
            )
        val state = MutableStateFlow<RangingState>(RangingState.Starting.Negotiating)
        val resultChannel = createResultChannel(backpressureStrategy)

        sessionScope.launch {
            try {
                state.value = RangingState.Starting.Initializing
                state.value = RangingState.Active.Ranging

                this@AndroidPreparedSession.sessionScope.prepareSession(rangingParameters).collect { platformResult ->
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

        return AndroidRangingSession(
            config = config,
            scope = sessionScope,
            _state = state,
            resultChannel = resultChannel,
        )
    }
}

private class AndroidRangingSession(
    override val config: RangingConfig,
    private val scope: CoroutineScope,
    private val _state: MutableStateFlow<RangingState>,
    private val resultChannel: Channel<RangingResult>,
) : RangingSession {
    override val state: StateFlow<RangingState> = _state.asStateFlow()
    override val rangingResults = resultChannel.receiveAsFlow()

    override fun close() {
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        resultChannel.close()
        scope.cancel()
    }
}
