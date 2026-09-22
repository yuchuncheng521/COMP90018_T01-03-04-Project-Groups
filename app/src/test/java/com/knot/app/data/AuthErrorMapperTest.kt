package com.knot.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {

    @Test
    fun `wrong password maps to friendly message`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_WRONG_PASSWORD")
        assertEquals(AuthErrorCode.WRONG_PASSWORD, code)
        assertEquals("Incorrect email or password. Please type it again!", message)
    }

    @Test
    fun `invalid credential also maps to wrong password`() {
        val (code, _) = AuthErrorMapper.messageFor("ERROR_INVALID_CREDENTIAL")
        assertEquals(AuthErrorCode.WRONG_PASSWORD, code)
    }

    @Test
    fun `email already in use maps correctly`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_EMAIL_ALREADY_IN_USE")
        assertEquals(AuthErrorCode.EMAIL_ALREADY_IN_USE, code)
        assertEquals("That email is already registered. Try logging in instead.", message)
    }

    @Test
    fun `weak password maps correctly`() {
        val (code, _) = AuthErrorMapper.messageFor("ERROR_WEAK_PASSWORD")
        assertEquals(AuthErrorCode.WEAK_PASSWORD, code)
    }

    @Test
    fun `network error maps correctly`() {
        val (code, _) = AuthErrorMapper.messageFor("ERROR_NETWORK_REQUEST_FAILED")
        assertEquals(AuthErrorCode.NETWORK_ERROR, code)
    }

    @Test
    fun `unknown error code falls back to fallback message`() {
        val (code, message) = AuthErrorMapper.messageFor("ERROR_SOMETHING_WEIRD", "Original SDK message")
        assertEquals(AuthErrorCode.UNKNOWN, code)
        assertEquals("Original SDK message", message)
    }

    @Test
    fun `unknown error code with no fallback uses generic message`() {
        val (code, message) = AuthErrorMapper.messageFor(null, null)
        assertEquals(AuthErrorCode.UNKNOWN, code)
        assertEquals("Something went wrong. Please try again.", message)
    }
}