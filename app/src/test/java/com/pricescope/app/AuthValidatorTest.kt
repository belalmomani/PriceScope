package com.pricescope.app

import com.pricescope.app.auth.AuthValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun acceptsNormalEmails() {
        assertTrue(AuthValidator.isValidEmail("name@example.com"))
        assertTrue(AuthValidator.isValidEmail("  first.last+tag@sub.example.org "))
    }

    @Test
    fun rejectsBadEmails() {
        assertFalse(AuthValidator.isValidEmail(""))
        assertFalse(AuthValidator.isValidEmail("no-at-sign.com"))
        assertFalse(AuthValidator.isValidEmail("a@b"))
        assertFalse(AuthValidator.isValidEmail("a b@example.com"))
    }

    @Test
    fun passwordNeedsSixCharacters() {
        assertFalse(AuthValidator.isValidPassword("12345"))
        assertTrue(AuthValidator.isValidPassword("123456"))
    }
}
