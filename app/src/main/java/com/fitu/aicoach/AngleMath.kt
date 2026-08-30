package com.fitu.aicoach

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Utility object for angle calculations used in pose detection.
 * 
 * All angles are calculated using the atan2 function which provides
 * accurate angle measurements in any quadrant.
 */
object AngleMath {

    /**
     * Calculate the angle at the middle point (vertex) formed by three landmarks.
     * 
     * Mathematical approach:
     * 1. Calculate vectors from mid point to first and last points
     * 2. Use atan2 to get the angle of each vector relative to horizontal
     * 3. Subtract the angles to get the angle between vectors
     * 4. Convert from radians to degrees
     * 5. Normalize to 0-180° range
     * 
     * Example: For elbow angle with Shoulder → Elbow → Wrist
     * - first = Shoulder position
     * - mid = Elbow position (vertex)
     * - last = Wrist position
     * - Returns the angle at the elbow joint
     * 
     * @param first First landmark (starting point of first ray)
     * @param mid Middle landmark (vertex where angle is measured)
     * @param last Last landmark (starting point of second ray)
     * @return Angle in degrees (0-180), or -1 if any landmark is invalid
     */
    fun calculateAngle(
        first: PoseLandmark?,
        mid: PoseLandmark?,
        last: PoseLandmark?
    ): Float {
        if (first == null || mid == null || last == null) {
            return -1f
        }

        // Get positions
        val firstPos = first.position
        val midPos = mid.position
        val lastPos = last.position

        // Calculate vectors from mid point
        // Vector 1: mid → first
        val vector1X = firstPos.x - midPos.x
        val vector1Y = firstPos.y - midPos.y

        // Vector 2: mid → last
        val vector2X = lastPos.x - midPos.x
        val vector2Y = lastPos.y - midPos.y

        // Calculate angles using atan2
        // atan2(y, x) returns angle in radians from -π to π
        val angle1 = atan2(vector1Y, vector1X)
        val angle2 = atan2(vector2Y, vector2X)

        // Difference between angles
        var angleDiff = angle1 - angle2

        // Convert to degrees
        var angleDegrees = Math.toDegrees(angleDiff.toDouble()).toFloat()

        // Normalize to 0-360 range first
        if (angleDegrees < 0) {
            angleDegrees += 360f
        }

        // Convert to 0-180 range (we want the interior angle)
        if (angleDegrees > 180f) {
            angleDegrees = 360f - angleDegrees
        }

        return angleDegrees
    }

    /**
     * Calculate angle using raw coordinates instead of PoseLandmark objects.
     * Useful for testing or when you have pre-extracted coordinates.
     * 
     * @param firstX X coordinate of first point
     * @param firstY Y coordinate of first point
     * @param midX X coordinate of middle point (vertex)
     * @param midY Y coordinate of middle point (vertex)
     * @param lastX X coordinate of last point
     * @param lastY Y coordinate of last point
     * @return Angle in degrees (0-180)
     */
    fun calculateAngle(
        firstX: Float, firstY: Float,
        midX: Float, midY: Float,
        lastX: Float, lastY: Float
    ): Float {
        // Vector 1: mid → first
        val vector1X = firstX - midX
        val vector1Y = firstY - midY

        // Vector 2: mid → last
        val vector2X = lastX - midX
        val vector2Y = lastY - midY

        // Calculate angles using atan2
        val angle1 = atan2(vector1Y, vector1X)
        val angle2 = atan2(vector2Y, vector2X)

        // Difference between angles
        val angleDiff = angle1 - angle2

        // Convert to degrees
        var angleDegrees = Math.toDegrees(angleDiff.toDouble()).toFloat()

        // Normalize to 0-360 range first
        if (angleDegrees < 0) {
            angleDegrees += 360f
        }

        // Convert to 0-180 range
        if (angleDegrees > 180f) {
            angleDegrees = 360f - angleDegrees
        }

        return angleDegrees
    }

    /**
     * Check if a landmark has sufficient confidence (in-frame likelihood).
     * 
     * @param landmark The pose landmark to check
     * @param minConfidence Minimum confidence threshold (0.5 = 50% - balanced for reliability)
     * @return True if landmark is reliable, false otherwise
     */
    fun isLandmarkReliable(landmark: PoseLandmark?, minConfidence: Float = 0.5f): Boolean {
        return landmark != null && landmark.inFrameLikelihood >= minConfidence
    }

    /**
     * Check if all three landmarks for an angle calculation are reliable.
     */
    fun areLandmarksReliable(
        first: PoseLandmark?,
        mid: PoseLandmark?,
        last: PoseLandmark?,
        minConfidence: Float = 0.5f
    ): Boolean {
        return isLandmarkReliable(first, minConfidence) &&
               isLandmarkReliable(mid, minConfidence) &&
               isLandmarkReliable(last, minConfidence)
    }
}
