package com.example.fitpro.data

data class WorkoutCategory(
    val id: Int,
    val name: String,
    val icon: String, // We'll use this for icon selection
    val minDuration: Int, // in minutes
    val maxDuration: Int, // in minutes
    val minCalories: Int?, // null if calories not applicable
    val maxCalories: Int?, // null if calories not applicable
    val hasCalories: Boolean = minCalories != null && maxCalories != null
)

object WorkoutCategories {
    val categories = listOf(
        WorkoutCategory(
            id = 1,
            name = "Strength Training",
            icon = "strength",
            minDuration = 20,
            maxDuration = 120,
            minCalories = 100,
            maxCalories = 1000
        ),
        WorkoutCategory(
            id = 2,
            name = "Cardio",
            icon = "cardio",
            minDuration = 15,
            maxDuration = 90,
            minCalories = 150,
            maxCalories = 900
        ),
        WorkoutCategory(
            id = 3,
            name = "Yoga",
            icon = "yoga",
            minDuration = 20,
            maxDuration = 80,
            minCalories = null,
            maxCalories = null
        ),
        WorkoutCategory(
            id = 4,
            name = "Pilates",
            icon = "pilates",
            minDuration = 20,
            maxDuration = 75,
            minCalories = 80,
            maxCalories = 400
        ),
        WorkoutCategory(
            id = 5,
            name = "HIIT",
            icon = "hiit",
            minDuration = 10,
            maxDuration = 40,
            minCalories = 150,
            maxCalories = 600
        ),
        WorkoutCategory(
            id = 6,
            name = "Dance Fitness / Zumba",
            icon = "dance",
            minDuration = 20,
            maxDuration = 60,
            minCalories = 200,
            maxCalories = 600
        ),
        WorkoutCategory(
            id = 7,
            name = "Stretching / Mobility",
            icon = "stretching",
            minDuration = 10,
            maxDuration = 40,
            minCalories = null,
            maxCalories = null
        ),
        WorkoutCategory(
            id = 8,
            name = "Flexibility Training",
            icon = "flexibility",
            minDuration = 15,
            maxDuration = 60,
            minCalories = null,
            maxCalories = null
        ),
        WorkoutCategory(
            id = 9,
            name = "CrossFit / Functional Training",
            icon = "crossfit",
            minDuration = 15,
            maxDuration = 60,
            minCalories = 200,
            maxCalories = 800
        ),
        WorkoutCategory(
            id = 10,
            name = "Swimming",
            icon = "swimming",
            minDuration = 20,
            maxDuration = 90,
            minCalories = 200,
            maxCalories = 800
        ),
        WorkoutCategory(
            id = 11,
            name = "Cycling",
            icon = "cycling",
            minDuration = 20,
            maxDuration = 120,
            minCalories = 150,
            maxCalories = 1000
        ),
        WorkoutCategory(
            id = 12,
            name = "Walking (Brisk)",
            icon = "walking",
            minDuration = 15,
            maxDuration = 120,
            minCalories = 50,
            maxCalories = 500
        ),
        WorkoutCategory(
            id = 13,
            name = "Rowing",
            icon = "rowing",
            minDuration = 15,
            maxDuration = 60,
            minCalories = 150,
            maxCalories = 600
        ),
        WorkoutCategory(
            id = 14,
            name = "Boxing / Kickboxing",
            icon = "boxing",
            minDuration = 15,
            maxDuration = 60,
            minCalories = 200,
            maxCalories = 700
        ),
        WorkoutCategory(
            id = 15,
            name = "Hiking",
            icon = "hiking",
            minDuration = 30,
            maxDuration = 180,
            minCalories = 200,
            maxCalories = 1200
        ),
        WorkoutCategory(
            id = 16,
            name = "Bodyweight Circuit",
            icon = "bodyweight",
            minDuration = 10,
            maxDuration = 60,
            minCalories = 100,
            maxCalories = 500
        )
    )
}
