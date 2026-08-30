package com.fitu.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database Migrations for Fitu App
 * 
 * IMPORTANT: 
 * - Never delete old migrations! Users who skip versions need them.
 * - Always test migrations before release.
 * - Schema is exported to app/schemas/ for testing.
 * 
 * Current Schema (Version 9):
 * 
 * TABLE: steps
 *   - id: INTEGER PRIMARY KEY AUTOINCREMENT
 *   - date: TEXT (Format: "yyyy-MM-dd", UNIQUE INDEX)
 *   - steps: INTEGER
 *   - goal: INTEGER DEFAULT 10000
 *   - caloriesBurned: INTEGER DEFAULT 0
 *   - distanceMeters: REAL DEFAULT 0
 *   - lastUpdated: INTEGER
 * 
 * TABLE: meals
 *   - id: INTEGER PRIMARY KEY AUTOINCREMENT
 *   - name: TEXT
 *   - description: TEXT DEFAULT ""
 *   - calories: INTEGER
 *   - protein: INTEGER DEFAULT 0
 *   - carbs: INTEGER DEFAULT 0
 *   - fat: INTEGER DEFAULT 0
 *   - fiber: INTEGER DEFAULT 0
 *   - mealType: TEXT DEFAULT "snack"
 *   - portion: REAL DEFAULT 1
 *   - photoUri: TEXT (nullable)
 *   - date: INTEGER (INDEX)
 *   - timestamp: INTEGER
 *   - createdAt: INTEGER
 * 
 * TABLE: workouts
 *   - id: INTEGER PRIMARY KEY AUTOINCREMENT
 *   - exerciseType: TEXT DEFAULT ""
 *   - type: TEXT DEFAULT ""
 *   - reps: INTEGER DEFAULT 0
 *   - sets: INTEGER DEFAULT 0
 *   - durationSeconds: INTEGER DEFAULT 0
 *   - durationMs: INTEGER DEFAULT 0
 *   - caloriesBurned: INTEGER DEFAULT 0
 *   - date: INTEGER (INDEX)
 *   - timestamp: INTEGER
 *   - createdAt: INTEGER
 * 
 * TABLE: workout_plans
 *   - id: INTEGER PRIMARY KEY AUTOINCREMENT
 *   - name: TEXT
 *   - description: TEXT DEFAULT ""
 *   - exercises: TEXT DEFAULT ""
 *   - difficulty: TEXT DEFAULT "intermediate"
 *   - durationMinutes: INTEGER DEFAULT 30
 *   - duration: INTEGER DEFAULT 30
 *   - targetMuscles: TEXT DEFAULT ""
 *   - muscleGroups: TEXT DEFAULT ""
 *   - equipment: TEXT DEFAULT ""
 *   - createdAt: INTEGER
 * 
 * TABLE: food_cache
 *   - query: TEXT PRIMARY KEY
 *   - resultJson: TEXT
 *   - timestamp: INTEGER
 */
object DatabaseMigrations {

    /**
     * All migrations - Add new migrations to this array
     * Room will automatically apply needed migrations in sequence
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        // Add migrations here as needed, e.g.:
        // MIGRATION_9_10,
        // MIGRATION_10_11,
    )

    // ============================================================
    // MIGRATION TEMPLATES - Uncomment and modify when needed
    // ============================================================

    /**
     * Example: Migration from version 9 to 10
     * Adding a new column to steps table
     */
    /*
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example: Add sync status column
            database.execSQL(
                "ALTER TABLE steps ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    */

    /**
     * Example: Adding a completely new table
     */
    /*
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS user_achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    achievementType TEXT NOT NULL,
                    achievedAt INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
            
            // Add index if needed
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_user_achievements_achievementType ON user_achievements(achievementType)"
            )
        }
    }
    */

    /**
     * Example: Adding multiple columns at once
     */
    /*
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // SQLite requires separate ALTER statements for each column
            database.execSQL(
                "ALTER TABLE meals ADD COLUMN sodium INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE meals ADD COLUMN sugar INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
    */

    /**
     * Example: Complex migration - recreating table (for column removal/rename)
     * Note: SQLite doesn't support DROP COLUMN directly in older versions
     */
    /*
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Create new table with desired schema
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS meals_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    calories INTEGER NOT NULL,
                    protein INTEGER NOT NULL DEFAULT 0,
                    carbs INTEGER NOT NULL DEFAULT 0,
                    fat INTEGER NOT NULL DEFAULT 0,
                    date INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // 2. Copy data from old table
            database.execSQL("""
                INSERT INTO meals_new (id, name, calories, protein, carbs, fat, date, createdAt)
                SELECT id, name, calories, protein, carbs, fat, date, createdAt FROM meals
            """.trimIndent())
            
            // 3. Drop old table
            database.execSQL("DROP TABLE meals")
            
            // 4. Rename new table to original name
            database.execSQL("ALTER TABLE meals_new RENAME TO meals")
            
            // 5. Recreate indexes
            database.execSQL("CREATE INDEX IF NOT EXISTS index_meals_date ON meals(date)")
        }
    }
    */
}
