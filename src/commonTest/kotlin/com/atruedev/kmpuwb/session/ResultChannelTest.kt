package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.BackpressureStrategy
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResultChannelTest {
    @Test
    fun keepLatestDropsOldestWhenBufferFull() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.KeepLatest)
            val sent = DEFAULT_BUFFER_CAPACITY + 1
            repeat(sent) { channel.trySend(positionWithDistance(it.toDouble())) }

            val first = channel.tryReceive().getOrNull()
            assertIs<RangingResult.Position>(first)
            assertEquals(
                1.0,
                first.measurement.distance?.meters,
                "Expected item 0 dropped, first available is item 1",
            )
            channel.close()
        }

    @Test
    fun unboundedKeepsAllMeasurements() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.Unbounded)
            val sent = DEFAULT_BUFFER_CAPACITY * 3
            repeat(sent) { channel.trySend(positionWithDistance(it.toDouble())) }

            var count = 0
            while (channel.tryReceive().isSuccess) count++
            assertEquals(sent, count, "Expected all $sent items buffered")
            channel.close()
        }

    @Test
    fun keepOldestDiscardsNewestWhenBufferFull() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.KeepOldest)
            val sent = DEFAULT_BUFFER_CAPACITY + 1
            repeat(sent) { channel.trySend(positionWithDistance(it.toDouble())) }

            val first = channel.tryReceive().getOrNull()
            assertIs<RangingResult.Position>(first)
            assertEquals(0.0, first.measurement.distance?.meters, "Expected oldest items retained")

            var count = 1
            while (channel.tryReceive().isSuccess) count++
            assertTrue(count <= DEFAULT_BUFFER_CAPACITY + 1, "Expected overflow items dropped, got $count")
            channel.close()
        }

    private fun positionWithDistance(meters: Double) =
        RangingResult.Position(
            peer = FIXED_PEER,
            measurement = RangingMeasurement(distance = Distance.meters(meters)),
        )

    private companion object {
        val FIXED_PEER = Peer(address = PeerAddress(byteArrayOf(0x01, 0x02)))
    }
}
