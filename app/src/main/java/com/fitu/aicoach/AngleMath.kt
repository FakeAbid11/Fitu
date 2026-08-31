package com.fitu.aicoach

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
     * Note: when using normalized coordinates, call this on a pose scaled to
     * pixel space ([CoachPose.scaledToPixels]) so the aspect ratio of the real
     * frame does not distort the angle.
     *
     * @return Angle in degrees (0-180), or -1 if any landmark is invalid
     */
    fun calculateAngle(
        first: CoachLandmark?,
        mid: CoachLandmark?,
        last: CoachLandmark?
    ): Float {
        if (first == null || mid == null || last == null) {
            return -1f
        }
        return calculateAngle(first.x, first.y, mid.x, mid.y, last.x, last.y)
    }

    /**
     * Calculate angle using raw coordinates instead of landmark objects.
     * Useful for testing or when you have pre-extracted coordinates.
     *
     * @return Angle in degrees (0-180)
     */
    fun calculateAngle(
        firstX: Float, firstY: Float,
        midX: Float, midY: Float,
        lastX: Float, lastY: Float
    ): Float {
        // Vector 1: mid to first
        val vector1X = firstX - midX
        val vector1Y = firstY - midY

        // Vector 2: mid to last
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

        // Convert to 0-180 range (we want the interior angle)
        if (180f < angleDegrees) {
            angleDegrees = 360f - angleDegrees
        }

        return angleDegrees
    }

    /**
     * Check if a landmark has sufficient confidence (visibility).
     *
     * @param landmark The pose landmark to check
     * @param minConfidence Minimum confidence threshold (0.5 = 50% - balanced for reliability)
     * @return True if landmark is reliable, false otherwise
     */
    fun isLandmarkReliable(landmark: CoachLandmark?, minConfidence: Float = 0.5f): Boolean {
        return landmark != null && !(landmark.visibility < minConfidence)
    }

    /**
     * Check if all three landmarks for an angle calculation are reliable.
     */
    fun areLandmarksReliable(
        first: CoachLandmark?,
        mid: CoachLandmark?,
        last: CoachLandmark?,
        minConfidence: Float = 0.5f
    ): Boolean {
        return isLandmarkReliable(first, minConfidence) &&
               isLandmarkReliable(mid, minConfidence) &&
               isLandmarkReliable(last, minConfidence)
    }

    /**
     * Check whether the body segment [first]->[second] is roughly horizontal
     * in the (upright) camera frame. Used as a position gate: push-ups and
     * planks are only valid when the body is sideways to the camera.
     *
     * 0 deg = perfectly horizontal segment, 90 deg = vertical segment.
     */
    fun isBodyHorizontal(
        first: CoachLandmark?,
        second: CoachLandmark?,
        toleranceDeg: Float = 60f
    ): Boolean {
        val p1 = first ?: return false
        val p2 = second ?: return false
        return isHorizontalDelta(p2.x - p1.x, p2.y - p1.y, toleranceDeg)
    }

    /**
     * Raw-coordinate variant of [isBodyHorizontal] (unit-test friendly).
     */
    fun isHorizontalDelta(dx: Float, dy: Float, toleranceDeg: Float = 60f): Boolean {
        if (dx == 0f && dy == 0f) return false
        val deg = Math.toDegrees(abs(atan2(dy.toDouble(), dx.toDouble())))
        return deg <= toleranceDeg.toDouble()
    }
}