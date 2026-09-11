package dev.laraib.khidki.platform.permission

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PermissionGateTest {
    @Test
    fun createAppDetailsIntent_targetsPackage() {
        val intent = PermissionGate.createAppDetailsIntent("dev.laraib.khidki.debug")
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:dev.laraib.khidki.debug", intent.data.toString())
    }

    @Test
    fun sideloadSetupSteps_listsRequiredSteps() {
        val steps = PermissionGate.sideloadSetupSteps()
        assertEquals(3, steps.size)
        assertTrue(steps.first().contains("restricted settings"))
        assertTrue(steps.last().contains("Realme"))
    }

    @Test
    fun hasSmsPermissions_falseByDefault() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(PermissionGate.hasSmsPermissions(context))
    }
}
