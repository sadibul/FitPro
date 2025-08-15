package com.example.fitpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val email: String, // Use email as primary key for multi-user support
    val name: String,
    val password: String, // Store password for authentication
    val gender: String,
    val age: Int,
    val weight: Float,
    val height: Int,
    val currentPlan: String = "Weight Loss Plan", // Default plan
    val dailySteps: Int = 0,
    val caloriesBurned: Int = 0,
    val heartRate: Int = 0,
    val stepTarget: Int = 0, // Default step target is 0
    val calorieTarget: Int = 0, // Default calorie target is 0
    val isStepTargetCompleted: Boolean = false, // Track if current step target is completed
    val profileImageUri: String? = null // Profile image URI
) {
    fun calculateBMI(): Float {
        return weight / ((height / 100f).pow(2))
    }

    fun getBMICategory(): String {
        return when (calculateBMI()) {
            in 0f..18.5f -> "Underweight"
            in 18.5f..24.9f -> "Normal"
            in 25f..29.9f -> "Overweight"
            else -> "Obese"
        }
    }

    fun getBMITips(): String {
        return when (getBMICategory()) {
            "Underweight" -> "Focus on nutrient-rich foods and strength training to gain healthy weight."
            "Normal" -> "Maintain your healthy lifestyle with balanced diet and regular exercise."
            "Overweight" -> "Incorporate more cardio and maintain a caloric deficit for healthy weight loss."
            else -> "Consult with a healthcare provider for a personalized weight management plan."
        }
    }
}
