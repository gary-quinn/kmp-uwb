package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer

/**
 * A UWB session that has been prepared but not yet started.
 *
 * Holds allocated platform resources (Android session scope, iOS NISession)
 * and exposes [localParams] for out-of-band exchange with the remote peer.
 * Once the remote peer's parameters have been received and decoded into a [Peer],
 * call [startRanging] to begin the session.
 *
 * If ranging is not needed, call [close] to release platform resources.
 *
 * ```kotlin
 * val prepared = adapter.prepareSession(config)
 * val localBytes = prepared.localParams.toByteArray()
 * // ... exchange over BLE/NFC/WiFi ...
 * val session = prepared.startRanging(remotePeer)
 * ```
 */
public interface PreparedSession : AutoCloseable {
    /** The configuration this session was prepared with. */
    public val config: RangingConfig

    /** Local session parameters to send to the remote peer over OOB transport. */
    public val localParams: SessionParams

    /**
     * Finalize the prepared session and begin ranging with the remote peer.
     *
     * The returned [RangingSession] transitions through the standard state machine
     * (Starting → Active). This method consumes the prepared session — calling it
     * again or calling [close] after is a no-op.
     */
    public suspend fun startRanging(remotePeer: Peer): RangingSession

    /** Release platform resources without starting a ranging session. */
    override fun close()
}
