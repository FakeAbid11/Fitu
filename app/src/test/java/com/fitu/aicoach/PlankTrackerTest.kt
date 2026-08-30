package com.fitu.aicoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PlankTracker time tracking and form scoring.
 */
class PlankTrackerTest {

    private lateinit var tracker: PlankTracker
    private val DELTA = 0.1f

    @Before
    fun setup() {
        // Default plank: valid angle 160°-180°
        tracker = PlankTracker(
            minValidAngle = 160f,
            maxValidAngle = 180f
        )
    }

    @Test
    fun `initial state has zero values`() {
        assertEquals(0L, tracker.currentHoldTimeMs)
        assertEquals(0L, tracker.bestHoldTimeMs)
        assertEquals(0f, tracker.formScore, DELTA)
        assertFalse(tracker.isHolding)
    }

    @Test
    fun `starts holding when angle is valid`() {
        tracker.update(170f, 1000L)
        assertTrue(tracker.isHolding)
    }

    @Test
    fun `does not hold when angle below minimum`() {
        tracker.update(150f, 1000L)  // Below 160°
        assertFalse(tracker.isHolding)
    }

    @Test
    fun `accumulates hold time correctly`() {
        tracker.update(170f, 1000L)  // Start hold
        tracker.update(172f, 2000L)  // 1 second later
        tracker.update(175f, 3000L)  // 2 seconds later
        
        assertEquals(2000L, tracker.currentHoldTimeMs)
    }

    @Test
    fun `resets hold time when form breaks`() {
        tracker.update(170f, 1000L)  // Start hold
        tracker.update(172f, 2000L)  // Continue
        tracker.update(150f, 3000L)  // Form breaks
        
        assertEquals(0L, tracker.currentHoldTimeMs)
        assertFalse(tracker.isHolding)
    }

    @Test
    fun `updates best time when form breaks`() {
        tracker.update(170f, 1000L)  // Start
        tracker.update(172f, 3000L)  // 2 seconds
        tracker.update(150f, 4000L)  // Form breaks, should save best
        
        assertEquals(2000L, tracker.bestHoldTimeMs)
    }

    @Test
    fun `formScore is 10 at 180 degrees`() {
        tracker.update(180f, 1000L)
        assertEquals(10f, tracker.formScore, DELTA)
    }

    @Test
    fun `formScore is 0 at 160 degrees`() {
        tracker.update(160f, 1000L)
        assertEquals(0f, tracker.formScore, DELTA)
    }

    @Test
    fun `formScore is 5 at 170 degrees`() {
        tracker.update(170f, 1000L)
        assertEquals(5f, tracker.formScore, DELTA)
    }

    @Test
    fun `formScore scales linearly`() {
        // 165° should be 2.5
        tracker.update(165f, 1000L)
        assertEquals(2.5f, tracker.formScore, DELTA)
        
        // 175° should be 7.5
        tracker.update(175f, 2000L)
        assertEquals(7.5f, tracker.formScore, DELTA)
    }

    @Test
    fun `reset clears all values`() {
        tracker.update(170f, 1000L)
        tracker.update(172f, 3000L)
        tracker.update(150f, 4000L)  // Saves best
        
        tracker.reset()
        
        assertEquals(0L, tracker.currentHoldTimeMs)
        assertEquals(0L, tracker.bestHoldTimeMs)
        assertEquals(0f, tracker.formScore, DELTA)
        assertFalse(tracker.isHolding)
    }

    @Test
    fun `formatTime returns correct format`() {
        tracker.update(170f, 0L)
        tracker.update(175f, 65000L)  // 65 seconds = 1:05
        
        assertEquals("01:05", tracker.getCurrentTimeFormatted())
    }

    @Test
    fun `getFormFeedback returns appropriate messages`() {
        assertEquals("Get in position", tracker.getFormFeedback())
        
        tracker.update(180f, 1000L)  // Perfect form
        assertEquals("Perfect! 🔥", tracker.getFormFeedback())
        
        tracker.update(175f, 2000L)  // Score 7.5 -> Great
        assertEquals("Great form! 👍", tracker.getFormFeedback())
        
        tracker.update(170f, 3000L)  // Score 5 -> Good
        assertEquals("Good! Keep straight", tracker.getFormFeedback())
        
        tracker.update(163f, 4000L)  // Score 1.5 -> Adjust
        assertEquals("Adjust form", tracker.getFormFeedback())
    }

    @Test
    fun `invalid angle breaks form`() {
        tracker.update(170f, 1000L)
        assertTrue(tracker.isHolding)
        
        tracker.update(-1f, 2000L)  // Invalid
        assertFalse(tracker.isHolding)
    }
}
