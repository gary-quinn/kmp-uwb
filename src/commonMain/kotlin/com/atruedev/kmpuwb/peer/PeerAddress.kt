package com.atruedev.kmpuwb.peer

import kotlin.jvm.JvmInline

/**
 * A UWB device address used to identify a peer in a ranging session.
 *
 * Platform-specific: on Android this maps to a UWB address byte array,
 * on iOS this maps to the NearbyInteraction discovery token.
 * The raw bytes are opaque — equality and identity are the only
 * valid operations across platforms.
 */
@JvmInline
public value class PeerAddress(public val bytes: ByteArray) {

    /** Number of bytes in the address. */
    public val size: Int get() = bytes.size

    override fun toString(): String =
        bytes.joinToString(":") { it.toUByte().toString(16).padStart(2, '0').uppercase() }
}
