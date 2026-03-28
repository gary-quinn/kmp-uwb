package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.NearbyInteraction.NISession

internal class IosUwbAdapter : UwbAdapter {

    private val _state = MutableStateFlow(resolveAdapterState())

    override val state: StateFlow<UwbAdapterState> = _state.asStateFlow()

    override suspend fun capabilities(): UwbCapabilities {
        if (_state.value == UwbAdapterState.UNSUPPORTED) {
            return UwbCapabilities.NONE
        }

        return UwbCapabilities(
            supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
            angleOfArrivalSupported = true,
            supportedChannels = setOf(5, 9),
            backgroundRangingSupported = false,
        )
    }

    private fun resolveAdapterState(): UwbAdapterState =
        if (NISession.deviceCapabilities.supportsPreciseDistanceMeasurement) {
            UwbAdapterState.ON
        } else {
            UwbAdapterState.UNSUPPORTED
        }
}

public actual fun UwbAdapter(): UwbAdapter = IosUwbAdapter()
