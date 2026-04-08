package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A UWB ranging session with one or more peers.
 *
 * Obtained exclusively via [PreparedSession.startRanging] after exchanging
 * session parameters with a remote peer over an out-of-band transport.
 *
 * State transitions are observable via [state].
 * Ranging measurements are emitted via [rangingResults] using the
 * [BackpressureStrategy][com.atruedev.kmpuwb.config.BackpressureStrategy]
 * specified at [PreparedSession.startRanging] time.
 *
 * ```
 * val prepared = adapter.prepareSession(config)
 * // ... exchange params via BLE/NFC ...
 * val session = prepared.startRanging(remoteParams)
 * session.rangingResults.collect { result -> ... }
 * session.close()
 * ```
 */
public interface RangingSession : AutoCloseable {
    public val config: RangingConfig
    public val state: StateFlow<RangingState>
    public val rangingResults: Flow<RangingResult>

    /**
     * Stop ranging and release platform resources.
     *
     * Transitions state to [RangingState.Stopped.ByRequest].
     * Safe to call multiple times.
     */
    override fun close()
}
