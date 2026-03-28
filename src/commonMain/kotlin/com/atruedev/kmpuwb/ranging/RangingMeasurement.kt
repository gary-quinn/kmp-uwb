package com.atruedev.kmpuwb.ranging

/**
 * A single spatial measurement from a UWB ranging session.
 *
 * Contains the distance and optional angle-of-arrival data
 * for a specific peer at a specific point in time.
 */
public data class RangingMeasurement(
    /** Measured distance to the peer. */
    val distance: Distance,
    /** Horizontal angle relative to the device. Null if AoA is unsupported or disabled. */
    val azimuth: Angle? = null,
    /** Vertical angle relative to the device. Null if AoA is unsupported or disabled. */
    val elevation: Angle? = null,
    /** Measurement accuracy as a standard deviation in meters. Null if unavailable. */
    val distanceAccuracy: Distance? = null,
)
