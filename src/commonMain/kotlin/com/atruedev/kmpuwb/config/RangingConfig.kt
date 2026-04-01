package com.atruedev.kmpuwb.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration for a UWB ranging session.
 *
 * Built via the [rangingConfig] DSL function. Immutable after construction.
 *
 * **Platform support:** [role], [channel], [sessionId], and [sessionKey] are used by
 * both Android and iOS. [rangingInterval], [angleOfArrival], and [stsMode] are accepted
 * for forward compatibility but currently ignored — Android uses automatic update rate
 * and DS-TWR config, iOS delegates to NearbyInteraction defaults.
 */
public data class RangingConfig(
    /** Role this device plays in the session. Used on both platforms. */
    val role: RangingRole,
    /** Reserved for future use. Android uses RANGING_UPDATE_RATE_AUTOMATIC. */
    val rangingInterval: Duration = DEFAULT_RANGING_INTERVAL,
    /** Reserved for future use. Both platforms report AoA when hardware supports it. */
    val angleOfArrival: Boolean = true,
    /** Reserved for future use. Android uses CONFIG_UNICAST_DS_TWR. */
    val stsMode: StsMode = StsMode.DYNAMIC,
    /** UWB channel number (5 or 9 per FiRa). Used on both platforms. */
    val channel: Int = DEFAULT_CHANNEL,
    /** Session identifier shared between controller and controlee. Used on both platforms. */
    val sessionId: Int = 0,
    /** Pre-shared key for STS. Null uses platform-generated keys. Used on Android. */
    val sessionKey: ByteArray? = null,
) {
    init {
        require(channel == 5 || channel == 9) {
            "UWB channel must be 5 or 9 per FiRa specification, was $channel"
        }
        require(rangingInterval >= MIN_RANGING_INTERVAL) {
            "Ranging interval must be >= ${MIN_RANGING_INTERVAL}, was $rangingInterval"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RangingConfig) return false
        return role == other.role &&
            rangingInterval == other.rangingInterval &&
            angleOfArrival == other.angleOfArrival &&
            stsMode == other.stsMode &&
            channel == other.channel &&
            sessionId == other.sessionId &&
            sessionKey.contentEquals(other.sessionKey)
    }

    override fun hashCode(): Int {
        var result = role.hashCode()
        result = 31 * result + rangingInterval.hashCode()
        result = 31 * result + angleOfArrival.hashCode()
        result = 31 * result + stsMode.hashCode()
        result = 31 * result + channel
        result = 31 * result + sessionId
        result = 31 * result + (sessionKey?.contentHashCode() ?: 0)
        return result
    }

    public companion object {
        public val DEFAULT_RANGING_INTERVAL: Duration = 200.milliseconds
        public val MIN_RANGING_INTERVAL: Duration = 50.milliseconds
        public const val DEFAULT_CHANNEL: Int = 9
    }
}

/**
 * DSL builder for [RangingConfig].
 */
public class RangingConfigBuilder {
    public var role: RangingRole = RangingRole.CONTROLEE
    public var rangingInterval: Duration = RangingConfig.DEFAULT_RANGING_INTERVAL
    public var angleOfArrival: Boolean = true
    public var stsMode: StsMode = StsMode.DYNAMIC
    public var channel: Int = RangingConfig.DEFAULT_CHANNEL
    public var sessionId: Int = 0
    public var sessionKey: ByteArray? = null

    public fun build(): RangingConfig =
        RangingConfig(
            role = role,
            rangingInterval = rangingInterval,
            angleOfArrival = angleOfArrival,
            stsMode = stsMode,
            channel = channel,
            sessionId = sessionId,
            sessionKey = sessionKey,
        )
}

/** Create a [RangingConfig] using a DSL builder. */
public fun rangingConfig(block: RangingConfigBuilder.() -> Unit): RangingConfig = RangingConfigBuilder().apply(block).build()
