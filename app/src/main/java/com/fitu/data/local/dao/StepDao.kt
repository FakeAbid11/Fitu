package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitu.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(step: StepEntity)

    @Update
    suspend fun update(step: StepEntity)

    @Query("SELECT * FROM steps WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getStepsInRange(startDate: Long, endDate: Long): List<StepEntity>

    @Query("SELECT * FROM steps WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStepsInRangeFlow(startDate: Long, endDate: Long): Flow<List<StepEntity>>

    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    suspend fun getStepsForDate(date: String): StepEntity?

    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    fun getStepsForDateFlow(date: String): Flow<StepEntity?>

    @Query("SELECT * FROM steps WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStepsBetweenDates(startDate: String, endDate: String): Flow<List<StepEntity>>

    @Query("SELECT * FROM steps WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getStepsBetweenDatesSync(startDate: String, endDate: String): List<StepEntity>

    @Query("SELECT SUM(steps) FROM steps WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalStepsInRange(startDate: Long, endDate: Long): Int?

    @Query("DELETE FROM steps WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: String)
}
