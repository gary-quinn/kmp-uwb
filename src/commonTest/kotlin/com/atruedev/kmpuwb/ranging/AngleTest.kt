package com.atruedev.kmpuwb.ranging

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AngleTest {
    @Test
    fun degreesCreatesValidAngle() {
        val angle = Angle.degrees(45.0)
        assertEquals(45.0, angle.degrees)
    }

    @Test
    fun radiansConvertsCorrectly() {
        val angle = Angle.radians(PI / 2)
        assertTrue(abs(angle.degrees - 90.0) < 0.0001)
    }

    @Test
    fun degreesToRadians() {
        val angle = 180.0.degrees
        assertTrue(abs(angle.radians - PI) < 0.0001)
    }

    @Test
    fun extensionPropertyCreatesAngle() {
        val angle = 45.degrees
        assertEquals(45.0, angle.degrees)
    }

    @Test
    fun anglesAreComparable() {
        val small = 10.0.degrees
        val large = 90.0.degrees
        assertTrue(small < large)
    }

    @Test
    fun negativeAnglesAreValid() {
        val angle = Angle.degrees(-45.0)
        assertEquals(-45.0, angle.degrees)
    }

    @Test
    fun infiniteAngleThrows() {
        assertFailsWith<IllegalArgumentException> {
            Angle.degrees(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun nanAngleThrows() {
        assertFailsWith<IllegalArgumentException> {
            Angle.degrees(Double.NaN)
        }
    }

    @Test
    fun infiniteRadiansThrows() {
        assertFailsWith<IllegalArgumentException> {
            Angle.radians(Double.NEGATIVE_INFINITY)
        }
    }
}
