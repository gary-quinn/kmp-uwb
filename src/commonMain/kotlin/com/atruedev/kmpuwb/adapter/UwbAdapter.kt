package com.atruedev.kmpuwb.adapter

import kotlinx.coroutines.flow.StateFlow

/**
 * Provides access to UWB adapter state and capabilities.
 *
 * Platform implementations monitor the system's UWB hardware
 * and expose state changes as a [StateFlow].
 *
 * Obtain an instance via [UwbAdapter] factory function.
 */
public interface UwbAdapter {

    /** Current state of the UWB adapter. Always-readable [StateFlow]. */
    public val state: StateFlow<UwbAdapterState>

    /** Hardware capabilities of this device's UWB chipset. */
    public suspend fun capabilities(): UwbCapabilities
}

/** Create a platform-specific [UwbAdapter] instance. */
public expect fun UwbAdapter(): UwbAdapter
