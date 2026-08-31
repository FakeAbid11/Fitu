package com.fitu.aicoach

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom View that draws pose skeleton overlay on top of camera preview.
 *
 * Rendering features:
 *  - Exponential smoothing of joint positions (no shivering; see SkeletonSmoother)
 *  - Visibility-aware: joints AND bones are skipped for weak detections
 *  - Complete skeleton including feet and hands, subtle left/right tint
 *  - Density-scaled strokes, radii and text (dp, not raw px)
 *  - State-colored skeleton: orange (extended), green (working position),
 *    red (bad position / no reliable angle), white (neutral)
 *  - Pulse animation on each counted rep
 *  - Angle drawn on a readable translucent badge
 *  - Joint size subtly modulated by landmark depth (z)
 *
 * The pose arrives already rotation-corrected in upright pixel space
 * (see PoseAnalyzer), so only front-camera mirroring is handled here.
 */
class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PoseOverlay {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float): Float = value * density

    // State colors
    private val colorNeutral = Color.WHITE
    private val colorWorking = Color.parseColor("#4CAF50")   // green: in the working position
    private val colorExtended = Color.parseColor("#FF6B00")  // orange: extended / ready
    private val colorInvalid = Color.parseColor("#FF5252")   // red: bad position / no angle

    // Per-side bone tints
    private val colorLeftBones = Color.parseColor("#BFD7FF")
    private val colorRightBones = Color.parseColor("#FFD9B3")

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
    private var currentBodyColor: Int = colorNeutral

    // Rep pulse animation
    private var pulseProgress = 0f
    private var pulseAnimator: ValueAnimator? = null

    // Display-side smoothing (counting logic uses its own median smoothing)
    private val smoother = SkeletonSmoother()

    // Paints (density-scaled)
    private val jointPaint = Paint().apply {
        color = colorExtended
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bonePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        isAntiAlias = true
    }

    private val highlightBonePaint = Paint().apply {
        color = colorExtended
        style = Paint.Style.STROKE
        strokeWidth = dp(4.5f)
        isAntiAlias = true
    }

    private val leftBonePaint = Paint().apply {
        color = colorLeftBones
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        isAntiAlias = true
    }

    private val rightBonePaint = Paint().apply {
        color = colorRightBones
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = dp(16f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(dp(2f), dp(1f), dp(1f), Color.BLACK)
    }

    private val angleTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = dp(13f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val angleBadgePaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val feedbackPaint = Paint().apply {
        color = Color.GREEN
        textSize = dp(22f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        setShadowLayer(dp(3f), dp(1.5f), dp(1.5f), Color.BLACK)
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
        Pair(LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE),

        // Feet (previously missing)
        Pair(LandmarkIndex.LEFT_ANKLE, LandmarkIndex.LEFT_HEEL),
        Pair(LandmarkIndex.LEFT_HEEL, LandmarkIndex.LEFT_FOOT_INDEX),
        Pair(LandmarkIndex.RIGHT_ANKLE, LandmarkIndex.RIGHT_HEEL),
        Pair(LandmarkIndex.RIGHT_HEEL, LandmarkIndex.RIGHT_FOOT_INDEX),

        // Hands (previously missing)
        Pair(LandmarkIndex.LEFT_WRIST, LandmarkIndex.LEFT_PINKY),
        Pair(LandmarkIndex.LEFT_WRIST, LandmarkIndex.LEFT_INDEX),
        Pair(LandmarkIndex.RIGHT_WRIST, LandmarkIndex.RIGHT_PINKY),
        Pair(LandmarkIndex.RIGHT_WRIST, LandmarkIndex.RIGHT_INDEX)
    )

    private val leftSideIndices = setOf(
        LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST,
        LandmarkIndex.LEFT_PINKY, LandmarkIndex.LEFT_INDEX, LandmarkIndex.LEFT_THUMB,
        LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE,
        LandmarkIndex.LEFT_HEEL, LandmarkIndex.LEFT_FOOT_INDEX
    )

    private val rightSideIndices = setOf(
        LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST,
        LandmarkIndex.RIGHT_PINKY, LandmarkIndex.RIGHT_INDEX, LandmarkIndex.RIGHT_THUMB,
        LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE,
        LandmarkIndex.RIGHT_HEEL, LandmarkIndex.RIGHT_FOOT_INDEX
    )

    override fun updatePose(
        pose: CoachPose?,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFrontCamera = isFrontCamera
        this.currentPose = if (pose == null) null else {
            pose.copy(landmarks = smoother.smooth(pose.landmarks))
        }
        postInvalidate()
    }

    override fun updateExerciseInfo(
        exerciseType: ExerciseType,
        angle: Float,
        repCount: Int,
        holdTimeMs: Long,
        formScore: Float,
        feedback: String,
        state: RepCounter.State,
        isRepEvent: Boolean
    ) {
        this.currentExerciseType = exerciseType
        this.currentAngle = angle
        this.currentRepCount = repCount
        this.currentHoldTimeMs = holdTimeMs
        this.currentFormScore = formScore
        this.currentFeedback = feedback

        this.currentBodyColor = if (angle < 0f) {
            colorInvalid
        } else if (state == RepCounter.State.DOWN) {
            colorWorking
        } else if (state == RepCounter.State.UP) {
            colorExtended
        } else {
            colorNeutral
        }

        if (isRepEvent) startPulse()
        postInvalidate()
    }

    override fun clear() {
        currentPose = null
        smoother.reset()
        postInvalidate()
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = 450
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                pulseProgress = animation.animatedValue as Float
                postInvalidate()
            }
        })
        animator.start()
        pulseAnimator = animator
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pose = currentPose ?: return

        // Draw skeleton
        drawSkeleton(canvas, pose)

        // Draw exercise info
        drawExerciseInfo(canvas)
    }

    private fun drawSkeleton(canvas: Canvas, pose: CoachPose) {
        val config = ExerciseConfig.forExercise(currentExerciseType)
        val highlighted = setOf(
            config.landmarks.first,
            config.landmarks.second,
            config.landmarks.third
        )

        jointPaint.color = currentBodyColor
        highlightBonePaint.color = currentBodyColor

        // Bones (visibility-aware: skip weak detections)
        for (connection in skeletonConnections) {
            val start = pose.landmark(connection.first)
            val end = pose.landmark(connection.second)
            if (start == null || end == null) continue
            if (start.visibility < 0.5f || end.visibility < 0.5f) continue

            val startPoint = translatePoint(start.x, start.y)
            val endPoint = translatePoint(end.x, end.y)

            val paint = if (connection.first in highlighted && connection.second in highlighted) {
                highlightBonePaint
            } else if (connection.first in leftSideIndices) {
                leftBonePaint
            } else if (connection.first in rightSideIndices) {
                rightBonePaint
            } else {
                bonePaint
            }

            canvas.drawLine(
                startPoint.x, startPoint.y,
                endPoint.x, endPoint.y,
                paint
            )
        }

        // Joints (depth-modulated radius)
        for (i in pose.landmarks.indices) {
            val landmark = pose.landmarks[i]
            if (landmark.visibility < 0.5f) continue
            val point = translatePoint(landmark.x, landmark.y)
            val base = if (i in highlighted) dp(6f) else dp(4f)
            val depthFactor = (1f - landmark.z * 0.15f).coerceIn(0.75f, 1.25f)
            canvas.drawCircle(point.x, point.y, base * depthFactor, jointPaint)
        }

        // Angle badge at the tracked joint
        val midLandmark = pose.landmark(config.landmarks.second)
        if (midLandmark != null && 0 < currentAngle) {
            drawAngleBadge(canvas, midLandmark)
        }
    }

    private fun drawAngleBadge(canvas: Canvas, mid: CoachLandmark) {
        val text = "${currentAngle.toInt()}deg"
        val point = translatePoint(mid.x, mid.y)
        val textWidth = angleTextPaint.measureText(text)
        val baseline = point.y - dp(18f)
        val pad = dp(6f)
        val badge = RectF(
            point.x - textWidth / 2f - pad,
            baseline + angleTextPaint.fontMetrics.ascent - pad,
            point.x + textWidth / 2f + pad,
            baseline + angleTextPaint.fontMetrics.descent + pad
        )
        canvas.drawRoundRect(badge, dp(8f), dp(8f), angleBadgePaint)
        canvas.drawText(text, point.x, baseline, angleTextPaint)
    }

    private fun drawExerciseInfo(canvas: Canvas) {
        val centerX = width / 2f

        canvas.drawText(
            "${currentExerciseType.emoji} ${currentExerciseType.displayName}",
            centerX,
            dp(28f),
            textPaint
        )

        // Feedback with a brief scale/brighten pulse on each counted rep
        if (currentFeedback.isNotEmpty()) {
            feedbackPaint.textSize = dp(22f) * (1f + 0.35f * pulseProgress)
            feedbackPaint.alpha = (200f + 55f * pulseProgress).toInt().coerceIn(0, 255)
            canvas.drawText(
                currentFeedback,
                centerX,
                height - dp(110f),
                feedbackPaint
            )
        }
    }

    private fun translatePoint(x: Float, y: Float): PointF {
        var mappedX = x
        var mappedY = y

        if (isFrontCamera) {
            mappedX = imageWidth - mappedX
        }

        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)  // FILL (crop if needed)

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f

        return PointF(mappedX * scale + offsetX, mappedY * scale + offsetY)
    }
}