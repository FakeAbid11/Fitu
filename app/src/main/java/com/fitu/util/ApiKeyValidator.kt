package com.fitu.util

/**
 * Format validation for Gemini API keys.
 *
 * Supports BOTH formats:
 *  - Classic Google AI Studio keys:  start with "AIza" (e.g. AIzaSy...)
 *  - Newer Google AI key formats:    keys issued by Google that do not
 *                                    start with "AIza"
 *
 * The rule is intentionally permissive (length + character set only);
 * real validity is verified when the first API request is made.
 */
object ApiKeyValidator {

    /** API keys are alphanumeric plus these separators. */
    private val KEY_PATTERN = Regex("^[A-Za-z0-9_.-]+$")

    private const val MIN_LENGTH = 20

    /**
     * Returns true if [key] looks like a valid Gemini API key
     * (classic "AIza..." or newer formats).
     */
    fun isValid(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.length < MIN_LENGTH) return false
        if (trimmed.any { it.isWhitespace() }) return false
        return KEY_PATTERN.matches(trimmed)
    }
}