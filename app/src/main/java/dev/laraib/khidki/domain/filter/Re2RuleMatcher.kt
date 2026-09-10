package dev.laraib.khidki.domain.filter

import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.RuleMatchOutcome
import dev.laraib.khidki.domain.model.RuleMatchResult
import dev.laraib.khidki.domain.ports.RuleMatcher

class Re2RuleMatcher : RuleMatcher {
    override fun isValid(rules: FilterRules): Boolean {
        if (rules.senderPatterns.none { it.isNotEmpty() }) {
            return false
        }
        if (rules.contentPatterns.none { it.isNotEmpty() }) {
            return false
        }
        return rules.allPatterns().all { pattern ->
            pattern.length <= MAX_PATTERN_LENGTH && compilePattern(pattern) != null
        }
    }

    override fun matches(
        rules: FilterRules,
        sender: String,
        body: String,
        trustedRequesters: Set<String>,
    ): RuleMatchResult {
        if (!isValid(rules)) {
            return RuleMatchResult(RuleMatchOutcome.INVALID_RULES)
        }
        if (body.length > MAX_BODY_LENGTH) {
            return RuleMatchResult(RuleMatchOutcome.BODY_TOO_LONG)
        }

        if (trustedRequesters.any { trusted ->
                sender == trusted || matchesPattern(trusted, sender)
            }) {
            return RuleMatchResult(RuleMatchOutcome.EXCLUDED)
        }

        for (pattern in rules.effectiveSenderExclusions()) {
            if (pattern.isNotEmpty() && matchesPattern(pattern, sender)) {
                return RuleMatchResult(RuleMatchOutcome.EXCLUDED)
            }
        }
        for (pattern in rules.effectiveContentExclusions()) {
            if (pattern.isNotEmpty() && matchesPattern(pattern, body)) {
                return RuleMatchResult(RuleMatchOutcome.EXCLUDED)
            }
        }

        val senderMatched = rules.senderPatterns.any { pattern ->
            pattern.isNotEmpty() && matchesPattern(pattern, sender)
        }
        val contentMatched = rules.contentPatterns.any { pattern ->
            pattern.isNotEmpty() && matchesPattern(pattern, body)
        }

        return if (senderMatched && contentMatched) {
            RuleMatchResult(RuleMatchOutcome.MATCHED)
        } else {
            RuleMatchResult(RuleMatchOutcome.NO_MATCH)
        }
    }

    private fun compilePattern(pattern: String): Pattern? =
        try {
            Pattern.compile(pattern)
        } catch (_: PatternSyntaxException) {
            null
        }

    private fun matchesPattern(pattern: String, value: String): Boolean {
        val compiled = compilePattern(pattern) ?: return false
        return compiled.matcher(value).find()
    }

    companion object {
        const val MAX_PATTERN_LENGTH: Int = 512
        const val MAX_BODY_LENGTH: Int = 8192
    }
}
