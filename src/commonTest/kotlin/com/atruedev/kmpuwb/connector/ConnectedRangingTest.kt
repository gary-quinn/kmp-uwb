package com.atruedev.kmpuwb.connector

import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.config.rangingConfig
import com.atruedev.kmpuwb.session.SessionParams
import com.atruedev.kmpuwb.state.RangingState
import com.atruedev.kmpuwb.testing.FailingPeerConnector
import com.atruedev.kmpuwb.testing.FakePeerConnector
import com.atruedev.kmpuwb.testing.FakePreparedSession
import com.atruedev.kmpuwb.testing.FakeUwbAdapter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConnectedRangingTest {
    private val config =
        rangingConfig {
            role = RangingRole.CONTROLLER
            sessionId = 42
        }

    private val remoteParams = SessionParams(byteArrayOf(0x0A, 0x0B, 0x0C))

    @Test
    fun happyPathReturnsActiveSession() =
        runTest {
            val adapter = FakeUwbAdapter()
            val connector = FakePeerConnector(remoteParams)

            val session = adapter.startWithConnector(config, connector)

            assertIs<RangingState.Active.Ranging>(session.state.value)
            assertTrue(connector.exchangeCalled)
        }

    @Test
    fun connectorReceivesLocalParams() =
        runTest {
            val adapter = FakeUwbAdapter()
            val connector = FakePeerConnector(remoteParams)

            adapter.startWithConnector(config, connector)

            val exchangedLocal = connector.lastLocalParams
            assertTrue(exchangedLocal != null)
            assertTrue(exchangedLocal.size > 0)
        }

    @Test
    fun connectorFailureReleasesResources() =
        runTest {
            val prepared = FakePreparedSession(config = config)
            val adapter = FakeUwbAdapter()
            adapter.setPreparedSessionFactory { prepared }

            val connector = FailingPeerConnector(ExchangeTimedOut("timeout"))

            assertFailsWith<ConnectorException> {
                adapter.startWithConnector(config, connector)
            }

            assertTrue(prepared.closeCalled)
        }

    @Test
    fun connectorFailurePropagatesError() =
        runTest {
            val adapter = FakeUwbAdapter()
            val error = TransportFailure("BLE disconnected")
            val connector = FailingPeerConnector(error)

            val exception =
                assertFailsWith<ConnectorException> {
                    adapter.startWithConnector(config, connector)
                }

            assertIs<TransportFailure>(exception.error)
        }
}
