package com.atruedev.kmpuwb.sample.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.atruedev.kmpuwb.sample.RangingDemoScreen

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                setContent { RangingDemoScreen() }
            } else {
                Toast.makeText(this, "UWB and Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions =
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.UWB_RANGING)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_ADVERTISE)
                }
            }

        if (permissions.isEmpty()) {
            setContent { RangingDemoScreen() }
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
