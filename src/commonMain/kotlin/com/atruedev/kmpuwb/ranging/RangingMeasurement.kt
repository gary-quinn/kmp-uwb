package com.atruedev.kmpuwb.ranging

/**
 * A single spatial measurement from a UWB ranging session.
 *
 * Contains the distance and optional angle-of-arrival data
 * for a specific peer at a specific point in time.
 */
public data class RangingMeasurement(
    /** Measured distance to the peer. Null when the device cannot determine distance. */
    val distance: Distance? = null,
    val azimuth: Angle? = null,
    val elevation: Angle? = null,
    val distanceAccuracy: Distance? = null,
)
