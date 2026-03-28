package com.atruedev.kmpuwb.ranging

import kotlin.jvm.JvmInline

/**
 * An angle measurement from a UWB ranging session.
 *
 * Represents azimuth (horizontal) or elevation (vertical) angles
 * reported by Angle of Arrival (AoA) measurements.
 * Values are in degrees, where 0 is directly ahead.
 */
@JvmInline
public value class Angle private constructor(public val degrees: Double) : Comparable<Angle> {

    /** Angle in radians. */
    public val radians: Double
        get() = degrees * (kotlin.math.PI / 180.0)

    override fun compareTo(other: Angle): Int = degrees.compareTo(other.degrees)

    override fun toString(): String {
        val rounded = kotlin.math.round(degrees * 10.0) / 10.0
        return "$rounded°"
    }

    public companion object {
        /** Create an [Angle] from degrees. */
        public fun degrees(value: Double): Angle = Angle(value)

        /** Create an [Angle] from radians. */
        public fun radians(value: Double): Angle = Angle(value * (180.0 / kotlin.math.PI))
    }
}

/** Converts a [Double] representing degrees to an [Angle]. */
public val Double.degrees: Angle get() = Angle.degrees(this)

/** Converts an [Int] representing degrees to an [Angle]. */
public val Int.degrees: Angle get() = Angle.degrees(this.toDouble())
