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
public interface UwbAdapter : AutoCloseable {
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
