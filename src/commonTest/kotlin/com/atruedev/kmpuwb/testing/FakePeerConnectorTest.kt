package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.ExchangeTimedOut
import com.atruedev.kmpuwb.session.SessionParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakePeerConnectorTest {
    private val localParams = SessionParams(byteArrayOf(0x01, 0x02))
    private val remoteParams = SessionParams(byteArrayOf(0x0A, 0x0B))

    @Test
    fun successConnectorReturnsConfiguredParams() =
        runTest {
            val connector = FakePeerConnector(remoteParams)
            val result = connector.exchange(localParams)
            assertEquals(remoteParams, result)
        }

    @Test
    fun successConnectorTracksExchangeCall() =
        runTest {
            val connector = FakePeerConnector(remoteParams)
            assertFalse(connector.exchangeCalled)

            connector.exchange(localParams)

            assertTrue(connector.exchangeCalled)
            assertEquals(localParams, connector.lastLocalParams)
        }

    @Test
    fun failingConnectorThrowsConfiguredError() =
        runTest {
            val error = ExchangeTimedOut("test timeout")
            val connector = FailingPeerConnector(error)

            val exception =
                assertFailsWith<ConnectorException> {
                    connector.exchange(localParams)
                }
            assertEquals(error, exception.error)
        }

    @Test
    fun failingConnectorTracksExchangeCall() =
        runTest {
            val connector = FailingPeerConnector(ExchangeTimedOut("timeout"))
            assertFalse(connector.exchangeCalled)

            runCatching { connector.exchange(localParams) }

            assertTrue(connector.exchangeCalled)
        }
}
