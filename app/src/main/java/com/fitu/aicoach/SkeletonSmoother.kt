package com.fitu.aicoach

/**
 * Exponential smoothing for drawn skeleton landmarks.
 *
 * Display-only: the counting logic (RepCounter/PlankTracker) works on raw
 * angles and has its own median smoothing. This class only removes the
 * frame-to-frame jitter that makes the skeleton shiver on screen.
 *
 *  - Each frame moves each landmark a fraction [smoothingFactor] of the way
 *    toward the raw detection (0 = frozen, 1 = raw/no smoothing).
 *  - A sudden jump larger than the snap threshold (user re-entered the
 *    frame, exercise switch) snaps instantly instead of leaving a trail.
 *  - Visibility (detection confidence) is passed through unsmoothed.
 */
class SkeletonSmoother(private val smoothingFactor: Float = 0.4f) {

    companion object {
        // Normalized-distance squared that triggers an instant snap (0.3 of the frame)
        private const val SNAP_DISTANCE_SQUARED = 0.09f
    }

    private val smoothed = mutableListOf<CoachLandmark>()

    /**
     * Smooth one frame of landmarks.
     * The first frame (or a landmark-count change) initializes directly.
     */
    fun smooth(landmarks: List<CoachLandmark>): List<CoachLandmark> {
        if (landmarks.isEmpty()) {
            reset()
            return landmarks
        }
        if (smoothed.size != landmarks.size) {
            smoothed.clear()
            smoothed.addAll(landmarks)
            return smoothed.toList()
        }

        val result = mutableListOf<CoachLandmark>()
        for (i in landmarks.indices) {
            val raw = landmarks[i]
            val prev = smoothed[i]
            val dx = raw.x - prev.x
            val dy = raw.y - prev.y
            val jumped = dx * dx + dy * dy
            val next = if (SNAP_DISTANCE_SQUARED < jumped) {
                raw
            } else {
                CoachLandmark(
                    x = prev.x + dx * smoothingFactor,
                    y = prev.y + dy * smoothingFactor,
                    z = prev.z + (raw.z - prev.z) * smoothingFactor,
                    visibility = raw.visibility
                )
            }
            smoothed[i] = next
            result.add(next)
        }
        return result
    }

    /** Forget all smoothed state (e.g. when the overlay is cleared). */
    fun reset() = smoothed.clear()
}