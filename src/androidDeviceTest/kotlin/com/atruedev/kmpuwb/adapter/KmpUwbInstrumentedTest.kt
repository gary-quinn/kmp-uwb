package com.atruedev.kmpuwb.adapter

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class KmpUwbInstrumentedTest {
    @Test
    fun requireContextDoesNotThrowAfterStartup() {
        // AndroidX Startup should have already initialized KmpUwb
        val context = KmpUwb.requireContext()
        assertNotNull(context)
    }

    @Test
    fun requireContextReturnsApplicationContext() {
        val context = KmpUwb.requireContext()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        assertIs<Application>(context)
        // Should be the same application instance
        kotlin.test.assertEquals(appContext, context)
    }
}
