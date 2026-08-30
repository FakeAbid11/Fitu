package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkoutsInRange(startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getWorkoutsInRangeSync(startDate: Long, endDate: Long): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): WorkoutEntity?

    @Query("SELECT COUNT(*) FROM workouts WHERE date >= :startDate AND date <= :endDate")
    suspend fun getWorkoutCountInRange(startDate: Long, endDate: Long): Int

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(SUM(reps), 0) FROM workouts WHERE date >= :startDate AND date <= :endDate")
    fun getTotalRepsInRange(startDate: Long, endDate: Long): Flow<Int>
}
