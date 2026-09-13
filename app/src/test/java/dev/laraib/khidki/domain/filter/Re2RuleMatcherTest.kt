package dev.laraib.khidki.domain.filter

import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.RuleMatchOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Re2RuleMatcherTest {
    private val matcher = Re2RuleMatcher()

    private val validRules = FilterRules(
        senderPatterns = listOf("HDFCBK"),
        contentPatterns = listOf("\\d{6}"),
    )

    @Test
    fun isValid_requiresSenderAndContentPatterns() {
        assertFalse(
            matcher.isValid(
                FilterRules(
                    senderPatterns = listOf("HDFCBK"),
                    contentPatterns = emptyList(),
                ),
            ),
        )
        assertFalse(
            matcher.isValid(
                FilterRules(
                    senderPatterns = emptyList(),
                    contentPatterns = listOf("\\d{6}"),
                ),
            ),
        )
        assertTrue(matcher.isValid(validRules))
    }

    @Test
    fun isValid_rejectsInvalidRegex() {
        assertFalse(
            matcher.isValid(
                validRules.copy(senderPatterns = listOf("(")),
            ),
        )
    }

    @Test
    fun isValid_rejectsPatternLongerThan512() {
        val longPattern = "a".repeat(Re2RuleMatcher.MAX_PATTERN_LENGTH + 1)
        assertFalse(
            matcher.isValid(
                validRules.copy(senderPatterns = listOf(longPattern)),
            ),
        )
    }

    @Test
    fun matches_requiresSenderAndContent() {
        val result = matcher.matches(validRules, sender = "HDFCBK", body = "Your OTP is 123456")
        assertEquals(RuleMatchOutcome.MATCHED, result.outcome)
    }

    @Test
    fun matches_rejectsWhenSenderDoesNotMatch() {
        val result = matcher.matches(validRules, sender = "OTHER", body = "Your OTP is 123456")
        assertEquals(RuleMatchOutcome.NO_MATCH, result.outcome)
    }

    @Test
    fun matches_rejectsWhenContentDoesNotMatch() {
        val result = matcher.matches(validRules, sender = "HDFCBK", body = "No code here")
        assertEquals(RuleMatchOutcome.NO_MATCH, result.outcome)
    }

    @Test
    fun matches_exclusionVetoesMatch() {
        val rules = validRules.copy(contentExclusions = listOf("blocked"))
        val result = matcher.matches(rules, sender = "HDFCBK", body = "123456 blocked")
        assertEquals(RuleMatchOutcome.EXCLUDED, result.outcome)
    }

    @Test
    fun matches_excludesTrustedRequesterAsSource() {
        val result = matcher.matches(
            rules = validRules,
            sender = "+919999999999",
            body = "123456",
            trustedRequesters = setOf("+919999999999"),
        )
        assertEquals(RuleMatchOutcome.EXCLUDED, result.outcome)
    }

    @Test
    fun matches_rejectsBodyLongerThan8192() {
        val body = "1".repeat(Re2RuleMatcher.MAX_BODY_LENGTH + 1)
        val result = matcher.matches(validRules, sender = "HDFCBK", body = body)
        assertEquals(RuleMatchOutcome.BODY_TOO_LONG, result.outcome)
    }

    @Test
    fun matches_invalidRulesFailClosed() {
        val invalidRules = validRules.copy(senderPatterns = listOf("("))
        val result = matcher.matches(invalidRules, sender = "HDFCBK", body = "123456")
        assertEquals(RuleMatchOutcome.INVALID_RULES, result.outcome)
    }
}
