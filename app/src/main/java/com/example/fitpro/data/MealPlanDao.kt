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

    @Query("SELECT * FROM meal_plans WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getCurrentMealPlan(userId: Int): Flow<MealPlan?>

    @Query("SELECT * FROM meal_plans WHERE userId = :userId")
    fun getAllMealPlans(userId: Int): Flow<List<MealPlan>>

    @Query("SELECT COALESCE(SUM(totalCalories), 0) FROM meal_plans WHERE userId = :userId AND createdAt >= date('now', '-7 days')")
    fun getWeeklyCalories(userId: Int): Flow<Int>

    @Query("SELECT COALESCE(AVG(totalCalories), 0.0) FROM meal_plans WHERE userId = :userId AND createdAt >= date('now', '-7 days')")
    fun getAverageDailyCalories(userId: Int): Flow<Float>

    @Query("SELECT date(createdAt) as date, SUM(totalCalories) as calories FROM meal_plans WHERE userId = :userId AND date(createdAt) >= date('now', '-7 days') GROUP BY date(createdAt)")
    fun getDailyCaloriesForWeek(userId: Int): Flow<List<DailyCalories>>

    @Query("SELECT MAX(totalCalories) FROM meal_plans WHERE userId = :userId AND date(createdAt) >= date('now', '-7 days')")
    fun getMaxDailyCalories(userId: Int): Flow<Int?>

    @Query("SELECT * FROM meal_plans WHERE userId = :userId")
    fun getMealPlansForUser(userId: Int): Flow<List<MealPlan>>

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
        WHERE userId = :userId AND createdAt >= date('now', '-7 days')
        GROUP BY strftime('%w', createdAt)
    """)
    fun getWeeklyCaloriesByDay(userId: Int): Flow<List<WeeklyCalories>>

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)
}
