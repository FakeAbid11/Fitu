package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitu.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getMealsInRange(startDate: Long, endDate: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getMealsForDay(startDate: Long, endDate: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getMealsInRangeSync(startDate: Long, endDate: Long): List<MealEntity>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?

    @Query("SELECT SUM(calories) FROM meals WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalCaloriesInRange(startDate: Long, endDate: Long): Int?

    @Query("SELECT COALESCE(SUM(calories), 0) FROM meals WHERE date >= :startDate AND date <= :endDate")
    fun getCaloriesConsumedForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(protein), 0) FROM meals WHERE date >= :startDate AND date <= :endDate")
    fun getProteinForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(carbs), 0) FROM meals WHERE date >= :startDate AND date <= :endDate")
    fun getCarbsForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(fat), 0) FROM meals WHERE date >= :startDate AND date <= :endDate")
    fun getFatsForDay(startDate: Long, endDate: Long): Flow<Int>

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
