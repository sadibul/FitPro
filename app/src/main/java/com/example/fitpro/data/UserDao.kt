package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("UPDATE user_profile SET dailySteps = :steps WHERE id = 1")
    suspend fun updateSteps(steps: Int)

    @Query("UPDATE user_profile SET caloriesBurned = :calories WHERE id = 1")
    suspend fun updateCalories(calories: Int)

    @Query("UPDATE user_profile SET heartRate = :rate WHERE id = 1")
    suspend fun updateHeartRate(rate: Int)
}
