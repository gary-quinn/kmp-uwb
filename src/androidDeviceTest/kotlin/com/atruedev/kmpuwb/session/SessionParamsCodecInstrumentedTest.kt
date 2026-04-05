package com.atruedev.kmpuwb.session

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Runs a subset of SessionParamsCodec tests on a real Android device/emulator
 * to validate against real framework classes (catches Robolectric shadow fidelity issues).
 */
@RunWith(AndroidJUnit4::class)
class SessionParamsCodecInstrumentedTest {
    @Test
    fun roundTripEncodeDecodeOnDevice() {
        val address = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
        val channel = 9
        val preambleIndex = 10
        val sessionId = 42

        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(address),
                complexChannel = UwbComplexChannel(channel, preambleIndex),
                sessionId = sessionId,
            )

        val decoded = SessionParamsCodec.decode(encoded)

        assertContentEquals(address, decoded.address)
        assertEquals(channel, decoded.channel)
        assertEquals(preambleIndex, decoded.preambleIndex)
        assertEquals(sessionId, decoded.sessionId)
    }

    @Test
    fun wireFormatMatchesSpecOnDevice() {
        val address = byteArrayOf(0x01, 0x02)
        val channel = 9
        val preambleIndex = 5
        val sessionId = 0x12345678

        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(address),
                complexChannel = UwbComplexChannel(channel, preambleIndex),
                sessionId = sessionId,
            )

        val bytes = encoded.toByteArray()
        assertEquals(0x01, bytes[0]) // version
        assertEquals(2, bytes[1].toInt()) // addressLen
        assertEquals(0x01, bytes[2]) // address[0]
        assertEquals(0x02, bytes[3]) // address[1]
        assertEquals(channel.toByte(), bytes[4]) // channel
        assertEquals(preambleIndex.toByte(), bytes[5]) // preambleIndex

        val sessionIdBytes = ByteBuffer.wrap(bytes, 6, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(sessionId, sessionIdBytes)
    }
}
