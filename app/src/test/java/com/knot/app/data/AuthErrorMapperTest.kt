package com.knot.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the errorCode -> friendly-message mapping directly with plain strings (the same
 * constants Firebase puts on FirebaseAuthException.errorCode), rather than constructing real
 * FirebaseAuthException subtypes -- see the class doc on [AuthErrorMapper] for why.
 */
class AuthErrorMapperTest {

    @Test
    fun `wrong password maps to a friendly, non-technical message`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_WRONG_PASSWORD")

        assertEquals(AuthErrorCode.WRONG_PASSWORD, code)
        assertEquals("Incorrect email or password.", message)
    }

    @Test
    fun `email already in use maps to a message that points at logging in instead`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_EMAIL_ALREADY_IN_USE")

        assertEquals(AuthErrorCode.EMAIL_ALREADY_IN_USE, code)
        assertEquals("That email is already registered. Try logging in instead.", message)
    }

    @Test
    fun `weak password maps to a message with the actual requirement`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_WEAK_PASSWORD")

        assertEquals(AuthErrorCode.WEAK_PASSWORD, code)
        assertEquals("Password is too weak. Use at least 6 characters, mixing letters and numbers.", message)
    }

    @Test
    fun `user not found maps to no-account message`() {
        val (code, _) = AuthErrorMapper.messageFor("ERROR_USER_NOT_FOUND")

        assertEquals(AuthErrorCode.USER_NOT_FOUND, code)
    }

    @Test
    fun `invalid email maps to a validation message`() {
        val (code, _) = AuthErrorMapper.messageFor("ERROR_INVALID_EMAIL")

        assertEquals(AuthErrorCode.INVALID_EMAIL, code)
    }

    @Test
    fun `unrecognized error code falls back to the SDK message when present`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_SOMETHING_NEW", fallbackMessage = "Some new SDK message")

        assertEquals(AuthErrorCode.UNKNOWN, code)
        assertEquals("Some new SDK message", message)
    }

    @Test
    fun `unrecognized error code with a blank fallback still returns a friendly generic message`() {
        val (code, message) = AuthErrorMapper.messageFor(errorCode = null, fallbackMessage = "  ")

        assertEquals(AuthErrorCode.UNKNOWN, code)
        assertEquals("Something went wrong. Please try again.", message)
    }

    @Test
    fun `map wraps the mapped message in an AuthException carrying the original cause`() {
        val cause = RuntimeException("raw SDK text nobody should see")

        val exception = AuthErrorMapper.map("ERROR_WEAK_PASSWORD", cause.message, cause)

        assertEquals(AuthErrorCode.WEAK_PASSWORD, exception.code)
        assertEquals("Password is too weak. Use at least 6 characters, mixing letters and numbers.", exception.message)
        assertEquals(cause, exception.cause)
    }
}
