package com.atruedev.kmpuwb.error

/**
 * Root of the UWB error hierarchy.
 *
 * Uses sealed interfaces to enable composable error handling —
 * an error can implement multiple interfaces (e.g., [ChipsetError]
 * is both a [RangingError] and a [HardwareError]).
 *
 * All errors carry a human-readable [message] and an optional
 * [cause] for chaining platform exceptions.
 */
public sealed interface UwbError {
    public val message: String
    public val cause: Throwable?
}

/** Errors related to UWB ranging session lifecycle. */
public sealed interface SessionError : UwbError

/** Errors occurring during active ranging. */
public sealed interface RangingError : UwbError

/** Errors related to UWB hardware or chipset capabilities. */
public sealed interface HardwareError : UwbError

/** Errors related to security, STS verification, or credential management. */
public sealed interface SecurityError : UwbError

/** Errors related to out-of-band peer connection and parameter exchange. */
public sealed interface ConnectionError : UwbError

/** The ranging session was lost unexpectedly after being established. */
public data class SessionLost(
    override val message: String,
    override val cause: Throwable? = null,
) : SessionError

/** The remote peer rejected the session. */
public data class SessionRejected(
    override val message: String,
    override val cause: Throwable? = null,
) : SessionError

/** A peer became unreachable during an active session. */
public data class PeerUnreachable(
    override val message: String,
    override val cause: Throwable? = null,
) : RangingError

/** A chipset-level error — composable as both [RangingError] and [HardwareError]. */
public data class ChipsetError(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : RangingError, HardwareError

/** The requested feature is not supported by this device's UWB hardware. */
public data class UnsupportedFeature(
    val feature: String,
    override val message: String = "UWB feature not supported: $feature",
    override val cause: Throwable? = null,
) : HardwareError

/** UWB is not available on this device (no hardware or disabled). */
public data class UwbUnavailable(
    override val message: String = "UWB is not available on this device",
    override val cause: Throwable? = null,
) : HardwareError

/** STS verification failed during secure ranging. */
public data class StsVerificationFailed(
    override val message: String = "STS verification failed",
    override val cause: Throwable? = null,
) : SecurityError

/** The out-of-band parameter exchange with the remote peer failed. */
public data class OobExchangeFailed(
    override val message: String,
    override val cause: Throwable? = null,
) : ConnectionError
