package com.example.fitpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.*
import com.example.fitpro.ui.theme.ChartBlue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    workoutPlanDao: WorkoutPlanDao,
    mealPlanDao: MealPlanDao
) {
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val weeklyCalories by mealPlanDao.getWeeklyCalories(1)
        .collectAsStateWithLifecycle(initialValue = 0)
    val weeklyWorkouts by workoutPlanDao.getWeeklyWorkoutCount(1)
        .collectAsStateWithLifecycle(initialValue = 0)
    val weeklyDuration by workoutPlanDao.getWeeklyWorkoutDuration(1)
        .collectAsStateWithLifecycle(initialValue = 0)
    val averageDailyCalories by mealPlanDao.getAverageDailyCalories(1)
        .collectAsStateWithLifecycle(initialValue = 0f)
    val dailyCaloriesList by mealPlanDao.getDailyCaloriesForWeek(1)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Convert List<DailyCalories> to Map<String, Int>
    val dailyCaloriesForWeek = dailyCaloriesList.associate { it.date to it.calories }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Overview Section
            item {
                WeeklyOverviewCard(
                    caloriesBurned = userProfile?.caloriesBurned ?: 0,
                    weeklyCalories = weeklyCalories ?: 0
                )
            }

            // Activity Stats
            item {
                ActivityProgressCard(
                    steps = userProfile?.dailySteps ?: 0,
                    targetSteps = 10000,
                    calories = userProfile?.caloriesBurned ?: 0,
                    targetCalories = 500
                )
            }

            // Heart Rate Chart
            item {
                HeartRateCard(
                    currentBPM = userProfile?.heartRate ?: 0,
                    heartRateHistory = listOf(75, 72, 78, 70, 76, 74, 71)
                )
            }

            // Weekly Activity Chart with actual data
            item {
                WeeklyActivityCard(
                    weeklyWorkouts = weeklyWorkouts,
                    weeklyDuration = weeklyDuration ?: 0,
                    averageCalories = averageDailyCalories?.toInt() ?: 0,
                    dailyCalories = dailyCaloriesForWeek
                )
            }

            // Achievement Section
            item {
                AchievementsCard(
                    weeklyWorkouts = weeklyWorkouts,
                    dailySteps = userProfile?.dailySteps ?: 0,
                    weeklyCaloriesBurned = userProfile?.caloriesBurned?.times(7) ?: 0
                )
            }
        }
    }
}

@Composable
private fun WeeklyOverviewCard(caloriesBurned: Int, weeklyCalories: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    value = caloriesBurned,
                    label = "Calories Burned",
                    icon = Icons.Default.LocalFireDepartment
                )
                StatColumn(
                    value = weeklyCalories,
                    label = "Weekly Goal",
                    icon = Icons.Default.Flag
                )
            }
        }
    }
}

@Composable
private fun ActivityProgressCard(
    steps: Int,
    targetSteps: Int,
    calories: Int,
    targetCalories: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ProgressBar(
                current = steps,
                target = targetSteps,
                label = "Steps",
                icon = Icons.Default.DirectionsWalk
            )

            ProgressBar(
                current = calories,
                target = targetCalories,
                label = "Calories",
                icon = Icons.Default.LocalFireDepartment
            )
        }
    }
}

@Composable
private fun HeartRateCard(
    currentBPM: Int,
    heartRateHistory: List<Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heart Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$currentBPM BPM",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heart Rate Chart
            HeartRateChart(heartRateHistory)
        }
    }
}

@Composable
private fun HeartRateChart(heartRateHistory: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val path = Path()
        val points = heartRateHistory.mapIndexed { index, value ->
            Offset(
                x = size.width * (index.toFloat() / (heartRateHistory.size - 1)),
                y = size.height * (1 - (value - 60f) / 40f)
            )
        }

        // Draw the line
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = path,
                color = ChartBlue,
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
private fun WeeklyActivityCard(
    weeklyWorkouts: Int,
    weeklyDuration: Int,
    averageCalories: Int,
    dailyCalories: Map<String, Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeeklyStatItem(
                    value = weeklyWorkouts,
                    label = "Workouts",
                    icon = Icons.Default.FitnessCenter
                )
                WeeklyStatItem(
                    value = weeklyDuration,
                    label = "Minutes",
                    icon = Icons.Default.Timer
                )
                WeeklyStatItem(
                    value = averageCalories,
                    label = "Avg. Cal",
                    icon = Icons.Default.LocalFireDepartment
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Calories Chart
            if (dailyCalories.isNotEmpty()) {
                WeeklyCaloriesChart(dailyCalories)
            }
        }
    }
}

