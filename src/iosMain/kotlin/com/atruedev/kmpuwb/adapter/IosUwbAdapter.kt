package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.session.IosPreparedSession
import com.atruedev.kmpuwb.session.PreparedSession
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

    override suspend fun prepareSession(config: RangingConfig): PreparedSession = IosPreparedSession(config)

    private fun resolveAdapterState(): UwbAdapterState =
        try {
            if (NISession.deviceCapabilities.supportsPreciseDistanceMeasurement) {
                UwbAdapterState.ON
            } else {
                UwbAdapterState.UNSUPPORTED
            }
        } catch (_: Exception) {
            UwbAdapterState.UNSUPPORTED
        }
}

public actual fun UwbAdapter(): UwbAdapter = IosUwbAdapter()
