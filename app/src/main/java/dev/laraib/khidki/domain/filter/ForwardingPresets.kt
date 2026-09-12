package dev.laraib.khidki.domain.filter

enum class MessageKind {
    OTP,
    ALERT,
    OTHER,
}

enum class OtpDomain {
    SHOPPING,
    BANKS,
    UPI,
    GOVERNMENT,
    OTHER,
}

enum class AlertDomain {
    BANK,
    ORDER,
}

data class MessageClassification(
    val kind: MessageKind,
    val otpDomain: OtpDomain? = null,
    val alertDomain: AlertDomain? = null,
    val matchedCustomSender: CustomSenderRule? = null,
)

object ForwardingPresets {
    val shoppingTokens =
        listOf(
            "BLNKIT", "BLINKIT", "AMAZON", "FLIPKART", "SWIGGY", "ZOMATO",
            "DUNZO", "MYNTRA", "MEESHO", "NYKAA", "BIGBASK", "ZEPTO",
            "INSTAM", "DOMINO", "PAYTMML",
        )

    val bankTokens =
        listOf(
            "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "IDFC", "PNB", "BOB",
            "CANARA", "UNION", "YESBNK", "INDUS", "FEDERAL", "RBL", "CITI",
            "SCBANK", "HSBC", "DBS", "BANDHAN",
        )

    val upiTokens =
        listOf(
            "PHONEPE", "GPAY", "GOOGLEP", "PAYTM", "BHIM", "CRED", "MOBIKW",
            "FREECH", "AMAZONP",
        )

    val governmentTokens =
        listOf(
            "EPFO", "EPF", "UAN", "NPS", "ITD", "ITAX", "INCOMETAX", "GST",
            "GSTIN", "AADHAAR", "AADHAR", "UIDAI", "DIGILOCK", "PARIVAHAN",
            "CGHS", "MCA", "UMANG",
        )

    private val bankAlertTokens =
        listOf("DEBIT", "CREDITED", "CREDIT", "WITHDRAW", "TXN", "TRANSACTION", "A/C", "ACCOUNT")

    private val orderAlertTokens =
        listOf("DELIVER", "SHIPPED", "DISPATCH", "OUT FOR", "ORDER", "ARRIVING", "PICKED")

    private val otpWordPattern =
        Regex("(?i)(otp|one[\\s-]?time|verification(?:\\s*code)?|passcode|auth(?:entication)?\\s*code)")

    private val otpCodePattern = Regex("\\b\\d{4,8}\\b")

    fun classify(sender: String, body: String): MessageClassification {
        val senderUpper = sender.uppercase()
        if (isOtpBody(body)) {
            val domain = classifyOtpDomain(senderUpper)
            return MessageClassification(kind = MessageKind.OTP, otpDomain = domain)
        }
        if (isAlertBody(body)) {
            val domain =
                when {
                    bankAlertTokens.any { body.contains(it, ignoreCase = true) } -> AlertDomain.BANK
                    orderAlertTokens.any { body.contains(it, ignoreCase = true) } -> AlertDomain.ORDER
                    else -> AlertDomain.ORDER
                }
            return MessageClassification(kind = MessageKind.ALERT, alertDomain = domain)
        }
        return MessageClassification(kind = MessageKind.OTHER)
    }

    fun classifyWithCustomSenders(
        sender: String,
        body: String,
        customSenders: List<CustomSenderRule>,
    ): MessageClassification {
        val senderUpper = sender.uppercase()
        val custom = matchCustomSender(senderUpper, customSenders)
        if (custom != null) {
            return MessageClassification(
                kind = if (custom.codesOnly && isOtpBody(body)) MessageKind.OTP else MessageKind.OTHER,
                matchedCustomSender = custom,
            )
        }
        return classify(sender, body)
    }

    fun shouldForward(classification: MessageClassification, policy: ForwardingPolicy): Boolean {
        if (policy.allSms) {
            return true
        }
        classification.matchedCustomSender?.let { return true }
        return when (classification.kind) {
            MessageKind.OTP ->
                when (classification.otpDomain) {
                    OtpDomain.SHOPPING -> policy.otpShopping
                    OtpDomain.BANKS -> policy.otpBanks
                    OtpDomain.UPI -> policy.otpUpi
                    OtpDomain.GOVERNMENT -> policy.otpGovernment
                    OtpDomain.OTHER -> policy.otpOther
                    null -> false
                }
            MessageKind.ALERT ->
                when (classification.alertDomain) {
                    AlertDomain.BANK -> policy.alertBank
                    AlertDomain.ORDER -> policy.alertOrder
                    null -> false
                }
            MessageKind.OTHER -> false
        }
    }

    fun matches(sender: String, body: String, policy: ForwardingPolicy): Boolean {
        if (policy.allSms) {
            return true
        }
        val classification = classifyWithCustomSenders(sender, body, policy.customSenders)
        if (classification.matchedCustomSender != null) {
            val rule = classification.matchedCustomSender
            return !rule.codesOnly || isOtpBody(body)
        }
        return shouldForward(classification, policy)
    }

    private fun isOtpBody(body: String): Boolean =
        otpWordPattern.containsMatchIn(body) && otpCodePattern.containsMatchIn(body)

    private fun isAlertBody(body: String): Boolean =
        !otpWordPattern.containsMatchIn(body) &&
            (bankAlertTokens.any { body.contains(it, ignoreCase = true) } ||
                orderAlertTokens.any { body.contains(it, ignoreCase = true) })

    private fun classifyOtpDomain(senderUpper: String): OtpDomain {
        if (containsAny(senderUpper, governmentTokens)) {
            return OtpDomain.GOVERNMENT
        }
        if (containsAny(senderUpper, shoppingTokens)) {
            return OtpDomain.SHOPPING
        }
        if (containsAny(senderUpper, bankTokens)) {
            return OtpDomain.BANKS
        }
        if (containsAny(senderUpper, upiTokens)) {
            return OtpDomain.UPI
        }
        return OtpDomain.OTHER
    }

    private fun matchCustomSender(
        senderUpper: String,
        customSenders: List<CustomSenderRule>,
    ): CustomSenderRule? =
        customSenders.firstOrNull { rule ->
            senderUpper.contains(rule.senderContains.uppercase())
        }

    private fun containsAny(haystack: String, tokens: List<String>): Boolean =
        tokens.any { haystack.contains(it) }
}
