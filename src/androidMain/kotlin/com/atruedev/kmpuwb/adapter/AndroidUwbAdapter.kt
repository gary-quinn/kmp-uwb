package com.atruedev.kmpuwb.adapter

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.uwb.UwbManager
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.session.AndroidPreparedSession
import com.atruedev.kmpuwb.session.PreparedSession
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
            val capabilities = controllerSession.rangingCapabilities

            val roles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE)

            UwbCapabilities(
                supportedRoles = roles,
                angleOfArrivalSupported = capabilities.isAzimuthalAngleSupported,
                supportedChannels = capabilities.supportedChannels.toSet(),
                backgroundRangingSupported = capabilities.isBackgroundRangingSupported,
            )
        } catch (_: Exception) {
            UwbCapabilities.NONE
        }
    }

    override suspend fun prepareSession(config: RangingConfig): PreparedSession {
        val uwbManager = UwbManager.createInstance(context)
        val sessionScope =
            when (config.role) {
                RangingRole.CONTROLLER -> uwbManager.controllerSessionScope()
                RangingRole.CONTROLEE -> uwbManager.controleeSessionScope()
            }
        return AndroidPreparedSession(config, context, sessionScope)
    }

    private fun resolveAdapterState(): UwbAdapterState {
        val hasFeature =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)
        if (hasFeature) return UwbAdapterState.ON

        // Fallback: some devices report UWB capability through UwbManager
        // even when PackageManager.FEATURE_UWB is absent
        return try {
            UwbManager.createInstance(context)
            UwbAdapterState.ON
        } catch (_: Exception) {
            UwbAdapterState.UNSUPPORTED
        }
    }
}

public actual fun UwbAdapter(): UwbAdapter {
    val context = KmpUwb.requireContext()
    return AndroidUwbAdapter(context)
}
