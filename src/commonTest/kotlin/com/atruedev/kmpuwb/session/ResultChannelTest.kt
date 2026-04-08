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
    fun latestDropsOldestWhenBufferFull() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.Latest)
            val sent = DEFAULT_BUFFER_CAPACITY + 36
            repeat(sent) { channel.trySend(positionAt(it)) }

            val first = channel.tryReceive().getOrNull()
            assertIs<RangingResult.Position>(first)
            assertTrue(distanceOf(first) > 0, "Expected oldest items dropped")
            channel.close()
        }

    @Test
    fun bufferKeepsAllMeasurements() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.Buffer)
            val sent = DEFAULT_BUFFER_CAPACITY * 3
            repeat(sent) { channel.trySend(positionAt(it)) }

            val first = channel.tryReceive().getOrNull()
            assertIs<RangingResult.Position>(first)
            assertEquals(0, distanceOf(first), "Expected no items dropped")
            channel.close()
        }

    @Test
    fun dropDiscardsNewestWhenBufferFull() =
        runTest {
            val channel = createResultChannel(BackpressureStrategy.Drop)
            val sent = DEFAULT_BUFFER_CAPACITY + 36
            repeat(sent) { channel.trySend(positionAt(it)) }

            val first = channel.tryReceive().getOrNull()
            assertIs<RangingResult.Position>(first)
            assertEquals(0, distanceOf(first), "Expected oldest items retained")

            // Drain all buffered items and count them
            var count = 1
            while (channel.tryReceive().isSuccess) count++
            assertTrue(count <= DEFAULT_BUFFER_CAPACITY + 1, "Expected newest items dropped, got $count")
            channel.close()
        }

    private fun positionAt(index: Int) =
        RangingResult.Position(
            peer = Peer(address = PeerAddress(byteArrayOf(index.toByte()))),
            measurement = RangingMeasurement(distance = Distance.meters(index.toDouble())),
        )

    private fun distanceOf(result: RangingResult.Position): Int =
        result.measurement.distance
            ?.meters
            ?.toInt() ?: -1
}
