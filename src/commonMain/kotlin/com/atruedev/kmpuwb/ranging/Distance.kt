package com.atruedev.kmpuwb.ranging

import kotlin.jvm.JvmInline

/**
 * A measured distance between two UWB devices.
 *
 * Wraps a value in meters with an associated accuracy estimate.
 * Accuracy represents the standard deviation of the measurement —
 * smaller values indicate higher confidence.
 */
@JvmInline
public value class Distance private constructor(private val packed: Long) : Comparable<Distance> {

    /** Distance in meters. */
    public val meters: Double
        get() = Double.fromBits(packed)

    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

    override fun toString(): String = "${roundTo(meters, 2)}m"

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

private fun roundTo(value: Double, decimals: Int): String {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    if (dotIndex == -1) return "$str.${"0".repeat(decimals)}"
    val currentDecimals = str.length - dotIndex - 1
    return if (currentDecimals >= decimals) str.substring(0, dotIndex + decimals + 1) else str + "0".repeat(decimals - currentDecimals)
}
