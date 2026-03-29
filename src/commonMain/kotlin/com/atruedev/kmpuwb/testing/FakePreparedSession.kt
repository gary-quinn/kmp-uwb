package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.session.PreparedSession
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.session.SessionParams

/**
 * Test double for [PreparedSession] with verification flags.
 *
 * ```kotlin
 * val prepared = FakePreparedSession()
 * val session = prepared.startRanging(peer)
 * assertTrue(prepared.startRangingCalled)
 * assertEquals(peer, prepared.lastRemotePeer)
 * ```
 */
public class FakePreparedSession(
    override val config: RangingConfig = FakeRangingSession.DEFAULT_CONFIG,
    override val localParams: SessionParams = DEFAULT_LOCAL_PARAMS,
    private val sessionFactory: (RangingConfig) -> RangingSession = { FakeRangingSession(it) },
) : PreparedSession {
    /** Whether [startRanging] has been called. */
    public var startRangingCalled: Boolean = false
        private set

    /** The last remote peer passed to [startRanging]. */
    public var lastRemotePeer: Peer? = null
        private set

    /** Whether [close] has been called. */
    public var closeCalled: Boolean = false
        private set

    private var consumed: Boolean = false

    override suspend fun startRanging(remotePeer: Peer): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true
        startRangingCalled = true
        lastRemotePeer = remotePeer
        val session = sessionFactory(config)
        if (session is FakeRangingSession) {
            session.start(remotePeer)
        }
        return session
    }

    override fun close() {
        closeCalled = true
        consumed = true
    }

    public companion object {
        public val DEFAULT_LOCAL_PARAMS: SessionParams =
            SessionParams(byteArrayOf(0x01, 0x02, 0x03, 0x04))
    }
}