@Composable
private fun WeeklyCaloriesChart(dailyCalories: Map<String, Int>) {
    val maxCalories = dailyCalories.values.maxOrNull()?.toFloat() ?: 1f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
    ) {
        val barWidth = size.width / 8 // Space for 7 bars with padding
        val gap = barWidth * 0.2f

        dailyCalories.values.forEachIndexed { index, calories ->
            val height = (calories / maxCalories) * size.height
            val x = index * barWidth + gap

            drawRect(
                color = ChartBlue,
                topLeft = Offset(x, size.height - height),
                size = androidx.compose.ui.geometry.Size(
                    barWidth - (gap * 2),
                    height
                )
            )
        }
    }
}

@Composable
private fun WeeklyStatItem(
    value: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AchievementsCard(
    weeklyWorkouts: Int,
    dailySteps: Int,
    weeklyCaloriesBurned: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic achievements based on actual progress
            AchievementItem(
                title = "Step Master",
                description = "Walked 10,000 steps in a day",
                icon = Icons.Default.DirectionsWalk,
                achieved = dailySteps >= 10000
            )
            AchievementItem(
                title = "Workout Warrior",
                description = "Completed 5 workouts this week",
                icon = Icons.Default.FitnessCenter,
                achieved = weeklyWorkouts >= 5
            )
            AchievementItem(
                title = "Calorie Crusher",
                description = "Burned 3500 calories this week",
                icon = Icons.Default.LocalFireDepartment,
                achieved = weeklyCaloriesBurned >= 3500
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ProgressBar(
    current: Int,
    target: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = label)
                Text(text = label)
            }
            Text(text = "$current/$target")
        }
        LinearProgressIndicator(
            progress = (current.toFloat() / target).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}

@Composable
private fun AchievementItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    achieved: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (achieved) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


// Preview functions
@Preview(showBackground = true)
@Composable
private fun ProgressScreenPreview() {
    MaterialTheme {
        ProgressScreenContent(
            userProfile = UserProfile(
                id = 1,
                name = "John Doe",
                email = "john.doe@example.com",
                gender = "Male",
                age = 30,
                height = 175,
                weight = 70.0f,
                currentPlan = "Weight Loss Plan",
                dailySteps = 8500,
                heartRate = 72,
                caloriesBurned = 450
            ),
            weeklyCalories = 2800,
            weeklyWorkouts = 4,
            weeklyDuration = 240,
            averageDailyCalories = 400f,
            dailyCaloriesForWeek = mapOf(
                "Mon" to 350,
                "Tue" to 420,
                "Wed" to 380,
                "Thu" to 450,
                "Fri" to 400,
                "Sat" to 500,
                "Sun" to 300
            )
        )
    }
}

@Composable
private fun ProgressScreenContent(
    userProfile: UserProfile?,
    weeklyCalories: Int,
    weeklyWorkouts: Int,
    weeklyDuration: Int,
    averageDailyCalories: Float,
    dailyCaloriesForWeek: Map<String, Int>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weekly Overview Section
        item {
            WeeklyOverviewCard(
                caloriesBurned = userProfile?.caloriesBurned ?: 0,
                weeklyCalories = weeklyCalories
            )
        }

        // Activity Stats
        item {
            ActivityProgressCard(
                steps = userProfile?.dailySteps ?: 0,
                targetSteps = 10000,
                calories = userProfile?.caloriesBurned ?: 0,
                targetCalories = 500
            )
        }

        // Heart Rate Chart
        item {
            HeartRateCard(
                currentBPM = userProfile?.heartRate ?: 0,
                heartRateHistory = listOf(75, 72, 78, 70, 76, 74, 71)
            )
        }

        // Weekly Activity Chart with actual data
        item {
            WeeklyActivityCard(
                weeklyWorkouts = weeklyWorkouts,
                weeklyDuration = weeklyDuration,
                averageCalories = averageDailyCalories.toInt(),
                dailyCalories = dailyCaloriesForWeek
            )
        }

        // Achievement Section
        item {
            AchievementsCard(
                weeklyWorkouts = weeklyWorkouts,
                dailySteps = userProfile?.dailySteps ?: 0,
                weeklyCaloriesBurned = userProfile?.caloriesBurned?.times(7) ?: 0
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyOverviewCardPreview() {
    MaterialTheme {
        WeeklyOverviewCard(
            caloriesBurned = 450,
            weeklyCalories = 2800
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityProgressCardPreview() {
    MaterialTheme {
        ActivityProgressCard(
            steps = 8500,
            targetSteps = 10000,
            calories = 450,
            targetCalories = 500
        )
    }
}
