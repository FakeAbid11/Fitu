package com.fitu.ui.coach

import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    @Mock
    private lateinit var workoutDao: WorkoutDao

    private lateinit var viewModel: CoachViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CoachViewModel(workoutDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startWorkout sets state to ACTIVE`() = runTest {
        viewModel.startWorkout()
        assertEquals(WorkoutState.ACTIVE, viewModel.workoutState.value)
    }

    @Test
    fun `pauseWorkout sets state to PAUSED`() = runTest {
        viewModel.startWorkout()
        viewModel.pauseWorkout()
        assertEquals(WorkoutState.PAUSED, viewModel.workoutState.value)
    }

    @Test
    fun `stopWorkout saves workout if active`() = runTest {
        viewModel.startWorkout()
        
        // Simulate some reps
        val exerciseResult = ExerciseResult("Squat", 10, FormFeedback(true, "Good"))
        viewModel.updateExerciseResult(exerciseResult)
        
        viewModel.stopWorkout()
        
        assertEquals(WorkoutState.COMPLETED, viewModel.workoutState.value)
        Mockito.verify(workoutDao).insertWorkout(Mockito.any(WorkoutEntity::class.java))
    }
}
