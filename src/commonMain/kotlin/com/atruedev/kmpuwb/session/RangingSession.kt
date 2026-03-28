package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A UWB ranging session with one or more peers.
 *
 * Lifecycle is managed explicitly via [start] and [close].
 * State transitions are observable via [state].
 * Ranging measurements are emitted as a cold [Flow] via [rangingResults].
 *
 * Implements [AutoCloseable] for structured resource cleanup.
 *
 * ```
 * val session = RangingSession(config)
 * session.start(peer)
 * session.rangingResults.collect { result ->
 *     when (result) {
 *         is RangingResult.Position -> handlePosition(result)
 *         is RangingResult.PeerLost -> handlePeerLost(result)
 *         is RangingResult.PeerRecovered -> handleRecovered(result)
 *     }
 * }
 * session.close()
 * ```
 */
public interface RangingSession : AutoCloseable {

    /** Configuration this session was created with. */
    public val config: RangingConfig

    /** Current state of the ranging session. Always-readable. */
    public val state: StateFlow<RangingState>

    /**
     * Start ranging with the specified peer.
     *
     * Transitions state from [RangingState.Idle] through
     * [RangingState.Starting] to [RangingState.Active].
     *
     * @throws IllegalStateException if the session is not in [RangingState.Idle.Ready].
     */
    public suspend fun start(peer: Peer)

    /**
     * Ranging measurements as a cold [Flow].
     *
     * No resources are allocated until collection begins.
     * The flow completes when the session enters [RangingState.Stopped].
     */
    public val rangingResults: Flow<RangingResult>

    /**
     * Stop ranging and release resources.
     *
     * Transitions state to [RangingState.Stopped.ByRequest].
     * Safe to call multiple times. After close, the session
     * cannot be restarted.
     */
    override fun close()
}

/** Create a platform-specific [RangingSession]. */
public expect fun RangingSession(config: RangingConfig): RangingSession
