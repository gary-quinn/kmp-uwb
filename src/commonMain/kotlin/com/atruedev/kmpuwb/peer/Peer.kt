package com.atruedev.kmpuwb.peer

/**
 * A UWB peer discovered or participating in a ranging session.
 *
 * Peers are identified by their [address] and expose the latest
 * known capabilities negotiated during session setup.
 */
public data class Peer(
    /** Unique address of this peer within the ranging session. */
    val address: PeerAddress,
    /** Human-readable name, if available from OOB discovery. */
    val name: String? = null,
)
