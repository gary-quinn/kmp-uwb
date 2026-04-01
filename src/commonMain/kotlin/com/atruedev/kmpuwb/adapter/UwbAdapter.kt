package com.atruedev.kmpuwb.adapter

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.session.PreparedSession
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides access to UWB adapter state and capabilities.
 *
 * [state] is a snapshot captured at construction time. Unlike BLE, the Android UWB SDK
 * does not provide a state-change broadcast — create a new adapter instance to re-query.
 *
 * Obtain an instance via [UwbAdapter] factory function.
 */
public interface UwbAdapter : AutoCloseable {
    /** Snapshot of the UWB adapter state at construction time. */
    public val state: StateFlow<UwbAdapterState>

    public suspend fun capabilities(): UwbCapabilities

    /**
     * Prepare a UWB ranging session without starting it.
     *
     * Allocates platform resources and generates [PreparedSession.localParams]
     * for out-of-band exchange with a remote peer. Call [PreparedSession.startRanging]
     * after exchanging parameters, or [PreparedSession.close] to release resources.
     */
    public suspend fun prepareSession(config: RangingConfig): PreparedSession

    override fun close()
}

/** Create a platform-specific [UwbAdapter] instance. */
public expect fun UwbAdapter(): UwbAdapter
