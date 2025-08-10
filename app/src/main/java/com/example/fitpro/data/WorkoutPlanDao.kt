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

    @Query("SELECT * FROM workout_plans WHERE userEmail = :userEmail ORDER BY id DESC LIMIT 1")
    fun getCurrentWorkoutPlan(userEmail: String): Flow<WorkoutPlan?>

    @Query("SELECT * FROM workout_plans WHERE userEmail = :userEmail ORDER BY createdAt DESC")
    fun getAllWorkoutPlans(userEmail: String): Flow<List<WorkoutPlan>>

    @Query("SELECT SUM(targetCalories) FROM workout_plans WHERE userEmail = :userEmail AND id IN (SELECT MAX(id) FROM workout_plans GROUP BY type)")
    fun getTotalTargetCalories(userEmail: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM workout_plans WHERE userEmail = :userEmail AND date(createdAt/1000, 'unixepoch') >= date('now', '-7 days')")
    fun getWeeklyWorkoutCount(userEmail: String): Flow<Int>

    @Query("SELECT SUM(duration) FROM workout_plans WHERE userEmail = :userEmail AND date(createdAt/1000, 'unixepoch') >= date('now', '-7 days')")
    fun getWeeklyWorkoutDuration(userEmail: String): Flow<Int?>

    @Query("SELECT type, COUNT(*) as count FROM workout_plans WHERE userEmail = :userEmail GROUP BY type")
    fun getWorkoutTypeDistribution(userEmail: String): Flow<List<WorkoutTypeCount>>

    @Query("SELECT * FROM workout_plans WHERE userEmail = :userEmail")
    fun getWorkoutPlansForUser(userEmail: String): Flow<List<WorkoutPlan>>

    @Update
    suspend fun updateWorkoutPlan(workoutPlan: WorkoutPlan)

    @Delete
    suspend fun deleteWorkoutPlan(workoutPlan: WorkoutPlan)
}
