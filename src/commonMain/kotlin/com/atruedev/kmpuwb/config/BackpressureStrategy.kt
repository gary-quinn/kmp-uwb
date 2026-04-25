package com.atruedev.kmpuwb.config

/**
 * Controls how ranging measurements are buffered when the consumer falls behind.
 *
 * Passed to [com.atruedev.kmpuwb.session.PreparedSession.startRanging] to configure
 * the result delivery channel independently of UWB protocol parameters.
 */
public enum class BackpressureStrategy {
    /**
     * Drop the oldest measurement when the buffer is full.
     * The consumer always sees the most recent data.
     */
    KeepLatest,

    /**
     * Buffer all measurements without dropping. Long-running sessions at high
     * update rates will accumulate memory proportionally - callers are responsible
     * for bounding session lifetime or draining results promptly.
     */
    Unbounded,

    /**
     * Drop the newest measurement when the buffer is full.
     * The consumer processes measurements in strict arrival order.
     */
    KeepOldest,
}
