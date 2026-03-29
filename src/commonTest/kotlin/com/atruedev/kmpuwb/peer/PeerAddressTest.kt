package com.atruedev.kmpuwb.peer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PeerAddressTest {

    @Test
    fun equalBytesProduceEqualAddresses() {
        val a = PeerAddress(byteArrayOf(0x01, 0x02, 0x03))
        val b = PeerAddress(byteArrayOf(0x01, 0x02, 0x03))
        assertEquals(a, b)
    }

    @Test
    fun differentBytesProduceUnequalAddresses() {
        val a = PeerAddress(byteArrayOf(0x01, 0x02))
        val b = PeerAddress(byteArrayOf(0x03, 0x04))
        assertNotEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = PeerAddress(byteArrayOf(0x0A, 0x0B))
        val b = PeerAddress(byteArrayOf(0x0A, 0x0B))
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun toByteArrayReturnsDefensiveCopy() {
        val original = byteArrayOf(0x01, 0x02)
        val address = PeerAddress(original)
        val copy = address.toByteArray()
        copy[0] = 0xFF.toByte()
        assertEquals(2, address.size)
        assertNotEquals(copy[0], address.toByteArray()[0])
    }

    @Test
    fun toStringFormatsAsHexPairs() {
        val address = PeerAddress(byteArrayOf(0x0A, 0xFF.toByte()))
        assertEquals("0A:FF", address.toString())
    }

    @Test
    fun constructorDefensivelyCopiesInput() {
        val original = byteArrayOf(0x01, 0x02)
        val address = PeerAddress(original)
        original[0] = 0xFF.toByte()
        assertEquals(0x01.toByte(), address.toByteArray()[0])
    }

    @Test
    fun emptyAddressIsValid() {
        val address = PeerAddress(byteArrayOf())
        assertEquals(0, address.size)
        assertEquals("", address.toString())
    }
}
