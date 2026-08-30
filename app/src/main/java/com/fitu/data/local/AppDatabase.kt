package com.fitu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fitu.data.local.dao.FoodCacheDao
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.dao.WorkoutPlanDao
import com.fitu.data.local.entity.FoodCacheEntity
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.local.entity.WorkoutEntity
import com.fitu.data.local.entity.WorkoutPlanEntity

@Database(
    entities = [
        StepEntity::class,
        MealEntity::class,
        WorkoutEntity::class,
        WorkoutPlanEntity::class,
        FoodCacheEntity::class
    ],
    version = 9,
    exportSchema = true  // Changed from false - enables schema export for migration testing
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun stepDao(): StepDao
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun foodCacheDao(): FoodCacheDao

    companion object {
        const val DATABASE_NAME = "fitu_database"
    }
}
