package com.atruedev.kmpuwb.ranging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DistanceTest {
    @Test
    fun metersCreatesValidDistance() {
        val distance = Distance.meters(5.0)
        assertEquals(5.0, distance.meters)
    }

    @Test
    fun centimetersConvertsToMeters() {
        val distance = Distance.centimeters(150.0)
        assertEquals(1.5, distance.meters)
    }

    @Test
    fun extensionPropertyCreatesDistance() {
        val distance = 3.5.meters
        assertEquals(3.5, distance.meters)
    }

    @Test
    fun intExtensionPropertyCreatesDistance() {
        val distance = 2.meters
        assertEquals(2.0, distance.meters)
    }

    @Test
    fun negativeDistanceThrows() {
        assertFailsWith<IllegalArgumentException> {
            Distance.meters(-1.0)
        }
    }

    @Test
    fun infiniteDistanceThrows() {
        assertFailsWith<IllegalArgumentException> {
            Distance.meters(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun nanDistanceThrows() {
        assertFailsWith<IllegalArgumentException> {
            Distance.meters(Double.NaN)
        }
    }

    @Test
    fun zeroDistanceIsValid() {
        val distance = Distance.meters(0.0)
        assertEquals(0.0, distance.meters)
    }

    @Test
    fun distancesAreComparable() {
        val near = 1.0.meters
        val far = 5.0.meters
        assertTrue(near < far)
        assertTrue(far > near)
    }

    @Test
    fun equalDistancesCompareAsEqual() {
        val a = Distance.meters(3.0)
        val b = Distance.meters(3.0)
        assertEquals(0, a.compareTo(b))
    }
}
