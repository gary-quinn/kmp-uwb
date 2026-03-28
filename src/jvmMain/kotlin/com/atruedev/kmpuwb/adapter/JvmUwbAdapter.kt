package com.atruedev.kmpuwb.adapter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class JvmUwbAdapter : UwbAdapter {

    override val state: StateFlow<UwbAdapterState> =
        MutableStateFlow(UwbAdapterState.UNSUPPORTED).asStateFlow()

    override suspend fun capabilities(): UwbCapabilities = UwbCapabilities.NONE
}

public actual fun UwbAdapter(): UwbAdapter = JvmUwbAdapter()
