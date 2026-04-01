package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.session.IosPreparedSession
import com.atruedev.kmpuwb.session.PreparedSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.Foundation.NSClassFromString
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.NearbyInteraction.NISession

internal class IosUwbAdapter : UwbAdapter {
    private val _state = MutableStateFlow(resolveAdapterState())

    override val state: StateFlow<UwbAdapterState> = _state.asStateFlow()

    override fun close() = Unit

    override suspend fun capabilities(): UwbCapabilities {
        if (_state.value == UwbAdapterState.UNSUPPORTED) {
            return UwbCapabilities.NONE
        }

        return UwbCapabilities(
            supportedRoles = setOf(RangingRole.CONTROLLER, RangingRole.CONTROLEE),
            angleOfArrivalSupported = true,
            supportedChannels = setOf(5, 9),
            backgroundRangingSupported = isBackgroundRangingAvailable(),
        )
    }

    override suspend fun prepareSession(config: RangingConfig): PreparedSession = IosPreparedSession.create(config)

    private fun resolveAdapterState(): UwbAdapterState {
        if (NSClassFromString("NISession") == null) return UwbAdapterState.UNSUPPORTED

        return if (NISession.isSupported()) {
            UwbAdapterState.ON
        } else {
            UwbAdapterState.UNSUPPORTED
        }
    }

    /**
     * Background ranging requires iOS 16+ and the com.apple.developer.nearby-interaction
     * entitlement. We check the OS version; the entitlement is the app's responsibility.
     *
     * Note: iOS NearbyInteraction does not expose a capabilities query API comparable to
     * Android's RangingCapabilities. Channels (5, 9) and AoA support are constant across
     * all UWB-capable iPhones (U1/U2 chip). Background ranging is the only capability
     * that varies by OS version.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun isBackgroundRangingAvailable(): Boolean {
        val version = cValue<NSOperatingSystemVersion> {
            this.majorVersion = 16
            this.minorVersion = 0
            this.patchVersion = 0
        }
        return NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(version)
    }
}

public actual fun UwbAdapter(): UwbAdapter = IosUwbAdapter()
