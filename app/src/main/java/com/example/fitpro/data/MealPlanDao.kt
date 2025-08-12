package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class DailyCalories(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "calories") val calories: Int
)

data class WeeklyCalories(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "calories") val calories: Int
)

@Dao
interface MealPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(plan: MealPlan)

    @Query("SELECT * FROM meal_plans WHERE userEmail = :userEmail ORDER BY id DESC LIMIT 1")
    fun getCurrentMealPlan(userEmail: String): Flow<MealPlan?>

    @Query("SELECT * FROM meal_plans WHERE userEmail = :userEmail")
    fun getAllMealPlans(userEmail: String): Flow<List<MealPlan>>

    @Query("SELECT COALESCE(SUM(totalCalories), 0) FROM meal_plans WHERE userEmail = :userEmail AND createdAt >= date('now', '-7 days')")
    fun getWeeklyCalories(userEmail: String): Flow<Int>

    @Query("SELECT COALESCE(AVG(totalCalories), 0.0) FROM meal_plans WHERE userEmail = :userEmail AND createdAt >= date('now', '-7 days')")
    fun getAverageDailyCalories(userEmail: String): Flow<Float>

    @Query("SELECT date(createdAt) as date, SUM(totalCalories) as calories FROM meal_plans WHERE userEmail = :userEmail AND date(createdAt) >= date('now', '-7 days') GROUP BY date(createdAt)")
    fun getDailyCaloriesForWeek(userEmail: String): Flow<List<DailyCalories>>

    @Query("SELECT MAX(totalCalories) FROM meal_plans WHERE userEmail = :userEmail AND date(createdAt) >= date('now', '-7 days')")
    fun getMaxDailyCalories(userEmail: String): Flow<Int?>

    @Query("SELECT * FROM meal_plans WHERE userEmail = :userEmail")
    fun getMealPlansForUser(userEmail: String): Flow<List<MealPlan>>

    @Query("UPDATE meal_plans SET isCompleted = :isCompleted WHERE id = :mealPlanId")
    suspend fun updateMealPlanCompletion(mealPlanId: Int, isCompleted: Boolean)

    @Query("SELECT * FROM meal_plans WHERE userEmail = :userEmail AND isCompleted = 1 ORDER BY id DESC LIMIT 1")
    fun getLastCompletedMealPlan(userEmail: String): Flow<MealPlan?>

    @Query("""
        SELECT 
            CASE strftime('%w', createdAt)
                WHEN '0' THEN 'Sun'
                WHEN '1' THEN 'Mon'
                WHEN '2' THEN 'Tue'
                WHEN '3' THEN 'Wed'
                WHEN '4' THEN 'Thu'
                WHEN '5' THEN 'Fri'
                WHEN '6' THEN 'Sat'
            END as day,
            COALESCE(SUM(totalCalories), 0) as calories
        FROM meal_plans 
        WHERE userEmail = :userEmail AND createdAt >= date('now', '-7 days')
        GROUP BY strftime('%w', createdAt)
    """)
    fun getWeeklyCaloriesByDay(userEmail: String): Flow<List<WeeklyCalories>>

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)
}
