package com.fitu.aicoach

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Custom View that draws pose skeleton overlay on top of camera preview.
 * 
 * Handles:
 * - Coordinate mapping from image space to view space
 * - Rotation handling (0°, 90°, 180°, 270°)
 * - Front camera mirroring
 * - Skeleton drawing with joints and connections
 * - Exercise information display (reps, timer, form score)
 */
class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PoseOverlay {

    // Current pose data
    private var currentPose: Pose? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var rotationDegrees: Int = 0
    private var isFrontCamera: Boolean = true

    // Exercise info
    private var currentExerciseType: ExerciseType = ExerciseType.PUSH_UP
    private var currentAngle: Float = 0f
    private var currentRepCount: Int = 0
    private var currentHoldTimeMs: Long = 0L
    private var currentFormScore: Float = 0f
    private var currentFeedback: String = ""

    // Paints
    private val jointPaint = Paint().apply {
        color = Color.parseColor("#FF6B00") // Orange
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bonePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val highlightBonePaint = Paint().apply {
        color = Color.parseColor("#FF6B00") // Orange for tracked limbs
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val angleTextPaint = Paint().apply {
        color = Color.parseColor("#FF6B00")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val feedbackPaint = Paint().apply {
        color = Color.GREEN
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(6f, 3f, 3f, Color.BLACK)
    }

    // Skeleton connections (pairs of landmark types)
    private val skeletonConnections = listOf(
        // Face
        Pair(PoseLandmark.LEFT_EAR, PoseLandmark.LEFT_EYE),
        Pair(PoseLandmark.LEFT_EYE, PoseLandmark.NOSE),
        Pair(PoseLandmark.NOSE, PoseLandmark.RIGHT_EYE),
        Pair(PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EAR),
        
        // Upper body
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
        Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
        Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
        Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
        
        // Torso
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
        Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
        Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
        
        // Lower body
        Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
        Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
        Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
        Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    )

    override fun updatePose(
        pose: Pose?,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ) {
        this.currentPose = pose
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.rotationDegrees = rotationDegrees
        this.isFrontCamera = isFrontCamera
        
        // Request redraw on UI thread
        postInvalidate()
    }

    override fun updateExerciseInfo(
        exerciseType: ExerciseType,
        angle: Float,
        repCount: Int,
        holdTimeMs: Long,
        formScore: Float,
        feedback: String
    ) {
        this.currentExerciseType = exerciseType
        this.currentAngle = angle
        this.currentRepCount = repCount
        this.currentHoldTimeMs = holdTimeMs
        this.currentFormScore = formScore
        this.currentFeedback = feedback
        
        postInvalidate()
    }

    override fun clear() {
        currentPose = null
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pose = currentPose ?: return

        // Draw skeleton
        drawSkeleton(canvas, pose)

        // Draw exercise info
        drawExerciseInfo(canvas)
    }

    /**
     * Draw the skeleton (joints and bones)
     */
    private fun drawSkeleton(canvas: Canvas, pose: Pose) {
        val landmarks = pose.allPoseLandmarks
        
        // Get the config for current exercise to highlight relevant bones
        val config = ExerciseConfig.forExercise(currentExerciseType)
        val highlightedLandmarks = setOf(
            config.landmarks.first,
            config.landmarks.second,
            config.landmarks.third
        )

        // Draw bones (connections)
        for (connection in skeletonConnections) {
            val startLandmark = pose.getPoseLandmark(connection.first)
            val endLandmark = pose.getPoseLandmark(connection.second)

            if (startLandmark != null && endLandmark != null) {
                val startPoint = translatePoint(startLandmark.position.x, startLandmark.position.y)
                val endPoint = translatePoint(endLandmark.position.x, endLandmark.position.y)

                // Use highlight paint for tracked limbs
                val isHighlighted = connection.first in highlightedLandmarks && 
                                   connection.second in highlightedLandmarks
                val paint = if (isHighlighted) highlightBonePaint else bonePaint

                canvas.drawLine(
                    startPoint.x, startPoint.y,
                    endPoint.x, endPoint.y,
                    paint
                )
            }
        }

        // Draw joints
        for (landmark in landmarks) {
            if (landmark.inFrameLikelihood > 0.5f) {
                val point = translatePoint(landmark.position.x, landmark.position.y)
                
                // Larger joint for tracked landmarks
                val isTracked = landmark.landmarkType in highlightedLandmarks
                val radius = if (isTracked) 16f else 10f
                
                canvas.drawCircle(point.x, point.y, radius, jointPaint)
            }
        }

        // Draw angle at the mid joint
        val midLandmark = pose.getPoseLandmark(config.landmarks.second)
        if (midLandmark != null && currentAngle > 0) {
            val point = translatePoint(midLandmark.position.x, midLandmark.position.y)
            canvas.drawText(
                "${currentAngle.toInt()}°",
                point.x,
                point.y - 30f,
                angleTextPaint
            )
        }
    }

    /**
     * Draw exercise information (reps, timer, feedback)
     */
    private fun drawExerciseInfo(canvas: Canvas) {
        val centerX = width / 2f
        
        // Draw exercise name at top
        canvas.drawText(
            "${currentExerciseType.emoji} ${currentExerciseType.displayName}",
            centerX,
            80f,
            textPaint
        )

        // Draw stats based on exercise type - handled by Composable StatsOverlay
        // We only draw the FEEDBACK (Up/Down/Good/Warn) here

        // Draw feedback higher up to avoid overlapping with the bottom stats card
        if (currentFeedback.isNotEmpty()) {
            canvas.drawText(
                currentFeedback,
                centerX,
                height - 300f, // Moved up significantly (was -50f)
                feedbackPaint
            )
        }
    }

    /**
     * Translate a point from image coordinates to view coordinates.
     * 
     * Key insight: ML Kit pose detection returns coordinates based on the 
     * ROTATED image dimensions. When rotation is 90° or 270°, the image
     * width and height are swapped in the coordinate system.
     * 
     * For front camera with FILL_CENTER PreviewView:
     * 1. The preview is mirrored horizontally by PreviewView
     * 2. We need to match that mirroring in our overlay
     * 3. Scale to fill the view while maintaining aspect ratio
     */
    private fun translatePoint(x: Float, y: Float): PointF {
        // After rotation, determine the effective dimensions
        // ML Kit returns coordinates in the rotated frame
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val effectiveWidth = if (isRotated) imageHeight.toFloat() else imageWidth.toFloat()
        val effectiveHeight = if (isRotated) imageWidth.toFloat() else imageHeight.toFloat()
        
        // Map coordinates based on rotation
        // ML Kit handles rotation internally via InputImage.fromMediaImage, so the
        // coordinates returned in the Pose object are ALREADY relative to the upright/rotated image.
        // We do NOT need to manually swap X/Y axes here.
        var mappedX = x
        var mappedY = y
        
        // For front camera: mirror horizontally to match PreviewView
        // PreviewView shows a mirrored image, so we need to flip X
        if (isFrontCamera) {
            mappedX = effectiveWidth - mappedX
        }
        
        // Calculate scale to fill the view (FILL_CENTER behavior)
        val scaleX = width.toFloat() / effectiveWidth
        val scaleY = height.toFloat() / effectiveHeight
        val scale = maxOf(scaleX, scaleY)  // Use max for FILL (crop if needed)
        
        // Calculate the scaled dimensions
        val scaledWidth = effectiveWidth * scale
        val scaledHeight = effectiveHeight * scale
        
        // Calculate offset to center the scaled image
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f
        
        // Apply scale and offset
        val finalX = mappedX * scale + offsetX
        val finalY = mappedY * scale + offsetY
        
        return PointF(finalX, finalY)
    }

    /**
     * Format milliseconds to MM:SS string
     */
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
