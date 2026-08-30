package com.fitu.aicoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RepCounter state machine logic.
 * Tests state transitions and rep counting for different exercises.
 */
class RepCounterTest {

    private lateinit var pushUpCounter: RepCounter
    private lateinit var curlCounter: RepCounter

    @Before
    fun setup() {
        // Push-up: DOWN = angle < 90°, UP = angle > 160°
        pushUpCounter = RepCounter(
            downThreshold = 90f,
            upThreshold = 160f,
            exerciseType = ExerciseType.PUSH_UP
        )

        // Curl: DOWN = angle > 160° (extended), UP = angle < 60° (curled)
        curlCounter = RepCounter(
            downThreshold = 160f,
            upThreshold = 60f,
            exerciseType = ExerciseType.DUMBBELL_CURL
        )
    }

    // ==================== PUSH-UP TESTS ====================

    @Test
    fun `pushup - initial state is UNKNOWN with 0 reps`() {
        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - transitions to UP when angle above upThreshold`() {
        // Start with extended arms (angle > 160°)
        pushUpCounter.update(170f)
        assertEquals(RepCounter.State.UP, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - transitions to DOWN when angle below downThreshold`() {
        // Start UP
        pushUpCounter.update(170f)
        // Go DOWN (bend arms)
        pushUpCounter.update(80f)
        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - counts rep when completing full cycle UP-DOWN-UP`() {
        // Start UP
        pushUpCounter.update(170f)
        // Go DOWN
        pushUpCounter.update(80f)
        // Return UP - should count rep
        val counted = pushUpCounter.update(165f)
        
        assertTrue(counted)
        assertEquals(RepCounter.State.UP, pushUpCounter.currentState)
        assertEquals(1, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - does not count rep on partial movement`() {
        // Start UP
        pushUpCounter.update(170f)
        // Go only halfway down (above threshold)
        pushUpCounter.update(100f)
        // Return UP
        val counted = pushUpCounter.update(165f)
        
        assertFalse(counted)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - counts multiple reps correctly`() {
        // Rep 1
        pushUpCounter.update(170f)  // UP
        pushUpCounter.update(80f)   // DOWN
        pushUpCounter.update(165f)  // UP - rep 1
        
        // Rep 2
        pushUpCounter.update(80f)   // DOWN
        pushUpCounter.update(165f)  // UP - rep 2
        
        // Rep 3
        pushUpCounter.update(75f)   // DOWN
        pushUpCounter.update(170f)  // UP - rep 3
        
        assertEquals(3, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - ignores invalid angles`() {
        pushUpCounter.update(170f)
        pushUpCounter.update(80f)
        
        // Invalid angle should not change state
        val counted = pushUpCounter.update(-1f)
        
        assertFalse(counted)
        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)
    }

    @Test
    fun `pushup - reset clears state and count`() {
        pushUpCounter.update(170f)
        pushUpCounter.update(80f)
        pushUpCounter.update(165f)
        
        pushUpCounter.reset()
        
        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    // ==================== CURL TESTS (INVERTED LOGIC) ====================

    @Test
    fun `curl - transitions to DOWN when angle above downThreshold (arm extended)`() {
        // Extended arm (start position for curl)
        curlCounter.update(170f)
        assertEquals(RepCounter.State.DOWN, curlCounter.currentState)
    }

    @Test
    fun `curl - transitions to UP when angle below upThreshold (arm curled)`() {
        curlCounter.update(170f)  // Extended (DOWN)
        curlCounter.update(50f)   // Curled (UP)
        assertEquals(RepCounter.State.UP, curlCounter.currentState)
    }

    @Test
    fun `curl - counts rep when completing cycle DOWN-UP-DOWN`() {
        curlCounter.update(170f)  // Start extended
        curlCounter.update(50f)   // Curl up
        val counted = curlCounter.update(165f)  // Extend again - rep counted
        
        assertTrue(counted)
        assertEquals(1, curlCounter.repCount)
    }

    @Test
    fun `curl - getStateDisplay returns correct strings`() {
        assertEquals("Ready", curlCounter.getStateDisplay())
        
        curlCounter.update(170f)
        assertEquals("Down", curlCounter.getStateDisplay())
        
        curlCounter.update(50f)
        assertEquals("Up", curlCounter.getStateDisplay())
    }
}
