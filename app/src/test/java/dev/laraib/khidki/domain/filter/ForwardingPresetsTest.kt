package dev.laraib.khidki.domain.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingPresetsTest {
    private val banksOnly =
        ForwardingPolicy(
            otpShopping = false,
            otpBanks = true,
            otpUpi = false,
            otpGovernment = false,
            otpOther = false,
        )

    private val governmentOnly =
        ForwardingPolicy(
            otpShopping = false,
            otpBanks = false,
            otpUpi = false,
            otpGovernment = true,
        )

    private val shoppingOnly =
        ForwardingPolicy(
            otpShopping = true,
            otpBanks = false,
            otpUpi = false,
            otpGovernment = false,
        )

    @Test
    fun epfOtp_dropsWhenOnlyBanksEnabled() {
        assertFalse(
            ForwardingPresets.matches(
                sender = "VM-EPFOHO",
                body = "Your EPF OTP is 123456 for login",
                policy = banksOnly,
            ),
        )
    }

    @Test
    fun epfOtp_forwardsWhenGovernmentEnabled() {
        assertTrue(
            ForwardingPresets.matches(
                sender = "VM-EPFOHO",
                body = "Your EPF OTP is 123456 for login",
                policy = governmentOnly,
            ),
        )
    }

    @Test
    fun hdfcOtp_forwardsWhenBanksEnabled() {
        assertTrue(
            ForwardingPresets.matches(
                sender = "VM-HDFCBK",
                body = "OTP 928371 for HDFC NetBanking login",
                policy = banksOnly,
            ),
        )
    }

    @Test
    fun hdfcDebitAlert_dropsWhenOnlyBanksOtpEnabled() {
        assertFalse(
            ForwardingPresets.matches(
                sender = "VM-HDFCBK",
                body = "Rs 5000 debited from A/C 1234",
                policy = banksOnly,
            ),
        )
    }

    @Test
    fun hdfcDebitAlert_forwardsWhenBankAlertsEnabled() {
        val policy = banksOnly.copy(alertBank = true)
        assertTrue(
            ForwardingPresets.matches(
                sender = "VM-HDFCBK",
                body = "Rs 5000 debited from A/C 1234",
                policy = policy,
            ),
        )
    }

    @Test
    fun blinkitOtp_forwardsWhenShoppingEnabled() {
        assertTrue(
            ForwardingPresets.matches(
                sender = "AD-BLNKIT",
                body = "Your Blinkit OTP is 445566",
                policy = shoppingOnly,
            ),
        )
    }

    @Test
    fun unknownOtp_dropsWhenOtherCodeDisabled() {
        assertFalse(
            ForwardingPresets.matches(
                sender = "AD-NEWAPP",
                body = "Your verification code is 778899",
                policy = banksOnly,
            ),
        )
    }

    @Test
    fun unknownOtp_forwardsWhenOtherCodeEnabled() {
        val policy = banksOnly.copy(otpOther = true)
        assertTrue(
            ForwardingPresets.matches(
                sender = "AD-NEWAPP",
                body = "Your verification code is 778899",
                policy = policy,
            ),
        )
    }

    @Test
    fun allSms_forwardsEverything() {
        assertTrue(
            ForwardingPresets.matches(
                sender = "AD-RANDOM",
                body = "Hello world",
                policy = ForwardingPolicy(allSms = true),
            ),
        )
    }
}
