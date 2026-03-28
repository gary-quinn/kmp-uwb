package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingRole

/**
 * Capabilities of the device's UWB hardware.
 *
 * Queried from the platform to determine what features
 * are available before starting a ranging session.
 */
public data class UwbCapabilities(
    /** Roles this device can play in a ranging session. */
    val supportedRoles: Set<RangingRole>,
    /** Whether Angle of Arrival is supported. */
    val angleOfArrivalSupported: Boolean,
    /** Available UWB channels (typically 5 and/or 9). */
    val supportedChannels: Set<Int>,
    /** Whether background ranging is supported. */
    val backgroundRangingSupported: Boolean,
) {
    public companion object {
        /** Capabilities for a device with no UWB support. */
        public val NONE: UwbCapabilities = UwbCapabilities(
            supportedRoles = emptySet(),
            angleOfArrivalSupported = false,
            supportedChannels = emptySet(),
            backgroundRangingSupported = false,
        )
    }
}
