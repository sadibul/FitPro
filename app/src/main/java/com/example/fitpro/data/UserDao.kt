package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfile)

    @Query("SELECT * FROM user_profile WHERE email = :email")
    fun getUserProfile(email: String): Flow<UserProfile?>

    @Query("UPDATE user_profile SET dailySteps = :steps WHERE email = :email")
    suspend fun updateSteps(email: String, steps: Int)

    @Query("UPDATE user_profile SET caloriesBurned = :calories WHERE email = :email")
    suspend fun updateCalories(email: String, calories: Int)

    @Query("UPDATE user_profile SET heartRate = :rate WHERE email = :email")
    suspend fun updateHeartRate(email: String, rate: Int)
    
    @Query("UPDATE user_profile SET stepTarget = :target WHERE email = :email")
    suspend fun updateStepTarget(email: String, target: Int)
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE email = :email)")
    suspend fun userExists(email: String): Boolean
}
