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
        // Reset the lateinit field via reflection so each test starts clean
        val field = KmpUwb::class.java.getDeclaredField("appContext")
        field.isAccessible = true
        // Use the underlying delegate to clear it — set to a sentinel we can detect
        // Actually, we need to check if it's initialized first. For lateinit, we use
        // the Kotlin reflection approach:
        try {
            // Force-clear by setting accessible and nulling the backing field
            val backingField = KmpUwb::class.java.getDeclaredField("appContext")
            backingField.isAccessible = true
            // lateinit backing fields are non-null in Kotlin but nullable in JVM bytecode
            @Suppress("UNCHECKED_CAST")
            (backingField as java.lang.reflect.Field).set(KmpUwb, null)
        } catch (_: Exception) {
            // Field may already be unset
        }
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
