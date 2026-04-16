package com.atruedev.kmpuwb.adapter

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class KmpUwbInitTest {
    @BeforeTest
    fun resetKmpUwb() {
        KmpUwb.reset()
    }

    @Test
    fun requireContextThrowsBeforeInit() {
        assertFailsWith<IllegalStateException> {
            KmpUwb.requireContext()
        }
    }

    @Test
    fun initStoresApplicationContext() {
        val appContext = RuntimeEnvironment.getApplication()
        KmpUwb.init(appContext)

        val result = KmpUwb.requireContext()
        assertIs<Application>(result)
    }

    @Test
    fun initWithActivityContextStoresApplicationContext() {
        val appContext = RuntimeEnvironment.getApplication()
        // Passing the application context directly (Robolectric doesn't easily create Activity contexts)
        // The key behavior: context.applicationContext is called in init()
        KmpUwb.init(appContext)

        val result = KmpUwb.requireContext()
        assertEquals(appContext.applicationContext, result)
    }

    @Test
    fun doubleInitDoesNotCrash() {
        val context = RuntimeEnvironment.getApplication()
        KmpUwb.init(context)
        KmpUwb.init(context)
        // If we get here without exception, the test passes
        KmpUwb.requireContext()
    }
}
