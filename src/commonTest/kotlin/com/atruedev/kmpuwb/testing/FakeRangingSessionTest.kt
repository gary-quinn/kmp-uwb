package com.atruedev.kmpuwb.testing

import app.cash.turbine.test
import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Angle
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeRangingSessionTest {
    private val testPeer =
        Peer(
            address = PeerAddress(byteArrayOf(0x01, 0x02)),
            name = "test-tag",
        )

    private val testMeasurement =
        RangingMeasurement(
            distance = Distance.meters(2.5),
            azimuth = Angle.degrees(15.0),
            elevation = Angle.degrees(-5.0),
        )

    @Test
    fun defaultStateIsActiveRanging() {
        val session = FakeRangingSession()
        assertIs<RangingState.Active.Ranging>(session.state.value)
    }

    @Test
    fun customInitialState() {
        val session = FakeRangingSession(initialState = RangingState.Idle.Ready)
        assertIs<RangingState.Idle.Ready>(session.state.value)
    }

    @Test
    fun closeEmitsStoppedByRequest() =
        runTest {
            val session = FakeRangingSession()

            session.state.test {
                assertIs<RangingState.Active.Ranging>(awaitItem())

                session.close()

                assertIs<RangingState.Stopped.ByRequest>(awaitItem())
            }
        }

    @Test
    fun closeClearsActivePeers() =
        runTest {
            val session = FakeRangingSession()
            session.addPeer(testPeer)
            session.close()
            assertTrue(session.activePeers.isEmpty())
        }

    @Test
    fun emitResultDeliversToRangingResults() =
        runTest {
            val session = FakeRangingSession()

            val position = RangingResult.Position(testPeer, testMeasurement)

            session.rangingResults.test {
                session.emitResult(position)

                val result = awaitItem()
                assertIs<RangingResult.Position>(result)
                assertEquals(testPeer, result.peer)
                assertEquals(testMeasurement.distance, result.measurement.distance)
            }
        }

    @Test
    fun multipleResultsArriveInOrder() =
        runTest {
            val session = FakeRangingSession()

            val position1 = RangingResult.Position(testPeer, testMeasurement)
            val position2 =
                RangingResult.Position(
                    testPeer,
                    testMeasurement.copy(distance = Distance.meters(3.0)),
                )

            session.rangingResults.test {
                session.emitResult(position1)
                session.emitResult(position2)

                val first = awaitItem()
                assertIs<RangingResult.Position>(first)
                assertEquals(2.5, first.measurement.distance?.meters)

                val second = awaitItem()
                assertIs<RangingResult.Position>(second)
                assertEquals(3.0, second.measurement.distance?.meters)
            }
        }

    @Test
    fun simulatePeerLostEmitsResultAndUpdatesState() =
        runTest {
            val session = FakeRangingSession()
            session.addPeer(testPeer)

            session.rangingResults.test {
                session.simulatePeerLost(testPeer)

                val result = awaitItem()
                assertIs<RangingResult.PeerLost>(result)
                assertEquals(testPeer, result.peer)
            }

            assertTrue(session.activePeers.isEmpty())
            assertIs<RangingState.Active.PeerLost>(session.state.value)
        }

    @Test
    fun simulatePeerRecoveredEmitsResultAndUpdatesState() =
        runTest {
            val session = FakeRangingSession()
            session.addPeer(testPeer)

            val recovered = RangingResult.PeerRecovered(testPeer, testMeasurement)

            session.rangingResults.test {
                session.simulatePeerLost(testPeer)
                assertIs<RangingResult.PeerLost>(awaitItem())

                session.simulatePeerRecovered(recovered)

                val result = awaitItem()
                assertIs<RangingResult.PeerRecovered>(result)
                assertEquals(testPeer, result.peer)
            }

            assertTrue(session.activePeers.contains(testPeer))
            assertIs<RangingState.Active.Ranging>(session.state.value)
        }

    @Test
    fun simulateErrorClosesRangingResults() =
        runTest {
            val session = FakeRangingSession()

            val error = SessionLost("test error")

            session.rangingResults.test {
                session.simulateError(error)

                awaitComplete()
            }

            assertIs<RangingState.Stopped.ByError>(session.state.value)
            assertEquals(error, (session.state.value as RangingState.Stopped.ByError).error)
        }

    @Test
    fun simulateSuspendAndResumeStateSequence() =
        runTest {
            val session = FakeRangingSession()

            session.state.test {
                assertIs<RangingState.Active.Ranging>(awaitItem())

                session.simulateSuspend()
                assertIs<RangingState.Active.Suspended>(awaitItem())

                session.simulateResume()
                assertIs<RangingState.Active.Ranging>(awaitItem())
            }
        }

    @Test
    fun simulatePeerDisconnectClosesRangingResults() =
        runTest {
            val session = FakeRangingSession()

            session.rangingResults.test {
                session.simulatePeerDisconnect()

                awaitComplete()
            }

            assertIs<RangingState.Stopped.ByPeer>(session.state.value)
        }
}
