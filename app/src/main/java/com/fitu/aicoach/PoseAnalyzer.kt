package com.fitu.aicoach

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

/**
 * Camera image analyzer that runs ML Kit Pose Detection on each frame.
 * 
 * Uses the ACCURATE pose detector for better accuracy at the cost of slightly
 * higher latency. STREAM_MODE is used for real-time analysis.
 * 
 * @param overlay The PoseOverlay implementation to update with pose data
 * @param onPoseDetected Callback with the detected pose and current exercise config
 */
class PoseAnalyzer(
    private val overlay: PoseOverlay,
    private val onPoseDetected: (Pose?, Float, ExerciseConfig) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
    }

    // Current exercise configuration
    private var exerciseConfig: ExerciseConfig = ExerciseConfig.forExercise(ExerciseType.PUSH_UP)

    // Pose detector with accurate model for better accuracy
    private val poseDetector: PoseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    // Rep counter for rep-based exercises
    private var repCounter: RepCounter = RepCounter.forExercise(exerciseConfig)

    // Plank tracker for time-based exercises
    private var plankTracker: PlankTracker = PlankTracker.forExercise(exerciseConfig)

    // Front camera flag
    private var isFrontCamera: Boolean = true

    /**
     * Set the current exercise to track
     */
    fun setExercise(type: ExerciseType, useLeftSide: Boolean = true) {
        exerciseConfig = ExerciseConfig.forExercise(type, useLeftSide)
        repCounter = RepCounter.forExercise(exerciseConfig)
        plankTracker = PlankTracker.forExercise(exerciseConfig)
    }

    /**
     * Set whether using front camera (for mirroring)
     */
    fun setFrontCamera(isFront: Boolean) {
        isFrontCamera = isFront
    }

    /**
     * Reset counters and trackers
     */
    fun reset() {
        repCounter.reset()
        plankTracker.reset()
    }

    /**
     * Get current rep count
     */
    fun getRepCount(): Int = repCounter.repCount

    /**
     * Get current hold time (for plank)
     */
    fun getHoldTimeMs(): Long = plankTracker.currentHoldTimeMs

    /**
     * Get best hold time (for plank)
     */
    fun getBestHoldTimeMs(): Long = plankTracker.bestHoldTimeMs

    /**
     * Get form score (for plank)
     */
    fun getFormScore(): Float = plankTracker.formScore

    /**
     * Analyze each camera frame for pose detection.
     * 
     * Processing pipeline:
     * 1. Create InputImage from camera frame
     * 2. Run ML Kit pose detection
     * 3. Calculate angle from relevant landmarks
     * 4. Update rep counter or plank tracker
     * 5. Update overlay with pose and exercise info
     */
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                processPose(pose, imageWidth, imageHeight, rotationDegrees)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
                overlay.clear()
            }
            .addOnCompleteListener {
                // Important: Close the image to allow next frame
                imageProxy.close()
            }
    }

    /**
     * Process detected pose and update trackers
     */
    private fun processPose(pose: Pose, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) {
        // Get landmarks for current exercise
        val firstLandmark = pose.getPoseLandmark(exerciseConfig.landmarks.first)
        val midLandmark = pose.getPoseLandmark(exerciseConfig.landmarks.second)
        val lastLandmark = pose.getPoseLandmark(exerciseConfig.landmarks.third)

        // Calculate angle only if landmarks are reliable (50%+ confidence)
        // This filters out noisy detections from camera shake
        val angle = if (AngleMath.areLandmarksReliable(firstLandmark, midLandmark, lastLandmark)) {
            AngleMath.calculateAngle(firstLandmark, midLandmark, lastLandmark)
        } else {
            -1f
        }

        // Update appropriate tracker
        val feedback: String
        if (exerciseConfig.exerciseType.isTimeBased) {
            // Time-based exercise (Plank)
            plankTracker.update(angle, System.currentTimeMillis())
            feedback = plankTracker.getFormFeedback()
        } else {
            // Rep-based exercise
            val counted = repCounter.update(angle)
            feedback = if (counted) {
                "${repCounter.repCount}! 🔥"
            } else {
                repCounter.getStateDisplay()
            }
        }

        // Update overlay with pose
        overlay.updatePose(
            pose = pose,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            isFrontCamera = isFrontCamera
        )

        // Update overlay with exercise info
        overlay.updateExerciseInfo(
            exerciseType = exerciseConfig.exerciseType,
            angle = angle,
            repCount = repCounter.repCount,
            holdTimeMs = plankTracker.currentHoldTimeMs,
            formScore = plankTracker.formScore,
            feedback = feedback
        )

        // Callback with results
        onPoseDetected(pose, angle, exerciseConfig)
    }

    /**
     * Release resources when done
     */
    fun close() {
        poseDetector.close()
    }
}
