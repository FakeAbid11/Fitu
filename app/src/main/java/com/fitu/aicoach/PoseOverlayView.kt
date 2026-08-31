package com.fitu.aicoach

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

/**
 * Custom View that draws pose skeleton overlay on top of camera preview.
 *
 * Handles:
 * - Coordinate mapping from pixel space to view space
 * - Front camera mirroring
 * - Skeleton drawing with joints and connections
 * - Exercise information display (feedback text)
 *
 * The pose arrives already rotation-corrected in upright pixel space
 * (see PoseAnalyzer), so no rotation handling is needed here.
 */
class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PoseOverlay {

    // Current pose data
    private var currentPose: CoachPose? = null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
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

    // Skeleton connections (pairs of landmark indices)
    private val skeletonConnections = listOf(
        // Face
        Pair(LandmarkIndex.LEFT_EAR, LandmarkIndex.LEFT_EYE),
        Pair(LandmarkIndex.LEFT_EYE, LandmarkIndex.NOSE),
        Pair(LandmarkIndex.NOSE, LandmarkIndex.RIGHT_EYE),
        Pair(LandmarkIndex.RIGHT_EYE, LandmarkIndex.RIGHT_EAR),

        // Upper body
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER),
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW),
        Pair(LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST),
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW),
        Pair(LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST),

        // Torso
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP),
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP),
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP),

        // Lower body
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE),
        Pair(LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE),
        Pair(LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE),
        Pair(LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE)
    )

    override fun updatePose(
        pose: CoachPose?,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ) {
        this.currentPose = pose
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
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
    private fun drawSkeleton(canvas: Canvas, pose: CoachPose) {
        // Get the config for current exercise to highlight relevant bones
        val config = ExerciseConfig.forExercise(currentExerciseType)
        val highlightedLandmarks = setOf(
            config.landmarks.first,
            config.landmarks.second,
            config.landmarks.third
        )

        // Draw bones (connections)
        for (connection in skeletonConnections) {
            val startLandmark = pose.landmark(connection.first)
            val endLandmark = pose.landmark(connection.second)

            if (startLandmark != null && endLandmark != null) {
                val startPoint = translatePoint(startLandmark.x, startLandmark.y)
                val endPoint = translatePoint(endLandmark.x, endLandmark.y)

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
        for (i in pose.landmarks.indices) {
            val landmark = pose.landmarks[i]
            if (0.5f < landmark.visibility) {
                val point = translatePoint(landmark.x, landmark.y)

                // Larger joint for tracked landmarks
                val isTracked = i in highlightedLandmarks
                val radius = if (isTracked) 16f else 10f

                canvas.drawCircle(point.x, point.y, radius, jointPaint)
            }
        }

        // Draw angle at the mid joint
        val midLandmark = pose.landmark(config.landmarks.second)
        if (midLandmark != null && 0 < currentAngle) {
            val point = translatePoint(midLandmark.x, midLandmark.y)
            canvas.drawText(
                "${currentAngle.toInt()}deg",
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
                height - 300f,
                feedbackPaint
            )
        }
    }

    /**
     * Translate a point from pixel coordinates to view coordinates.
     *
     * For front camera with FILL_CENTER PreviewView:
     * 1. The preview is mirrored horizontally by PreviewView
     * 2. We need to match that mirroring in our overlay
     * 3. Scale to fill the view while maintaining aspect ratio
     */
    private fun translatePoint(x: Float, y: Float): PointF {
        var mappedX = x
        var mappedY = y

        // For front camera: mirror horizontally to match PreviewView
        // PreviewView shows a mirrored image, so we need to flip X
        if (isFrontCamera) {
            mappedX = imageWidth - mappedX
        }

        // Calculate scale to fill the view (FILL_CENTER behavior)
        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)  // Use max for FILL (crop if needed)

        // Calculate the scaled dimensions
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        // Calculate offset to center the scaled image
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f

        // Apply scale and offset
        val finalX = mappedX * scale + offsetX
        val finalY = mappedY * scale + offsetY

        return PointF(finalX, finalY)
    }
}