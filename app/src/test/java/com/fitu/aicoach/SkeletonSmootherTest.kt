package com.fitu.aicoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the display-side skeleton smoothing.
 */
class SkeletonSmootherTest {

    private lateinit var smoother: SkeletonSmoother
    private val DELTA = 0.0001f

    @Before
    fun setup() {
        smoother = SkeletonSmoother(smoothingFactor = 0.4f)
    }

    private fun lm(x: Float, y: Float = 0.5f, vis: Float = 1f) =
        CoachLandmark(x = x, y = y, visibility = vis)

    @Test
    fun `first frame returns raw positions`() {
        val raw = listOf(lm(0.2f), lm(0.6f))
        val out = smoother.smooth(raw)
        assertEquals(0.2f, out[0].x, DELTA)
        assertEquals(0.6f, out[1].x, DELTA)
    }

    @Test
    fun `second frame moves only part of the way`() {
        smoother.smooth(listOf(lm(0.2f)))
        val out = smoother.smooth(listOf(lm(0.6f)))
        // lerp with alpha 0.4: 0.2 + 0.4 * 0.4 = 0.36
        assertEquals(0.36f, out[0].x, 0.001f)
    }

    @Test
    fun `converges to raw after many frames`() {
        smoother.smooth(listOf(lm(0.2f)))
        var out = listOf(lm(0.6f))
        repeat(60) { out = smoother.smooth(listOf(lm(0.6f))) }
        assertEquals(0.6f, out[0].x, 0.001f)
    }

    @Test
    fun `single-frame spike moves the skeleton only slightly`() {
        smoother.smooth(listOf(lm(0.5f)))
        val out = smoother.smooth(listOf(lm(0.9f)))
        // smoothed stays well under half of the spike distance
        assertTrue(out[0].x < 0.7f)
    }

    @Test
    fun `large jump snaps to raw immediately`() {
        smoother.smooth(listOf(lm(0.1f)))
        val out = smoother.smooth(listOf(lm(0.9f)))
        assertEquals(0.9f, out[0].x, DELTA)
    }

    @Test
    fun `visibility passes through unsmoothed`() {
        smoother.smooth(listOf(lm(0.2f, vis = 0.9f)))
        val out = smoother.smooth(listOf(lm(0.3f, vis = 0.4f)))
        assertEquals(0.4f, out[0].visibility, DELTA)
    }

    @Test
    fun `empty input resets the smoother`() {
        smoother.smooth(listOf(lm(0.2f)))
        smoother.smooth(emptyList())
        val out = smoother.smooth(listOf(lm(0.9f)))
        assertEquals(0.9f, out[0].x, DELTA)
    }

    @Test
    fun `z axis is smoothed the same way`() {
        smoother.smooth(listOf(CoachLandmark(x = 0.5f, y = 0.5f, z = 0f)))
        val out = smoother.smooth(listOf(CoachLandmark(x = 0.5f, y = 0.5f, z = -0.4f)))
        assertEquals(-0.16f, out[0].z, 0.001f)
    }
}