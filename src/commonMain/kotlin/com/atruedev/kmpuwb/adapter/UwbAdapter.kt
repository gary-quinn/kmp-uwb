package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.session.PreparedSession
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

    /**
     * Prepare a UWB ranging session without starting it.
     *
     * Allocates platform resources and generates [PreparedSession.localParams]
     * for out-of-band exchange with a remote peer. Call [PreparedSession.startRanging]
     * after exchanging parameters, or [PreparedSession.close] to release resources.
     *
     * @see com.atruedev.kmpuwb.connector.PeerConnector
     */
    public suspend fun prepareSession(config: RangingConfig): PreparedSession
}

/** Create a platform-specific [UwbAdapter] instance. */
public expect fun UwbAdapter(): UwbAdapter
