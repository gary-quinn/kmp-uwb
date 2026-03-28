package com.atruedev.kmpuwb.adapter

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.uwb.UwbManager
import com.atruedev.kmpuwb.config.RangingRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidUwbAdapter(
    private val context: Context,
) : UwbAdapter {

    private val _state = MutableStateFlow(resolveAdapterState())

    override val state: StateFlow<UwbAdapterState> = _state.asStateFlow()

    override suspend fun capabilities(): UwbCapabilities {
        if (_state.value == UwbAdapterState.UNSUPPORTED) {
            return UwbCapabilities.NONE
        }

        return try {
            val uwbManager = UwbManager.createInstance(context)
            val controllerSession = uwbManager.controllerSessionScope()

            UwbCapabilities(
                supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
                angleOfArrivalSupported = controllerSession.rangingCapabilities.isAzimuthalAngleSupported,
                supportedChannels = controllerSession.rangingCapabilities.supportedChannels.toSet(),
                backgroundRangingSupported = controllerSession.rangingCapabilities.isBackgroundRangingSupported,
            )
        } catch (_: Exception) {
            UwbCapabilities.NONE
        }
    }

    private fun resolveAdapterState(): UwbAdapterState =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)) {
            UwbAdapterState.ON
        } else {
            UwbAdapterState.UNSUPPORTED
        }
}

public actual fun UwbAdapter(): UwbAdapter {
    val context = KmpUwb.requireContext()
    return AndroidUwbAdapter(context)
}
