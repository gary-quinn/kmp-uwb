package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.adapter.UwbCapabilities
import com.atruedev.kmpuwb.config.RangingRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [UwbAdapter] that allows controlling state and capabilities.
 *
 * ```
 * val adapter = FakeUwbAdapter()
 * adapter.state.value // UwbAdapterState.ON
 *
 * adapter.simulateDisabled()
 * adapter.state.value // UwbAdapterState.OFF
 * ```
 */
public class FakeUwbAdapter(
    initialState: UwbAdapterState = UwbAdapterState.ON,
    private val capabilities: UwbCapabilities = DEFAULT_CAPABILITIES,
) : UwbAdapter {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<UwbAdapterState> = _state.asStateFlow()

    override suspend fun capabilities(): UwbCapabilities =
        if (_state.value == UwbAdapterState.UNSUPPORTED) {
            UwbCapabilities.NONE
        } else {
            capabilities
        }

    /** Transition adapter to [UwbAdapterState.OFF]. */
    public fun simulateDisabled() {
        _state.value = UwbAdapterState.OFF
    }

    /** Transition adapter to [UwbAdapterState.ON]. */
    public fun simulateEnabled() {
        _state.value = UwbAdapterState.ON
    }

    /** Transition adapter to [UwbAdapterState.UNSUPPORTED]. */
    public fun simulateUnsupported() {
        _state.value = UwbAdapterState.UNSUPPORTED
    }

    public companion object {
        public val DEFAULT_CAPABILITIES: UwbCapabilities = UwbCapabilities(
            supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
            angleOfArrivalSupported = true,
            supportedChannels = setOf(5, 9),
            backgroundRangingSupported = true,
        )
    }
}
