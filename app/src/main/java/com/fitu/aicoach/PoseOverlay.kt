package com.fitu.aicoach

import com.google.mlkit.vision.pose.Pose

/**
 * Interface for pose overlay views that display skeleton visualization.
 * 
 * This interface decouples the pose analysis from the rendering,
 * allowing different overlay implementations (View-based, Compose-based, etc.)
 */
interface PoseOverlay {
    
    /**
     * Update the overlay with new pose data.
     * 
     * @param pose The detected pose from ML Kit, or null if no pose detected
     * @param imageWidth Width of the source image in pixels
     * @param imageHeight Height of the source image in pixels
     * @param rotationDegrees Rotation of the image (0, 90, 180, or 270)
     * @param isFrontCamera True if using front camera (requires horizontal flip)
     */
    fun updatePose(
        pose: Pose?,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    )

    /**
     * Update the exercise info to display on the overlay
     * 
     * @param exerciseType Current exercise being tracked
     * @param angle Current angle being measured (degrees)
     * @param repCount Number of reps completed (for rep-based exercises)
     * @param holdTimeMs Current hold time in milliseconds (for time-based exercises)
     * @param formScore Form score 0-10 (for time-based exercises)
     * @param feedback Feedback text to display
     */
    fun updateExerciseInfo(
        exerciseType: ExerciseType,
        angle: Float,
        repCount: Int,
        holdTimeMs: Long,
        formScore: Float,
        feedback: String
    )

    /**
     * Clear the overlay (no pose to display)
     */
    fun clear()
}
