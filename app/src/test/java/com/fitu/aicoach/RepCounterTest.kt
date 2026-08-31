package com.fitu.aicoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RepCounter state machine logic.
 *
 * The counter deliberately includes anti-phantom-rep protection:
 *  1. angle smoothing (average of the last 5 frames)
 *  2. frame debounce (a position must be held 3 consecutive frames)
 *  3. minimum interval between counted reps (injected as 0 here for determinism)
 *
 * Because of smoothing + debounce, a steady position needs several identical
 * frames to register (worst case ~7), so these tests feed each position for
 * DEFAULT_FRAMES consecutive frames, like a real camera stream would.
 */
class RepCounterTest {

    companion object {
        // 5 (smoothing window) + 3 (debounce) + margin
        private const val DEFAULT_FRAMES = 10
    }

    private lateinit var pushUpCounter: RepCounter
    private lateinit var curlCounter: RepCounter

    @Before
    fun setup() {
        // Push-up: DOWN = angle < 90, UP = angle > 160
        pushUpCounter = RepCounter(
            downThreshold = 90f,
            upThreshold = 160f,
            exerciseType = ExerciseType.PUSH_UP,
            minRepIntervalOverride = 0L
        )

        // Curl: DOWN = angle > 160 (extended), UP = angle < 60 (curled)
        curlCounter = RepCounter(
            downThreshold = 160f,
            upThreshold = 60f,
            exerciseType = ExerciseType.DUMBBELL_CURL,
            minRepIntervalOverride = 0L
        )
    }

    /** Feed the same angle for [frames] consecutive frames (simulates a steady camera view). */
    private fun feed(counter: RepCounter, angle: Float, frames: Int = DEFAULT_FRAMES) {
        repeat(frames) { counter.update(angle) }
    }

    /** Feed an angle for [frames] frames and report whether any rep was counted. */
    private fun feedAndCount(counter: RepCounter, angle: Float, frames: Int = DEFAULT_FRAMES): Boolean {
        var counted = false
        repeat(frames) { if (counter.update(angle)) counted = true }
        return counted
    }

    // ==================== PUSH-UP TESTS ====================

    @Test
    fun `pushup - initial state is UNKNOWN with 0 reps`() {
        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - single frame is not enough to change state (debounce)`() {
        pushUpCounter.update(170f)
        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)
    }

    @Test
    fun `pushup - transitions to UP when angle above upThreshold`() {
        feed(pushUpCounter, 170f)
        assertEquals(RepCounter.State.UP, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - transitions to DOWN when angle below downThreshold`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)
        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - counts rep when completing full cycle UP-DOWN-UP`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)
        val counted = feedAndCount(pushUpCounter, 165f)

        assertTrue(counted)
        assertEquals(RepCounter.State.UP, pushUpCounter.currentState)
        assertEquals(1, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - does not count rep on partial movement`() {
        // Calibrate with a full-depth rep first (adaptive thresholds learn the
        // 80-170 range), then try a mid-range partial rep (130 deg).
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)
        feed(pushUpCounter, 170f)

        val counted = feedAndCount(pushUpCounter, 130f)

