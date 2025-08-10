package com.example.fitpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String, // Changed from userId to userEmail
    val type: String,
    val duration: Int, // in minutes
    val targetCalories: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val breakfast: String, // JSON string of meal details
    val lunch: String,
    val dinner: String,
    val totalCalories: Int,
    val createdAt: String = java.time.LocalDateTime.now().toString()
)
