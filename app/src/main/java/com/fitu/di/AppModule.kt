package com.fitu.di

import android.content.Context
import android.hardware.SensorManager
import androidx.room.Room
import com.fitu.data.local.AppDatabase
import com.fitu.data.local.DatabaseMigrations
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.FoodCacheDao
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.dao.WorkoutPlanDao
import com.fitu.data.repository.BackupRepositoryImpl
import com.fitu.data.repository.DashboardRepositoryImpl
import com.fitu.data.repository.StreakRepository
import com.fitu.domain.repository.BackupRepository
import com.fitu.domain.repository.DashboardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // ✅ FIX: Use proper migrations instead of destructive migration
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            // Only use destructive migration on DOWNGRADE (e.g., user installs older version)
            // This is acceptable as downgrades are rare and usually intentional
            .fallbackToDestructiveMigrationOnDowngrade()
            // ❌ REMOVED: .fallbackToDestructiveMigration() - This was deleting user data!
            .build()
    }

    @Provides
    @Singleton
    fun provideStepDao(database: AppDatabase): StepDao {
        return database.stepDao()
    }

    @Provides
    @Singleton
    fun provideMealDao(database: AppDatabase): MealDao {
        return database.mealDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutPlanDao(database: AppDatabase): WorkoutPlanDao {
        return database.workoutPlanDao()
    }

    @Provides
    @Singleton
    fun provideFoodCacheDao(database: AppDatabase): FoodCacheDao {
        return database.foodCacheDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(
        mealDao: MealDao,
        workoutDao: WorkoutDao,
        stepDao: StepDao
    ): DashboardRepository {
        return DashboardRepositoryImpl(mealDao, workoutDao, stepDao)
    }

    @Provides
    @Singleton
    fun provideStreakRepository(
        stepDao: StepDao,
        userPreferencesRepository: UserPreferencesRepository
    ): StreakRepository {
        return StreakRepository(stepDao, userPreferencesRepository)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        stepDao: StepDao,
        mealDao: MealDao,
        workoutDao: WorkoutDao,
        workoutPlanDao: WorkoutPlanDao,
        userPreferencesRepository: UserPreferencesRepository,
        secureStorage: SecureStorage
    ): BackupRepository {
        return BackupRepositoryImpl(
            context,
            stepDao,
            mealDao,
            workoutDao,
            workoutPlanDao,
            userPreferencesRepository,
            secureStorage
        )
    }
}
