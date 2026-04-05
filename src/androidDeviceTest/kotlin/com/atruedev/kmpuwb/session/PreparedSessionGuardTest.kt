package com.atruedev.kmpuwb.session

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.config.rangingConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests PreparedSession lifecycle guards on real UWB hardware.
 * All tests are skipped on devices without UWB support.
 */
@RunWith(AndroidJUnit4::class)
class PreparedSessionGuardTest {
    @Before
    fun requireUwbHardware() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "UWB hardware required",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB),
        )
    }

    @Test
    fun prepareSessionReturnsNonEmptyLocalParams() =
        runTest {
            val adapter = UwbAdapter()
            val config =
                rangingConfig {
                    role = RangingRole.CONTROLLER
                    sessionId = 1
                }
            val prepared = adapter.prepareSession(config)
            assertTrue(prepared.localParams.toByteArray().isNotEmpty())
            prepared.close()
            adapter.close()
        }

    @Test
    fun localParamsDecodesCorrectly() =
        runTest {
            val adapter = UwbAdapter()
            val config =
                rangingConfig {
                    role = RangingRole.CONTROLLER
                    sessionId = 42
                }
            val prepared = adapter.prepareSession(config)
            val decoded = SessionParamsCodec.decode(prepared.localParams)
            assertTrue(decoded.address.isNotEmpty())
            assertTrue(decoded.channel > 0)
            prepared.close()
            adapter.close()
        }

    @Test
    fun closePreventsFurtherStartRanging() =
        runTest {
            val adapter = UwbAdapter()
            val config =
                rangingConfig {
                    role = RangingRole.CONTROLLER
                    sessionId = 1
                }
            val prepared = adapter.prepareSession(config)
            prepared.close()
            assertFailsWith<IllegalStateException> {
                prepared.startRanging(SessionParams(byteArrayOf(0x01, 0x02)))
            }
            adapter.close()
        }
}