        assertFalse(counted)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - counts multiple reps correctly`() {
        repeat(3) {
            feed(pushUpCounter, 170f)
            feed(pushUpCounter, 80f)
            feedAndCount(pushUpCounter, 170f)
        }
        assertEquals(3, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - ignores invalid angles`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)    // now DOWN
        val counted = pushUpCounter.update(-1f)

        assertFalse(counted)
        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)
    }

    @Test
    fun `pushup - reset clears state and count`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)
        feedAndCount(pushUpCounter, 165f)

        pushUpCounter.reset()

        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    // ==================== CURL TESTS (INVERTED LOGIC) ====================

    @Test
    fun `curl - transitions to DOWN when angle above downThreshold (arm extended)`() {
        feed(curlCounter, 170f)
        assertEquals(RepCounter.State.DOWN, curlCounter.currentState)
    }

    @Test
    fun `curl - transitions to UP when angle below upThreshold (arm curled)`() {
        feed(curlCounter, 170f)
        feed(curlCounter, 50f)
        assertEquals(RepCounter.State.UP, curlCounter.currentState)
    }

    @Test
    fun `curl - counts rep when completing cycle DOWN-UP-DOWN`() {
        feed(curlCounter, 170f)
        feed(curlCounter, 50f)
        val counted = feedAndCount(curlCounter, 165f)

        assertTrue(counted)
        assertEquals(1, curlCounter.repCount)
    }

    @Test
    fun `curl - single update does not count rep`() {
        feed(curlCounter, 170f)
        feed(curlCounter, 50f)
        val counted = curlCounter.update(165f)

        assertFalse(counted)
    }

    @Test
    fun `curl - getStateDisplay returns correct strings`() {
        assertEquals("Ready", curlCounter.getStateDisplay())

        feed(curlCounter, 170f)
        assertEquals("Down", curlCounter.getStateDisplay())

        feed(curlCounter, 50f)
        assertEquals("Up", curlCounter.getStateDisplay())
    }

    // ==================== ACCURACY UPGRADE TESTS ====================

    @Test
    fun `pushup - median smoothing rejects single-frame spikes`() {
        feed(pushUpCounter, 170f)   // UP
        feed(pushUpCounter, 80f)    // DOWN, calibrated (80-170 range)

        // Isolated spike frames must not flip the state via the median filter
        pushUpCounter.update(170f)
        pushUpCounter.update(30f)
        pushUpCounter.update(170f)

        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)

        // The real rep still completes cleanly afterwards
        val counted = feedAndCount(pushUpCounter, 165f)
        assertTrue(counted)
        assertEquals(1, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - tolerates brief tracking dropouts`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)    // DOWN, calibrated

        // A short detection dropout (below MAX_DROPOUT_FRAMES) coasts
        repeat(5) { pushUpCounter.update(-1f) }

        val counted = feedAndCount(pushUpCounter, 165f)
        assertTrue(counted)
        assertEquals(1, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - resets after prolonged tracking loss`() {
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 80f)
        assertEquals(RepCounter.State.DOWN, pushUpCounter.currentState)

        // Prolonged dropout (at/above MAX_DROPOUT_FRAMES) resets the machine
        repeat(10) { pushUpCounter.update(-1f) }
        assertEquals(RepCounter.State.UNKNOWN, pushUpCounter.currentState)

        // And counting restarts cleanly afterwards (no phantom rep)
        feed(pushUpCounter, 170f)
        assertEquals(RepCounter.State.UP, pushUpCounter.currentState)
        assertEquals(0, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - adapts thresholds to the user range of motion`() {
        // User warms up at full extension but never bends past 110 deg
        // (fixed thresholds 90/160 would count nothing at all)
        feed(pushUpCounter, 170f)
        feed(pushUpCounter, 110f)

        val counted = feedAndCount(pushUpCounter, 165f)

        assertTrue(counted)
        assertEquals(1, pushUpCounter.repCount)
    }

    @Test
    fun `pushup - flags reps blocked by minimum interval`() {
        val slowCounter = RepCounter(
            downThreshold = 90f,
            upThreshold = 160f,
            exerciseType = ExerciseType.PUSH_UP,
            minRepIntervalOverride = 60000L
        )

        feed(slowCounter, 170f)
        feed(slowCounter, 80f)
        feedAndCount(slowCounter, 170f)  // first rep OK (long ago since last)
        assertEquals(1, slowCounter.repCount)
        assertFalse(slowCounter.lastRepWasBlocked)

        // Second rep immediately after is blocked by the min interval
        feed(slowCounter, 80f)
        feedAndCount(slowCounter, 170f)
        assertEquals(1, slowCounter.repCount)
        assertTrue(slowCounter.lastRepWasBlocked)
    }
}