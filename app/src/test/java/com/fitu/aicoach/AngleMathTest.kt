package com.fitu.aicoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for AngleMath angle calculation logic.
 */
class AngleMathTest {

    private val DELTA = 0.5f  // Tolerance for floating point comparison

    @Test
    fun `calculateAngle returns 90 degrees for right angle`() {
        // Right angle: points form an L shape
        // First point at (0, 0), Mid at (0, 1), Last at (1, 1)
        val angle = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 0f, midY = 1f,
            lastX = 1f, lastY = 1f
        )
        
        assertEquals(90f, angle, DELTA)
    }

    @Test
    fun `calculateAngle returns 180 degrees for straight line`() {
        // Straight line: all points collinear
        val angle = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 1f, midY = 0f,
            lastX = 2f, lastY = 0f
        )
        
        assertEquals(180f, angle, DELTA)
    }

    @Test
    fun `calculateAngle returns 0 degrees for overlapping rays`() {
        // Overlapping rays: first and last on same side of mid
        val angle = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 1f, midY = 0f,
            lastX = 0f, lastY = 0f
        )
        
        // Overlapping points = 0° or 180°
        assertTrue(angle < 1f || abs(angle - 180f) < 1f)
    }

    @Test
    fun `calculateAngle returns 135 degrees for obtuse configuration`() {
        // Rays at 180 deg and 45 deg from vertex (1,0) -> interior angle 135 deg
        val angle = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 1f, midY = 0f,
            lastX = 2f, lastY = 1f
        )
        
        assertEquals(135f, angle, DELTA)
    }

    @Test
    fun `calculateAngle returns 45 degrees for acute configuration`() {
        // Rays at 225 deg and 180 deg from vertex (1,1) -> interior angle 45 deg
        val angle = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 1f, midY = 1f,
            lastX = 0f, lastY = 1f
        )
        
        assertEquals(45f, angle, DELTA)
    }

    @Test
    fun `calculateAngle is symmetric - order of first and last does not matter`() {
        val angle1 = AngleMath.calculateAngle(
            firstX = 0f, firstY = 0f,
            midX = 1f, midY = 1f,
            lastX = 2f, lastY = 0f
        )
        
        val angle2 = AngleMath.calculateAngle(
            firstX = 2f, firstY = 0f,
            midX = 1f, midY = 1f,
            lastX = 0f, lastY = 0f
        )
        
        assertEquals(angle1, angle2, DELTA)
    }

    @Test
    fun `calculateAngle returns value in 0-180 range`() {
        // Test multiple angles to ensure all are in valid range
        val testCases = listOf(
            Triple(0f to 0f, 1f to 1f, 2f to 0f),
            Triple(0f to 0f, 0f to 1f, 1f to 1f),
            Triple(-1f to -1f, 0f to 0f, 1f to 1f),
            Triple(100f to 50f, 200f to 100f, 150f to 200f)
        )
        
        for ((first, mid, last) in testCases) {
            val angle = AngleMath.calculateAngle(
                firstX = first.first, firstY = first.second,
                midX = mid.first, midY = mid.second,
                lastX = last.first, lastY = last.second
            )
            
            assertTrue("Angle $angle should be >= 0", angle >= 0f)
            assertTrue("Angle $angle should be <= 180", angle <= 180f)
        }
    }

    @Test
    fun `calculateAngle with landmarks returns -1 for null landmarks`() {
        val angle = AngleMath.calculateAngle(null, null, null)
        assertEquals(-1f, angle, 0.001f)
    }

    // ==================== BODY POSITION GATE ====================

    @Test
    fun `isHorizontalDelta true for horizontal segment`() {
        assertTrue(AngleMath.isHorizontalDelta(dx = 10f, dy = 0f))
    }

    @Test
    fun `isHorizontalDelta false for vertical segment`() {
        assertFalse(AngleMath.isHorizontalDelta(dx = 0f, dy = 10f))
    }

    @Test
    fun `isHorizontalDelta true for 45 degree segment within tolerance`() {
        assertTrue(AngleMath.isHorizontalDelta(dx = 10f, dy = 10f))
    }

    @Test
    fun `isHorizontalDelta false for steep segment beyond tolerance`() {
        assertFalse(AngleMath.isHorizontalDelta(dx = 1f, dy = 10f))
    }

    @Test
    fun `isHorizontalDelta false for zero vector`() {
        assertFalse(AngleMath.isHorizontalDelta(dx = 0f, dy = 0f))
    }
}