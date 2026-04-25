package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.ranging.RangingMeasurement

/**
 * A result emitted from an active ranging session.
 *
 * Consumers collect these from [RangingSession.rangingResults] to
 * receive spatial measurements or peer lifecycle events.
 */
public sealed interface RangingResult {
    /** The peer this result relates to. */
    public val peer: Peer

    /**
     * A successful spatial measurement.
     *
     * Contains distance and optional angle-of-arrival data.
     */
    public data class Position(
        override val peer: Peer,
        val measurement: RangingMeasurement,
    ) : RangingResult

    /**
     * The peer moved out of range.
     *
     * The session remains active - measurements will resume
     * when the peer is detectable again.
     */
    public data class PeerLost(
        override val peer: Peer,
    ) : RangingResult

    /**
     * The peer is back in range after being lost.
     */
    public data class PeerRecovered(
        override val peer: Peer,
        val measurement: RangingMeasurement,
    ) : RangingResult
}
