package com.atruedev.kmpuwb.session

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class SessionParamsCodecTest {
    @Test
    fun roundTripEncodeDecodePreservesAllFields() {
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
    fun wireFormatMatchesSpec() {
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

        // sessionId as big-endian int
        val sessionIdBytes = ByteBuffer.wrap(bytes, 6, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(sessionId, sessionIdBytes)
    }

    @Test
    fun variableLengthAddress1Byte() {
        val address = byteArrayOf(0xFF.toByte())
        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(address),
                complexChannel = UwbComplexChannel(5, 0),
                sessionId = 1,
            )
        val decoded = SessionParamsCodec.decode(encoded)
        assertContentEquals(address, decoded.address)
    }

    @Test
    fun variableLengthAddress8Bytes() {
        val address = ByteArray(8) { it.toByte() }
        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(address),
                complexChannel = UwbComplexChannel(9, 3),
                sessionId = 99,
            )
        val decoded = SessionParamsCodec.decode(encoded)
        assertContentEquals(address, decoded.address)
        assertEquals(8, decoded.address.size)
    }

    @Test
    fun decodeRejectsWrongVersion() {
        val bytes =
            byteArrayOf(
                0x81.toByte(), // iOS version instead of Android
                0x02,
                0x01,
                0x02, // address
                0x09,
                0x00, // channel, preamble
                0x00,
                0x00,
                0x00,
                0x01, // sessionId
            )
        assertFailsWith<IllegalArgumentException> {
            SessionParamsCodec.decode(SessionParams(bytes))
        }
    }

    @Test
    fun decodeRejectsTruncatedHeader() {
        val bytes = byteArrayOf(0x01) // only version, no addressLen
        assertFailsWith<IllegalArgumentException> {
            SessionParamsCodec.decode(SessionParams(bytes))
        }
    }

    @Test
    fun decodeRejectsTruncatedBody() {
        val bytes =
            byteArrayOf(
                0x01, // version
                0x04, // addressLen = 4, but only 2 bytes of address follow
                0x01,
                0x02,
            )
        assertFailsWith<IllegalArgumentException> {
            SessionParamsCodec.decode(SessionParams(bytes))
        }
    }

    @Test
    fun sessionIdMaxValue() {
        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(byteArrayOf(0x01)),
                complexChannel = UwbComplexChannel(9, 0),
                sessionId = Int.MAX_VALUE,
            )
        val decoded = SessionParamsCodec.decode(encoded)
        assertEquals(Int.MAX_VALUE, decoded.sessionId)
    }

    @Test
    fun sessionIdMinValue() {
        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(byteArrayOf(0x01)),
                complexChannel = UwbComplexChannel(9, 0),
                sessionId = Int.MIN_VALUE,
            )
        val decoded = SessionParamsCodec.decode(encoded)
        assertEquals(Int.MIN_VALUE, decoded.sessionId)
    }

    @Test
    fun sessionIdZero() {
        val encoded =
            SessionParamsCodec.encode(
                address = UwbAddress(byteArrayOf(0x01)),
                complexChannel = UwbComplexChannel(9, 0),
                sessionId = 0,
            )
        val decoded = SessionParamsCodec.decode(encoded)
        assertEquals(0, decoded.sessionId)
    }

    @Test
    fun decodedParamsEquality() {
        val a =
            SessionParamsCodec.DecodedParams(
                address = byteArrayOf(0x01, 0x02),
                channel = 9,
                preambleIndex = 5,
                sessionId = 42,
            )
        val b =
            SessionParamsCodec.DecodedParams(
                address = byteArrayOf(0x01, 0x02),
                channel = 9,
                preambleIndex = 5,
                sessionId = 42,
            )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
