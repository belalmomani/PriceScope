package com.pricescope.app.auth

object AuthValidator {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    // Supabase's default minimum password length is 6.
    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD

    const val MIN_PASSWORD = 6
}
