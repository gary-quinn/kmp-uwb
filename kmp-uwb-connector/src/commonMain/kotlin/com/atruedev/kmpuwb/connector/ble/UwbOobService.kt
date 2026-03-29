package com.atruedev.kmpuwb.connector.ble

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * BLE GATT service definition for UWB out-of-band parameter exchange.
 *
 * The controller writes its [SessionParams] to [CONTROLLER_PARAMS_UUID].
 * The controlee exposes its [SessionParams] on [CONTROLEE_PARAMS_UUID]
 * via read + notify.
 */
@OptIn(ExperimentalUuidApi::class)
public object UwbOobService {
    public val SERVICE_UUID: Uuid =
        Uuid.parse("bd248b8e-2550-475b-94dd-54531845b2f1")

    public val CONTROLLER_PARAMS_UUID: Uuid =
        Uuid.parse("3591a788-b42f-46cc-a4b3-a52cc0587ed4")

    public val CONTROLEE_PARAMS_UUID: Uuid =
        Uuid.parse("6d115a7d-5042-4e84-9f3d-9b833f3862c1")
}
