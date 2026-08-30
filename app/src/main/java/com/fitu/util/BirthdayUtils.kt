 package com.fitu.util

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Utility functions for birthday-related features.
 */
object BirthdayUtils {

    /**
     * Calculate age from birth date.
     * @return age in years, or null if birth date is not complete
     */
    fun calculateAge(birthDay: Int?, birthMonth: Int?, birthYear: Int?): Int? {
        if (birthDay == null || birthMonth == null || birthYear == null) {
            return null
        }
        
        return try {
            val birthDate = LocalDate.of(birthYear, birthMonth, birthDay)
            val today = LocalDate.now()
            Period.between(birthDate, today).years
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if today is the user's birthday.
     * Only returns true if BOTH day AND month match.
     */
    fun isBirthday(birthDay: Int?, birthMonth: Int?): Boolean {
        if (birthDay == null || birthMonth == null) {
            return false
        }

        val today = LocalDate.now()
        val todayDay = today.dayOfMonth
        val todayMonth = today.monthValue

        // Handle leap year birthday (Feb 29)
        if (birthMonth == 2 && birthDay == 29) {
            // If today is Feb 28 and it's not a leap year, celebrate
            if (todayMonth == 2 && todayDay == 28 && !today.isLeapYear) {
                return true
            }
            // If today is Feb 29 (leap year), celebrate
            return todayMonth == 2 && todayDay == 29
        }

        // ✅ FIX: Check BOTH month AND day match
        return todayMonth == birthMonth && todayDay == birthDay
    }

    /**
     * Check if birthday is within the grace period (for late app opens).
     * @param graceDays number of days after birthday to still show wish
     */
    fun isBirthdayWithinGracePeriod(birthDay: Int?, birthMonth: Int?, birthYear: Int?, graceDays: Int = 3): Boolean {
        if (birthDay == null || birthMonth == null || birthYear == null) {
            return false
        }

        // First check if today is the exact birthday
        if (isBirthday(birthDay, birthMonth)) {
            return true
        }

        // Check if birthday was within the last 'graceDays'
        return try {
            val today = LocalDate.now()
            
            // Get this year's birthday
            val thisYearBirthday = try {
                LocalDate.of(today.year, birthMonth, birthDay)
            } catch (e: Exception) {
                // Handle invalid dates like Feb 30
                LocalDate.of(today.year, birthMonth, 1).plusMonths(1).minusDays(1)
            }
            
            // ✅ FIX: Use ChronoUnit.DAYS for accurate day difference
            val daysSinceBirthday = ChronoUnit.DAYS.between(thisYearBirthday, today)
            
            // Only show if birthday was 0 to graceDays ago (not future, not too old)
            daysSinceBirthday in 0..graceDays
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if birth date is set.
     */
    fun isBirthDateSet(birthDay: Int?, birthMonth: Int?, birthYear: Int?): Boolean {
        return birthDay != null && birthMonth != null && birthYear != null
    }

    /**
     * Format birth date for display.
     * @return formatted date string like "January 15, 1998" or null if not set
     */
    fun formatBirthDate(birthDay: Int?, birthMonth: Int?, birthYear: Int?): String? {
        if (birthDay == null || birthMonth == null || birthYear == null) {
            return null
        }

        return try {
            val birthDate = LocalDate.of(birthYear, birthMonth, birthDay)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
            birthDate.format(formatter)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get zodiac sign based on birth date.
     */
    fun getZodiacSign(birthDay: Int?, birthMonth: Int?): String? {
        if (birthDay == null || birthMonth == null) return null

        return when (birthMonth) {
            1 -> if (birthDay <= 19) "Capricorn" else "Aquarius"
            2 -> if (birthDay <= 18) "Aquarius" else "Pisces"
            3 -> if (birthDay <= 20) "Pisces" else "Aries"
            4 -> if (birthDay <= 19) "Aries" else "Taurus"
            5 -> if (birthDay <= 20) "Taurus" else "Gemini"
            6 -> if (birthDay <= 20) "Gemini" else "Cancer"
            7 -> if (birthDay <= 22) "Cancer" else "Leo"
            8 -> if (birthDay <= 22) "Leo" else "Virgo"
            9 -> if (birthDay <= 22) "Virgo" else "Libra"
            10 -> if (birthDay <= 22) "Libra" else "Scorpio"
            11 -> if (birthDay <= 21) "Scorpio" else "Sagittarius"
            12 -> if (birthDay <= 21) "Sagittarius" else "Capricorn"
            else -> null
        }
    }
} 
