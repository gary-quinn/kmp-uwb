package com.atruedev.kmpuwb.ranging

import kotlin.jvm.JvmInline
import kotlin.math.round

/**
 * A non-negative distance in meters between two UWB devices.
 *
 * Bit-packs the [Double] value into a [Long] for zero-allocation
 * on JVM via [value class]. Use [Distance.meters] or the [Double.meters]
 * extension to construct.
 */
@JvmInline
public value class Distance private constructor(private val packed: Long) : Comparable<Distance> {

    /** Distance in meters. */
    public val meters: Double
        get() = Double.fromBits(packed)

    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

    override fun toString(): String {
        val rounded = round(meters * 100.0) / 100.0
        return "${rounded}m"
    }

    public companion object {
        /** Create a [Distance] from a value in meters. */
        public fun meters(value: Double): Distance {
            require(value >= 0.0) { "Distance must be non-negative, was $value" }
            return Distance(value.toBits())
        }

        /** Create a [Distance] from a value in centimeters. */
        public fun centimeters(value: Double): Distance = meters(value / 100.0)
    }
}

/** Converts a [Double] representing meters to a [Distance]. */
public val Double.meters: Distance get() = Distance.meters(this)

/** Converts an [Int] representing meters to a [Distance]. */
public val Int.meters: Distance get() = Distance.meters(this.toDouble())
