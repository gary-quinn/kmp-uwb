package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.error.UwbUnavailable
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

internal class JvmRangingSession(
    override val config: RangingConfig,
) : RangingSession {

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Unsupported)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    override suspend fun start(peer: Peer) {
        throw UnsupportedOperationException(
            "UWB ranging is not available on JVM. " +
                "Use FakeRangingSession for testing.",
        )
    }

    override val rangingResults: Flow<RangingResult> = emptyFlow()

    override fun close() {
        _state.value = RangingState.Stopped.ByError(
            UwbUnavailable("UWB is not available on JVM"),
        )
    }
}

public actual fun RangingSession(config: RangingConfig): RangingSession =
    JvmRangingSession(config)
