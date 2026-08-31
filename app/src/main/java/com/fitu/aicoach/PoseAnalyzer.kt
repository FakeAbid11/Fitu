package com.fitu.aicoach

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Camera image analyzer that runs MediaPipe Pose Landmarker on each frame.
 *
 * Replaces the deprecated ML Kit pose detection with the actively maintained
 * MediaPipe Tasks solution:
 *  - pose_landmarker_full bundle (better accuracy than ML Kit accurate)
 *  - true 3D world landmarks (GHUM) exposed for camera-invariant angles
 *  - GPU delegate with automatic CPU fallback
 *  - LIVE_STREAM async mode for real-time analysis
 *
 * Frames are converted to upright pixel space before callbacks, so the
 * overlay receives rotation-corrected coordinates and only needs to handle
 * front-camera mirroring.
 */
class PoseAnalyzer(
    private val context: Context,
    private val overlay: PoseOverlay,
    private val onPoseDetected: (CoachPose, Float, ExerciseConfig) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "PoseAnalyzer"
        private const val MODEL_ASSET = "pose_landmarker_full.task"
    }

    // Current exercise configuration
    private var exerciseConfig: ExerciseConfig = ExerciseConfig.forExercise(ExerciseType.PUSH_UP)

    // Rep counter for rep-based exercises
    private var repCounter: RepCounter = RepCounter.forExercise(exerciseConfig)

    // Plank tracker for time-based exercises
    private var plankTracker: PlankTracker = PlankTracker.forExercise(exerciseConfig)

    // Front camera flag
    private var isFrontCamera: Boolean = true

    // Metadata of the frame currently being processed (captured before close())
    private var pendingFrameWidth: Int = 1
    private var pendingFrameHeight: Int = 1
    private var pendingRotationDegrees: Int = 0

    // Null when initialization failed on both delegates. The coach UI then
    // runs without pose detection instead of crashing the whole app.
    private var landmarker: PoseLandmarker? = null

    /** True when the landmarker was created successfully. */
    var isReady: Boolean = false
        private set

    /** Human-readable reason when [isReady] is false. */
    var initError: String? = null
        private set

    init {
        try {
            landmarker = createLandmarker(context, useGpu = true)
            isReady = true
        } catch (e: Exception) {
            Log.e(TAG, "GPU landmarker init failed, trying CPU", e)
            try {
                landmarker = createLandmarker(context, useGpu = false)
                isReady = true
            } catch (e2: Exception) {
                Log.e(TAG, "CPU landmarker init failed; pose detection unavailable", e2)
                initError = "Pose engine failed to start on this device"
            }
        }
    }

    private fun createLandmarker(context: Context, useGpu: Boolean): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(if (useGpu) Delegate.GPU else Delegate.CPU)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ ->
                onLandmarkerResult(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "MediaPipe pose landmarker error", error)
                overlay.clear()
            }
            .build()

        return PoseLandmarker.createFromOptions(context, options)
    }

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

    fun getRepCount(): Int = repCounter.repCount

    fun getHoldTimeMs(): Long = plankTracker.currentHoldTimeMs

    fun getBestHoldTimeMs(): Long = plankTracker.bestHoldTimeMs

    fun getFormScore(): Float = plankTracker.formScore

    /**
     * Analyze each camera frame for pose detection.
     */
    override fun analyze(imageProxy: ImageProxy) {
        val lm = landmarker
        if (lm == null) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmapSafe()
        if (bitmap == null) {
            imageProxy.close()
            return
        }

        // Capture frame metadata before closing the proxy
        pendingFrameWidth = imageProxy.width
        pendingFrameHeight = imageProxy.height
        pendingRotationDegrees = imageProxy.imageInfo.rotationDegrees

        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
        // CameraX timestamps are nanoseconds; MediaPipe expects milliseconds
        val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000

        // The bitmap is a copy, the proxy can be closed immediately
        imageProxy.close()

        try {
            lm.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "detectAsync failed", e)
        }
    }

    private fun ImageProxy.toBitmapSafe(): Bitmap? {
        return try {
            toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert frame to bitmap", e)
            null
        }
    }

    /**
     * Called by MediaPipe (LIVE_STREAM) when a result is ready.
     */
    private fun onLandmarkerResult(result: PoseLandmarkerResult) {
        val normalized = result.landmarks().firstOrNull()
        if (normalized == null || normalized.isEmpty()) {
            overlay.clear()
            return
        }

        val world = result.worldLandmarks().firstOrNull().orEmpty()

        // Build the pose in upright pixel space (rotation-corrected)
        val uprightWidth: Int
        val uprightHeight: Int
        if (pendingRotationDegrees == 90 || pendingRotationDegrees == 270) {
            uprightWidth = pendingFrameHeight
            uprightHeight = pendingFrameWidth
        } else {
            uprightWidth = pendingFrameWidth
            uprightHeight = pendingFrameHeight
        }

        val landmarks = normalized.map { lm ->
            val rotated = rotateNormalizedPoint(lm.x(), lm.y(), pendingRotationDegrees)
            CoachLandmark(
                x = rotated.first * uprightWidth,
                y = rotated.second * uprightHeight,
                z = lm.z(),
                visibility = lm.visibility().orElse(1f)
            )
        }
        val worldLandmarks = world.map { wl ->
            CoachLandmark(x = wl.x(), y = wl.y(), z = wl.z(), visibility = 1f)
        }

        val pose = CoachPose(
            landmarks = landmarks,
            width = uprightWidth,
            height = uprightHeight,
            worldLandmarks = worldLandmarks
        )

        processPose(pose)
    }

    /**
     * Rotate a normalized point so that it is expressed in the upright frame.
     */
    private fun rotateNormalizedPoint(nx: Float, ny: Float, rotationDegrees: Int): Pair<Float, Float> {
        return if (rotationDegrees == 90) {
            Pair(1f - ny, nx)
        } else if (rotationDegrees == 270) {
            Pair(ny, 1f - nx)
        } else if (rotationDegrees == 180) {
            Pair(1f - nx, 1f - ny)
        } else {
            Pair(nx, ny)
        }
    }

    /**
     * Process detected pose and update trackers + overlay.
     */
    private fun processPose(pose: CoachPose) {
        val pixelPose = pose.scaledToPixels()

        // Accuracy gate: some exercises are only valid with the body sideways
        // to the camera (push-up / plank). If the gate fails, pause tracking
        // and show guidance instead of counting phantom reps.
        val gateSegment = exerciseConfig.gateSegment
        if (gateSegment != null &&
            !AngleMath.isBodyHorizontal(
                pixelPose.landmark(gateSegment.first),
                pixelPose.landmark(gateSegment.second),
                exerciseConfig.gateToleranceDeg
            )
        ) {
            handleBodyPositionGate(pixelPose)
            return
        }

        val firstLandmark = pixelPose.landmark(exerciseConfig.landmarks.first)
        val midLandmark = pixelPose.landmark(exerciseConfig.landmarks.second)
        val lastLandmark = pixelPose.landmark(exerciseConfig.landmarks.third)

        // Calculate angle only if landmarks are reliable (50%+ confidence)
        val angle = if (AngleMath.areLandmarksReliable(firstLandmark, midLandmark, lastLandmark)) {
            AngleMath.calculateAngle(firstLandmark, midLandmark, lastLandmark)
        } else {
            -1f
        }

        // Update appropriate tracker
        val feedback: String
        var isRepEvent = false
        if (exerciseConfig.exerciseType.isTimeBased) {
            // Time-based exercise (Plank)
            plankTracker.update(angle, System.currentTimeMillis())
            feedback = plankTracker.getFormFeedback()
        } else {
            // Rep-based exercise
            val counted = repCounter.update(angle)
            isRepEvent = counted
            feedback = if (counted) {
                "${repCounter.repCount}! 🔥"
            } else {
                repCounter.getStateDisplay()
            }
        }

        // Skeleton color state: holding the plank maps to the working-position color
        val repState = if (exerciseConfig.exerciseType.isTimeBased) {
            if (plankTracker.isHolding) RepCounter.State.DOWN else RepCounter.State.UNKNOWN
        } else {
            repCounter.currentState
        }

        // Update overlay with pose (pixel space, upright frame)
        overlay.updatePose(
            pose = pixelPose,
            imageWidth = pixelPose.width,
            imageHeight = pixelPose.height,
            rotationDegrees = 0,
            isFrontCamera = isFrontCamera
        )

        // Update overlay with exercise info
        overlay.updateExerciseInfo(
            exerciseType = exerciseConfig.exerciseType,
            angle = angle,
            repCount = repCounter.repCount,
            holdTimeMs = plankTracker.currentHoldTimeMs,
            formScore = plankTracker.formScore,
            feedback = feedback,
            state = repState,
            isRepEvent = isRepEvent
        )

        // Callback with results
        onPoseDetected(pose, angle, exerciseConfig)
    }

    /**
     * Called when the body-position gate fails (e.g. the user is standing
     * during a push-up set). Breaks any plank hold / coasts the rep counter
     * and shows guidance instead of counting phantom reps.
     */
    private fun handleBodyPositionGate(pixelPose: CoachPose) {
        val now = System.currentTimeMillis()
        if (exerciseConfig.exerciseType.isTimeBased) {
            plankTracker.update(-1f, now) // break the hold
        } else {
            repCounter.update(-1f) // coast (prolonged failure resets the counter)
        }

        val hint = if (exerciseConfig.exerciseType.isTimeBased) {
            "Hold plank position - keep your body sideways to the camera"
        } else {
            "Get into position - keep your body sideways to the camera"
        }

        overlay.updatePose(
            pose = pixelPose,
            imageWidth = pixelPose.width,
            imageHeight = pixelPose.height,
            rotationDegrees = 0,
            isFrontCamera = isFrontCamera
        )
        overlay.updateExerciseInfo(
            exerciseType = exerciseConfig.exerciseType,
            angle = -1f,
            repCount = repCounter.repCount,
            holdTimeMs = plankTracker.currentHoldTimeMs,
            formScore = plankTracker.formScore,
            feedback = hint,
            state = RepCounter.State.UNKNOWN,
            isRepEvent = false
        )
        onPoseDetected(pixelPose, -1f, exerciseConfig)
    }

    /**
     * Release resources when done
     */
    fun close() {
        landmarker?.close()
        landmarker = null
        isReady = false
    }
}