package com.atruedev.kmpuwb.peer

/**
 * A UWB device address used to identify a peer in a ranging session.
 *
 * Platform-specific: on Android this maps to a UWB address byte array,
 * on iOS this maps to the serialized NearbyInteraction discovery token.
 * The raw bytes are opaque — equality, identity, and display are the
 * valid operations across platforms.
 *
 * Uses content-based equality: two addresses with the same bytes are equal.
 */
public class PeerAddress(bytes: ByteArray) {

    private val bytes: ByteArray = bytes.copyOf()

    /** Number of bytes in the address. */
    public val size: Int get() = bytes.size

    /** Returns a defensive copy of the underlying bytes. */
    public fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PeerAddress) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String =
        bytes.joinToString(":") { it.toUByte().toString(16).padStart(2, '0').uppercase() }

    public companion object
}
