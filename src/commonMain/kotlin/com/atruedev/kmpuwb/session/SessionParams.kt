package com.atruedev.kmpuwb.session

/**
 * Opaque container for platform-specific UWB session parameters.
 *
 * On Android this encodes the local UWB address, complex channel, session ID, and key.
 * On iOS this encodes a serialized `NIDiscoveryToken`.
 *
 * Applications should not inspect or construct these bytes directly — obtain them
 * from [PreparedSession.localParams] and exchange them with the remote peer over
 * any out-of-band transport (BLE, NFC, WiFi, etc.).
 */
public class SessionParams(bytes: ByteArray) {
    private val bytes: ByteArray = bytes.copyOf()

    /** Size of the encoded session parameters in bytes. */
    public val size: Int get() = bytes.size

    /** Returns a defensive copy of the raw parameter bytes for OOB transport. */
    public fun toByteArray(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionParams) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "SessionParams(${bytes.size} bytes)"
}
