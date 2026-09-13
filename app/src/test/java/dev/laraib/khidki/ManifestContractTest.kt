package dev.laraib.khidki

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestContractTest {
    @Test
    fun manifest_declaresSmsPermissionsOnly() {
        val path = System.getProperty("khidki.manifest")
            ?: error("khidki.manifest system property not set")
        val xml = File(path).readText()
        assertTrue(xml.contains("android.permission.RECEIVE_SMS"))
        assertTrue(xml.contains("android.permission.SEND_SMS"))
        assertTrue(xml.contains("BROADCAST_SMS"))
        assertTrue(xml.contains("android.provider.Telephony.SMS_RECEIVED"))
        assertFalse(xml.contains("android.permission.INTERNET"))
        assertFalse(xml.contains("android.permission.READ_SMS"))
        assertFalse(xml.contains("BIND_NOTIFICATION_LISTENER_SERVICE"))
    }
}
