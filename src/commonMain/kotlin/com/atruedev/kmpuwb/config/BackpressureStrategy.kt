package com.atruedev.kmpuwb.config

/**
 * Controls how ranging measurements are buffered when the consumer falls behind.
 *
 * Matches the `BackpressureStrategy` pattern in kmp-ble for ecosystem consistency.
 */
public enum class BackpressureStrategy {
    /**
     * Drop the oldest buffered measurement when the buffer is full.
     * The consumer always sees the most recent data. Default for real-time UIs.
     */
    Latest,

    /**
     * Buffer all measurements without dropping. Use for analytics or replay
     * scenarios where every measurement matters. Unbounded — long-running
     * sessions at high update rates will accumulate memory proportionally.
     */
    Buffer,

    /**
     * Drop the newest measurement when the buffer is full.
     * The consumer processes measurements in strict arrival order.
     */
    Drop,
}
