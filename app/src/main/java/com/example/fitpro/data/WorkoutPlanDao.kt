package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class WorkoutTypeCount(
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "count") val count: Int
)

@Dao
interface WorkoutPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlan)

    @Query("SELECT * FROM workout_plans WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getCurrentWorkoutPlan(userId: Int): Flow<WorkoutPlan?>

    @Query("SELECT * FROM workout_plans WHERE userId = :userId")
    fun getAllWorkoutPlans(userId: Int): Flow<List<WorkoutPlan>>

    @Query("SELECT SUM(targetCalories) FROM workout_plans WHERE userId = :userId AND id IN (SELECT MAX(id) FROM workout_plans GROUP BY type)")
    fun getTotalTargetCalories(userId: Int): Flow<Int?>

    @Query("SELECT COUNT(*) FROM workout_plans WHERE userId = :userId AND date(createdAt) >= date('now', '-7 days')")
    fun getWeeklyWorkoutCount(userId: Int): Flow<Int>

    @Query("SELECT SUM(duration) FROM workout_plans WHERE userId = :userId AND date(createdAt) >= date('now', '-7 days')")
    fun getWeeklyWorkoutDuration(userId: Int): Flow<Int?>

    @Query("SELECT type, COUNT(*) as count FROM workout_plans WHERE userId = :userId GROUP BY type")
    fun getWorkoutTypeDistribution(userId: Int): Flow<List<WorkoutTypeCount>>

    @Query("SELECT * FROM workout_plans WHERE userId = :userId")
    fun getWorkoutPlansForUser(userId: Int): Flow<List<WorkoutPlan>>

    @Update
    suspend fun updateWorkoutPlan(workoutPlan: WorkoutPlan)

    @Delete
    suspend fun deleteWorkoutPlan(workoutPlan: WorkoutPlan)
}
