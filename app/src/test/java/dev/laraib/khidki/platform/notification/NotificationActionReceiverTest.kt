package dev.laraib.khidki.platform.notification

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dev.laraib.khidki.KhidkiRuntime
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.domain.model.DestinationSnapshot
import dev.laraib.khidki.domain.session.ForwardingEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NotificationActionReceiverTest {
    private lateinit var runtime: KhidkiRuntime
    private lateinit var receiver: NotificationActionReceiver

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        runtime = KhidkiRuntime.get(context)
        receiver = NotificationActionReceiver()
        runtime.engine.cancelTimedWindow()
        runtime.engine.cancelActiveWindow()
        runtime.appPreferences.isMasterEnabled = true
        runtime.refreshAppState(true)
    }

    @Test
    fun stopAction_cancelsActiveForwarding() {
        val requester = dev.laraib.khidki.domain.model.CanonicalPhone("+919876543210")
        val armed =
            runtime.engine.armTimedWindow(
                listOf(
                    DestinationSnapshot(
                        id = "one",
                        name = "One",
                        phone = requester,
                        policy = ForwardingPolicy(otpBanks = true),
                    ),
                ),
                900,
            )
        assertTrue(armed is dev.laraib.khidki.domain.model.TimedArmResult.Success)

        receiver.onReceive(
            ApplicationProvider.getApplicationContext(),
            Intent(NotificationActionReceiver.ACTION_STOP_FORWARDING),
        )

        runBlocking {
            assertNull(runtime.container.sessionRepository.getActiveSession(runtime.clock.nowMillis()))
        }
    }
}
