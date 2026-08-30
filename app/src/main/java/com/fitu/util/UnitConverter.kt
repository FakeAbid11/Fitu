package com.fitu.util

/**
 * ✅ FIX #24: Unit conversion utility for metric/imperial support
 */
object UnitConverter {

    // --- Length Conversions ---

    /**
     * Convert centimeters to feet and inches
     * @return Pair of (feet, inches)
     */
    fun cmToFeetInches(cm: Int): Pair<Int, Int> {
        val totalInches = cm / 2.54
        val feet = (totalInches / 12).toInt()
        val inches = (totalInches % 12).toInt()
        return Pair(feet, inches)
    }

    /**
     * Convert feet and inches to centimeters
     */
    fun feetInchesToCm(feet: Int, inches: Int): Int {
        val totalInches = (feet * 12) + inches
        return (totalInches * 2.54).toInt()
    }

    /**
     * Format height for display
     */
    fun formatHeight(cm: Int, useImperial: Boolean): String {
        return if (useImperial) {
            val (feet, inches) = cmToFeetInches(cm)
            "$feet'$inches\""
        } else {
            "$cm cm"
        }
    }

    // --- Weight Conversions ---

    /**
     * Convert kilograms to pounds
     */
    fun kgToLbs(kg: Int): Double {
        return kg * 2.20462
    }

    /**
     * Convert pounds to kilograms
     */
    fun lbsToKg(lbs: Double): Int {
        return (lbs / 2.20462).toInt()
    }

    /**
     * Format weight for display
     */
    fun formatWeight(kg: Int, useImperial: Boolean): String {
        return if (useImperial) {
            val lbs = kgToLbs(kg)
            "${String.format("%.1f", lbs)} lbs"
        } else {
            "$kg kg"
        }
    }

    // --- Distance Conversions ---

    /**
     * Convert kilometers to miles
     */
    fun kmToMiles(km: Float): Float {
        return km * 0.621371f
    }

    /**
     * Convert miles to kilometers
     */
    fun milesToKm(miles: Float): Float {
        return miles / 0.621371f
    }

    /**
     * Format distance for display
     */
    fun formatDistance(km: Float, useImperial: Boolean): String {
        return if (useImperial) {
            val miles = kmToMiles(km)
            "${String.format("%.2f", miles)} mi"
        } else {
            "${String.format("%.2f", km)} km"
        }
    }

    /**
     * Get distance unit label
     */
    fun getDistanceUnit(useImperial: Boolean): String {
        return if (useImperial) "MI" else "KM"
    }

    /**
     * Get weight unit label
     */
    fun getWeightUnit(useImperial: Boolean): String {
        return if (useImperial) "LBS" else "KG"
    }

    /**
     * Get height unit label
     */
    fun getHeightUnit(useImperial: Boolean): String {
        return if (useImperial) "FT/IN" else "CM"
    }
} 
