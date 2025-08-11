package com.example.fitpro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.Executors

@Database(
    entities = [UserProfile::class, WorkoutPlan::class, MealPlan::class],
    version = 12, // Incremented version for SQLite fix
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun mealPlanDao(): MealPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Dedicated thread pool for database operations
        private val databaseExecutor = Executors.newFixedThreadPool(4)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitpro_database"
                )
                    .fallbackToDestructiveMigration() // Handle all migrations safely
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .setQueryExecutor(databaseExecutor) // Use dedicated executor
                    .setTransactionExecutor(databaseExecutor)
                    // Remove callback that was causing SQLite errors
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
