package com.atruedev.kmpuwb.adapter

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.uwb.UwbManager
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.session.AndroidPreparedSession
import com.atruedev.kmpuwb.session.PreparedSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidUwbAdapter(
    private val context: Context,
) : UwbAdapter {
    // Snapshot state — Android UWB SDK does not provide a state-change broadcast.
    // Re-query via UwbAdapter() if the user toggles UWB in Settings.
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

            UwbCapabilities(
                supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
                angleOfArrivalSupported = capabilities.isAzimuthalAngleSupported,
                supportedChannels = capabilities.supportedChannels.toSet(),
                backgroundRangingSupported = capabilities.isBackgroundRangingSupported,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Android UWB SDK (alpha) does not document specific exception types.
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

    override fun close() = Unit

    private fun resolveAdapterState(): UwbAdapterState {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)
        if (!hasFeature) return UwbAdapterState.UNSUPPORTED

        return try {
            UwbManager.createInstance(context)
            UwbAdapterState.ON
        } catch (_: Exception) {
            // Alpha SDK — any createInstance() failure means UWB is unavailable.
            UwbAdapterState.OFF
        }
    }
}

public actual fun UwbAdapter(): UwbAdapter {
    val context = KmpUwb.requireContext()
    return AndroidUwbAdapter(context)
}
