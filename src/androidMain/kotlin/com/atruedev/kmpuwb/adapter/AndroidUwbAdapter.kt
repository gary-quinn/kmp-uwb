package com.atruedev.kmpuwb.adapter

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.uwb.UwbManager
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.session.AndroidPreparedSession
import com.atruedev.kmpuwb.session.PreparedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

internal class AndroidUwbAdapter(
    private val context: Context,
) : UwbAdapter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<UwbAdapterState> =
        flow { emit(resolveAdapterState()) }
            .stateIn(scope, SharingStarted.Lazily, resolveAdapterState())

    override suspend fun capabilities(): UwbCapabilities {
        if (state.value == UwbAdapterState.UNSUPPORTED) {
            return UwbCapabilities.NONE
        }

        return try {
            val uwbManager = UwbManager.createInstance(context)
            val controllerSession = uwbManager.controllerSessionScope()
            val capabilities = controllerSession.rangingCapabilities

            UwbCapabilities(
                supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
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

    override fun close() {
        scope.cancel()
    }

    private fun resolveAdapterState(): UwbAdapterState {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)
        if (!hasFeature) return UwbAdapterState.UNSUPPORTED

        return try {
            UwbManager.createInstance(context)
            UwbAdapterState.ON
        } catch (_: Exception) {
            UwbAdapterState.OFF
        }
    }
}

public actual fun UwbAdapter(): UwbAdapter {
    val context = KmpUwb.requireContext()
    return AndroidUwbAdapter(context)
}
