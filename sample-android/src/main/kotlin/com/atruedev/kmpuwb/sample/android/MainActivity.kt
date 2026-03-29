package com.atruedev.kmpuwb.sample.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.atruedev.kmpuwb.sample.RangingDemoScreen
import com.atruedev.kmpuwb.sample.initClipboard

class MainActivity : ComponentActivity() {
    private val permissionGranted = mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initClipboard(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.UWB_RANGING) == PackageManager.PERMISSION_GRANTED) {
                permissionGranted.value = true
            } else {
                permissionLauncher.launch(Manifest.permission.UWB_RANGING)
            }
        } else {
            permissionGranted.value = true
        }

        setContent {
            if (permissionGranted.value) {
                RangingDemoScreen()
            }
        }
    }
}
