package dev.laraib.khidki.ui

import dev.laraib.khidki.domain.filter.Re2RuleMatcher
import dev.laraib.khidki.domain.model.FilterRules

object ConfigurationValidator {
    private const val MAX_CONFIGS = 20
    private val ruleMatcher = Re2RuleMatcher()

    fun validatePatterns(senderPattern: String, contentPattern: String): String? {
        if (senderPattern.isBlank() || contentPattern.isBlank()) {
            return "Sender and content filters are required"
        }
        val rules = FilterRules(
            senderPatterns = listOf(senderPattern),
            contentPatterns = listOf(contentPattern),
        )
        if (!ruleMatcher.isValid(rules)) {
            return "Invalid regex pattern"
        }
        return null
    }

    fun validateConfigCount(currentCount: Int): String? {
        if (currentCount >= MAX_CONFIGS) {
            return "Maximum $MAX_CONFIGS configurations allowed"
        }
        return null
    }
}
