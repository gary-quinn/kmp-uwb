package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.session.PreparedSession
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.session.SessionParams

/**
 * Test double for [PreparedSession] with verification flags.
 */
public class FakePreparedSession(
    override val config: RangingConfig = FakeRangingSession.DEFAULT_CONFIG,
    override val localParams: SessionParams = DEFAULT_LOCAL_PARAMS,
    private val sessionFactory: (RangingConfig) -> FakeRangingSession = { FakeRangingSession(it) },
) : PreparedSession {
    public var startRangingCalled: Boolean = false
        private set

    public var lastRemoteParams: SessionParams? = null
        private set

    public var closeCalled: Boolean = false
        private set

    private var consumed: Boolean = false

    override suspend fun startRanging(remoteParams: SessionParams): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true
        startRangingCalled = true
        lastRemoteParams = remoteParams

        val session = sessionFactory(config)
        session.start(
            com.atruedev.kmpuwb.peer.Peer(
                address = com.atruedev.kmpuwb.peer.PeerAddress(remoteParams.toByteArray()),
            ),
        )
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
