package dev.laraib.khidki.domain.protocol

import dev.laraib.khidki.domain.model.CommandParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestParserTest {
    @Test
    fun parse_happyPath() {
        val result = RequestParser.parse("req 12345678")
        assertTrue(result is CommandParseResult.Success)
        assertEquals("12345678", (result as CommandParseResult.Success).password)
    }

    @Test
    fun parse_happyPath_caseInsensitiveKeyword() {
        val result = RequestParser.parse("REQ 12345678")
        assertTrue(result is CommandParseResult.Success)
        assertEquals("12345678", (result as CommandParseResult.Success).password)
    }

    @Test
    fun parse_happyPath_trimsWhitespace() {
        val result = RequestParser.parse("  req 12345678  ")
        assertTrue(result is CommandParseResult.Success)
    }

    @Test
    fun parse_happyPath_boundedSpaces() {
        val result = RequestParser.parse("req\t12345678")
        assertTrue(result is CommandParseResult.Success)
    }

    @Test
    fun parse_rejectTooLong() {
        val result = RequestParser.parse("req " + "1".repeat(62))
        assertEquals(CommandParseResult.Reject.TooLong, result)
    }

    @Test
    fun parse_rejectMultiline() {
        val result = RequestParser.parse("req 12345678\nextra")
        assertEquals(CommandParseResult.Reject.Multiline, result)
    }

    @Test
    fun parse_rejectCarriageReturn() {
        val result = RequestParser.parse("req 12345678\rextra")
        assertEquals(CommandParseResult.Reject.Multiline, result)
    }

    @Test
    fun parse_rejectNonAscii() {
        val result = RequestParser.parse("req 1234567\u00A8")
        assertEquals(CommandParseResult.Reject.NonAscii, result)
    }

    @Test
    fun parse_rejectWrongKeyword() {
        val result = RequestParser.parse("request 12345678")
        assertEquals(CommandParseResult.Reject.WrongKeyword, result)
    }

    @Test
    fun parse_rejectMissingToken() {
        val result = RequestParser.parse("req")
        assertEquals(CommandParseResult.Reject.MissingToken, result)
    }

    @Test
    fun parse_rejectExtraTokens() {
        val result = RequestParser.parse("req 12345678 extra")
        assertEquals(CommandParseResult.Reject.ExtraTokens, result)
    }

    @Test
    fun parse_rejectWrongLengthTooShort() {
        val result = RequestParser.parse("req 1234567")
        assertEquals(CommandParseResult.Reject.WrongLength, result)
    }

    @Test
    fun parse_rejectWrongLengthTooLong() {
        val result = RequestParser.parse("req 123456789")
        assertEquals(CommandParseResult.Reject.WrongLength, result)
    }

    @Test
    fun parse_rejectNonDigit() {
        val result = RequestParser.parse("req 1234abcd")
        assertEquals(CommandParseResult.Reject.NonDigit, result)
    }

    @Test
    fun parse_rejectUnicodeDigit() {
        val result = RequestParser.parse("req 123456\u0660")
        assertEquals(CommandParseResult.Reject.NonAscii, result)
    }

    @Test
    fun parse_rejectEmptyInput() {
        val result = RequestParser.parse("   ")
        assertEquals(CommandParseResult.Reject.WrongKeyword, result)
    }
}
