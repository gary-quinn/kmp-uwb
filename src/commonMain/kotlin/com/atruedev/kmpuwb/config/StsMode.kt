package com.atruedev.kmpuwb.config

/**
 * Scrambled Timestamp Sequence mode for ranging security.
 *
 * STS encrypts timing data in UWB signals to prevent spoofing.
 * Higher security modes require more setup but provide stronger
 * guarantees against relay attacks.
 */
public enum class StsMode {
    /** Fixed key, simpler setup, lower security. */
    STATIC,

    /** Keys rotate per session, higher security. */
    DYNAMIC,

    /** Pre-provisioned keys for established device relationships. Requires Android 14+. */
    PROVISIONED,
}
