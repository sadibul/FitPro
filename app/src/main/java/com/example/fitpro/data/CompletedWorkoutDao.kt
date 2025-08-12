package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedWorkoutDao {
    @Insert
    suspend fun insertCompletedWorkout(completedWorkout: CompletedWorkout)

    @Query("SELECT * FROM completed_workouts WHERE userEmail = :userEmail ORDER BY completedAt DESC")
    fun getCompletedWorkouts(userEmail: String): Flow<List<CompletedWorkout>>

    @Query("SELECT * FROM completed_workouts WHERE userEmail = :userEmail AND completedAt >= :startOfDay ORDER BY completedAt DESC")
    fun getTodaysCompletedWorkouts(userEmail: String, startOfDay: Long): Flow<List<CompletedWorkout>>

    @Query("DELETE FROM completed_workouts WHERE userEmail = :userEmail")
    suspend fun deleteAllCompletedWorkouts(userEmail: String)

    @Query("SELECT COUNT(*) FROM completed_workouts WHERE userEmail = :userEmail")
    suspend fun getCompletedWorkoutCount(userEmail: String): Int
}
