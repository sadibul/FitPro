package com.example.fitpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(
    entities = [UserProfile::class, WorkoutPlan::class, MealPlan::class, CompletedWorkout::class],
    version = 16, // Incremented version for CompletedWorkout table
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun completedWorkoutDao(): CompletedWorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Dedicated thread pool for database operations
        private val databaseExecutor = Executors.newFixedThreadPool(4)
        
        // Migration from version 12 to 13
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to workout_plans table
                database.execSQL("ALTER TABLE workout_plans ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE workout_plans ADD COLUMN categoryName TEXT NOT NULL DEFAULT ''")
                
                // Make targetCalories nullable by creating new table and copying data
                database.execSQL("""
                    CREATE TABLE workout_plans_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userEmail TEXT NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER NOT NULL DEFAULT 0,
                        categoryName TEXT NOT NULL DEFAULT '',
                        duration INTEGER NOT NULL,
                        targetCalories INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO workout_plans_new (id, userEmail, type, categoryId, categoryName, duration, targetCalories, createdAt)
                    SELECT id, userEmail, type, 0, type, duration, targetCalories, createdAt
                    FROM workout_plans
                """)
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE workout_plans")
                database.execSQL("ALTER TABLE workout_plans_new RENAME TO workout_plans")
            }
        }

        // Migration from version 13 to 14
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add calorieTarget column to user_profile table
                database.execSQL("ALTER TABLE user_profile ADD COLUMN calorieTarget INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 14 to 15
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update meal_plans table: change userId to userEmail and add isCompleted
                database.execSQL("""
                    CREATE TABLE meal_plans_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userEmail TEXT NOT NULL,
                        name TEXT NOT NULL,
                        breakfast TEXT NOT NULL,
                        lunch TEXT NOT NULL,
                        dinner TEXT NOT NULL,
                        totalCalories INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL
                    )
                """)
                
                // Copy existing data (assuming userId 1 maps to default email)
                database.execSQL("""
                    INSERT INTO meal_plans_new (id, userEmail, name, breakfast, lunch, dinner, totalCalories, isCompleted, createdAt)
                    SELECT id, 'user@example.com', name, breakfast, lunch, dinner, totalCalories, 0, createdAt
                    FROM meal_plans
                """)
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE meal_plans")
                database.execSQL("ALTER TABLE meal_plans_new RENAME TO meal_plans")
            }
        }

        // Migration from version 15 to 16
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create completed_workouts table
                database.execSQL("""
                    CREATE TABLE completed_workouts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userEmail TEXT NOT NULL,
                        workoutType TEXT NOT NULL,
                        categoryName TEXT NOT NULL,
                        duration INTEGER NOT NULL,
                        targetCalories INTEGER,
                        actualDuration INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitpro_database"
                )
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16) // Add new migration
                    .fallbackToDestructiveMigration() // Fallback for other migrations
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .setQueryExecutor(databaseExecutor) // Use dedicated executor
                    .setTransactionExecutor(databaseExecutor)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Method to close database properly
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
