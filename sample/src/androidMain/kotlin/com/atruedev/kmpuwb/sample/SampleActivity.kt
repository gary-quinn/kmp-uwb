package com.atruedev.kmpuwb.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RangingDemoScreen() }
    }
}
