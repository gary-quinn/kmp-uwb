package com.atruedev.kmpuwb.adapter

/**
 * The state of the device's UWB hardware.
 */
public enum class UwbAdapterState {
    /** UWB hardware is available and enabled. */
    ON,

    /** UWB hardware is present but disabled. */
    OFF,

    /** This device has no UWB hardware. */
    UNSUPPORTED,
}
