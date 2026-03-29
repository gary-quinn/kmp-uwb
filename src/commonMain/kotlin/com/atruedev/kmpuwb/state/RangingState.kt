package com.atruedev.kmpuwb.state

import com.atruedev.kmpuwb.error.UwbError

/**
 * Exhaustive state machine for a UWB ranging session lifecycle.
 *
 * Mirrors the kmp-ble approach: every state communicates *why*
 * you're in that state, not just *that* you are. This enables
 * precise error recovery and UI feedback.
 *
 * Transition order:
 * [Idle] → [Starting] → [Active] → [Stopped]
 *
 * Any non-terminal state can transition to [Stopped] on error.
 */
public sealed interface RangingState {

    /** Session exists but has not started ranging. */
    public sealed interface Idle : RangingState {
        /** Ready to start. */
        public data object Ready : Idle

        /** UWB hardware is not available on this device. */
        public data object Unsupported : Idle
    }

    /** Session is being established. */
    public sealed interface Starting : RangingState {
        /** Negotiating session parameters with the peer. */
        public data object Negotiating : Starting

        /** Parameters exchanged, waiting for first measurement. */
        public data object Initializing : Starting
    }

    /** Session is actively ranging. */
    public sealed interface Active : RangingState {
        /** Receiving measurements normally. */
        public data object Ranging : Active

        /** App moved to background; ranging may be paused depending on platform. */
        public data object Suspended : Active

        /** Peer temporarily out of range but session is maintained. */
        public data object PeerLost : Active
    }

    /** Session has ended. */
    public sealed interface Stopped : RangingState {
        /** Clean shutdown initiated locally. */
        public data object ByRequest : Stopped

        /** Remote peer ended the session. */
        public data object ByPeer : Stopped

        /** Session ended due to an error. */
        public data class ByError(val error: UwbError) : Stopped

        /** Session ended because UWB adapter was turned off. */
        public data object BySystemEvent : Stopped
    }
}
