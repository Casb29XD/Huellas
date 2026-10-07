package com.desarrolloMovielexample.huella.core.utils

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

const val MIN_PASSWORD_LENGTH = 8

fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
