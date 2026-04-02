package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.connector.ConnectorError
import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.PeerConnector
import com.atruedev.kmpuwb.session.SessionParams

/**
 * Test double for [PeerConnector] that returns pre-configured remote parameters.
 *
 * ```kotlin
 * val connector = FakePeerConnector(remoteParams = SessionParams(remoteBytes))
 * val remote = connector.exchange(localParams)
 * assertTrue(connector.exchangeCalled)
 * ```
 */
public class FakePeerConnector(
    private val remoteParams: SessionParams,
) : PeerConnector {
    /** Whether [exchange] has been called. */
    public var exchangeCalled: Boolean = false
        private set

    /** The local params passed to the most recent [exchange] call. */
    public var lastLocalParams: SessionParams? = null
        private set

    override suspend fun exchange(localParams: SessionParams): SessionParams {
        exchangeCalled = true
        lastLocalParams = localParams
        return remoteParams
    }
}

/**
 * Test double for [PeerConnector] that always throws a [ConnectorException].
 *
 * ```kotlin
 * val connector = FailingPeerConnector(ExchangeTimedOut("timeout"))
 * assertFailsWith<ConnectorException> { connector.exchange(localParams) }
 * ```
 */
public class FailingPeerConnector(
    private val error: ConnectorError,
) : PeerConnector {
    /** Whether [exchange] has been called. */
    public var exchangeCalled: Boolean = false
        private set

    override suspend fun exchange(localParams: SessionParams): SessionParams {
        exchangeCalled = true
        throw ConnectorException(error)
    }
}
