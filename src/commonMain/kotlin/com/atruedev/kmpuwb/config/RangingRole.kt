package com.atruedev.kmpuwb.config

/**
 * The role a device plays in a UWB ranging session.
 *
 * In the FiRa standard, the controller initiates and manages
 * the session parameters, while the controlee responds.
 */
public enum class RangingRole {
    /** Initiates the session, determines the complex channel and timing. */
    CONTROLLER,

    /** Responds to the controller's session parameters. */
    CONTROLEE,
}
