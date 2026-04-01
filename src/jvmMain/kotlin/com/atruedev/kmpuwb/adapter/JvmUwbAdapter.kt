package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.session.PreparedSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class JvmUwbAdapter : UwbAdapter {
    override val state: StateFlow<UwbAdapterState> =
        MutableStateFlow(UwbAdapterState.UNSUPPORTED).asStateFlow()

    override suspend fun capabilities(): UwbCapabilities = UwbCapabilities.NONE

    override suspend fun prepareSession(config: RangingConfig): PreparedSession =
        throw UnsupportedOperationException(
            "UWB is not available on JVM. Use FakeUwbAdapter for testing.",
        )

    override fun close() = Unit
}

public actual fun UwbAdapter(): UwbAdapter = JvmUwbAdapter()
