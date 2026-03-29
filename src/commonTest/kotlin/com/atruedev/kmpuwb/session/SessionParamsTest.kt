package com.atruedev.kmpuwb.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class SessionParamsTest {
    @Test
    fun equalityBasedOnContent() {
        val a = SessionParams(byteArrayOf(0x01, 0x02, 0x03))
        val b = SessionParams(byteArrayOf(0x01, 0x02, 0x03))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun differentContentNotEqual() {
        val a = SessionParams(byteArrayOf(0x01, 0x02))
        val b = SessionParams(byteArrayOf(0x03, 0x04))
        assertNotEquals(a, b)
    }

    @Test
    fun defensiveCopyOnConstruction() {
        val source = byteArrayOf(0x01, 0x02)
        val params = SessionParams(source)
        source[0] = 0xFF.toByte()
        assertEquals(0x01, params.toByteArray()[0])
    }

    @Test
    fun defensiveCopyOnAccess() {
        val params = SessionParams(byteArrayOf(0x01, 0x02))
        val first = params.toByteArray()
        val second = params.toByteArray()
        assertNotSame(first, second)
        first[0] = 0xFF.toByte()
        assertEquals(0x01, params.toByteArray()[0])
    }

    @Test
    fun sizeReportsCorrectLength() {
        assertEquals(0, SessionParams(byteArrayOf()).size)
        assertEquals(4, SessionParams(byteArrayOf(1, 2, 3, 4)).size)
    }

    @Test
    fun toStringShowsByteCount() {
        val params = SessionParams(byteArrayOf(1, 2, 3))
        assertEquals("SessionParams(3 bytes)", params.toString())
    }
}
