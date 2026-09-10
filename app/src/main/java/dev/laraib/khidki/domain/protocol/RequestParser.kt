package dev.laraib.khidki.domain.protocol

import dev.laraib.khidki.domain.model.CommandParseResult

object RequestParser {
    private const val MAX_LENGTH = 64
    private const val PASSWORD_LENGTH = 8
    private val TOKEN_SPLIT = Regex("[ \\t]+")

    fun parse(input: String): CommandParseResult {
        val trimmed = input.trim()

        if (trimmed.length > MAX_LENGTH) {
            return CommandParseResult.Reject.TooLong
        }
        if (trimmed.contains('\n') || trimmed.contains('\r')) {
            return CommandParseResult.Reject.Multiline
        }
        if (!trimmed.all { it.code in 0..127 }) {
            return CommandParseResult.Reject.NonAscii
        }

        val tokens = trimmed.split(TOKEN_SPLIT).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return CommandParseResult.Reject.WrongKeyword
        }
        if (!tokens[0].equals("req", ignoreCase = true)) {
            return CommandParseResult.Reject.WrongKeyword
        }
        if (tokens.size == 1) {
            return CommandParseResult.Reject.MissingToken
        }
        if (tokens.size > 2) {
            return CommandParseResult.Reject.ExtraTokens
        }

        val passwordToken = tokens[1]
        for (character in passwordToken) {
            if (Character.isDigit(character) && character !in '0'..'9') {
                return CommandParseResult.Reject.UnicodeDigit
            }
            if (character !in '0'..'9') {
                return CommandParseResult.Reject.NonDigit
            }
        }
        if (passwordToken.length != PASSWORD_LENGTH) {
            return CommandParseResult.Reject.WrongLength
        }

        return CommandParseResult.Success(passwordToken)
    }
}
