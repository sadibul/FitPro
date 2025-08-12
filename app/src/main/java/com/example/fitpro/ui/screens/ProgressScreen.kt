package com.example.fitpro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.*
import com.example.fitpro.ui.theme.ChartBlue
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    workoutPlanDao: WorkoutPlanDao,
    mealPlanDao: MealPlanDao,
    completedWorkoutDao: CompletedWorkoutDao
) {
    val context = LocalContext.current
    val userSession = remember { UserSession(context) }
    val currentUserEmail = userSession.getCurrentUserEmail()
    
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    
    val weeklyWorkouts by (currentUserEmail?.let {
        workoutPlanDao.getWeeklyWorkoutCount(it).collectAsStateWithLifecycle(initialValue = 0)
    } ?: flowOf(0).collectAsStateWithLifecycle(initialValue = 0))
    
    // Get completed workouts for chart data
    val completedWorkouts by (currentUserEmail?.let {
        completedWorkoutDao.getCompletedWorkouts(it).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: flowOf(emptyList<CompletedWorkout>()).collectAsStateWithLifecycle(initialValue = emptyList()))
    
    // Get meal plans for calorie consumption data
    val allMealPlans by (currentUserEmail?.let {
        mealPlanDao.getAllMealPlans(it).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: flowOf(emptyList<MealPlan>()).collectAsStateWithLifecycle(initialValue = emptyList()))

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
            // Today's Progress Section
            item {
                ActivityProgressCard(
                    steps = userProfile?.dailySteps ?: 0,
                    targetSteps = userProfile?.stepTarget ?: 10000,
                    calories = userProfile?.caloriesBurned ?: 0,
                    targetCalories = userProfile?.calorieTarget ?: 500
                )
            }

            // Weekly Statistics Chart
            item {
                WeeklyStatsChart(
                    completedWorkouts = completedWorkouts,
                    allMealPlans = allMealPlans,
                    userProfile = userProfile
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
                email = "john.doe@example.com",
                name = "John Doe",
                password = "password123",
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

@Composable
private fun WeeklyStatsChart(
    completedWorkouts: List<CompletedWorkout>,
    allMealPlans: List<MealPlan>,
    userProfile: UserProfile?
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
            // Tab selector
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Calories Burn", "Calories Consume", "Workouts")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTab == index) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = tab,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTab == index) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            // Generate weekly data
            val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
            }
            
            // Chart content
            when (selectedTab) {
                0 -> WeeklyBarChart(
                    data = weeklyData.map { it.caloriesBurned },
                    title = "${(weeklyData.sumOf { it.caloriesBurned.toDouble() } / 7).toInt()} calories on average",
                    subtitle = "this week",
                    maxValue = weeklyData.maxOfOrNull { it.caloriesBurned } ?: 1000f,
                    color = MaterialTheme.colorScheme.error
                )
                1 -> WeeklyBarChart(
                    data = weeklyData.map { it.caloriesConsumed },
                    title = "${(weeklyData.sumOf { it.caloriesConsumed.toDouble() } / 7).toInt()} calories on average",
                    subtitle = "this week",
                    maxValue = weeklyData.maxOfOrNull { it.caloriesConsumed } ?: 2000f,
                    color = MaterialTheme.colorScheme.secondary
                )
                2 -> WeeklyBarChart(
                    data = weeklyData.map { it.workouts.toFloat() },
                    title = "${weeklyData.sumOf { it.workouts }} workouts",
                    subtitle = "this week",
                    maxValue = weeklyData.maxOfOrNull { it.workouts.toFloat() } ?: 3f,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Float>,
    title: String,
    subtitle: String,
    maxValue: Float,
    color: Color
) {
    Column {
        // Title and subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Dropdown",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bar chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val days = listOf("S", "M", "T", "W", "T", "F", "S")
            
            data.forEachIndexed { index, value ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Bar
                    val height = if (maxValue > 0) (value / maxValue * 80).dp else 4.dp
                    Card(
                        modifier = Modifier
                            .width(24.dp)
                            .height(maxOf(height, 4.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = color.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    ) {}
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Day label
                    Text(
                        text = days.getOrNull(index) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Data class for weekly statistics
data class DayData(
    val caloriesBurned: Float,
    val caloriesConsumed: Float,
    val workouts: Int
)

// Function to generate weekly data
private fun generateWeeklyData(
    completedWorkouts: List<CompletedWorkout>,
    allMealPlans: List<MealPlan>,
    userProfile: UserProfile?
): List<DayData> {
    val calendar = java.util.Calendar.getInstance()
    
    // Get start of current week (Sunday)
    calendar.firstDayOfWeek = java.util.Calendar.SUNDAY
    calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    
    val weeklyData = mutableListOf<DayData>()
    
    // Generate data for each day of the week
    for (dayIndex in 0..6) {
        val dayStartTime = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayEndTime = calendar.timeInMillis
        
        // Count workouts for this day
        val dayWorkouts = completedWorkouts.count { workout ->
            workout.completedAt >= dayStartTime && workout.completedAt < dayEndTime
        }
        
        // Calculate calories burned from workouts for this day
        val dayCaloriesBurned = completedWorkouts
            .filter { workout ->
                workout.completedAt >= dayStartTime && workout.completedAt < dayEndTime
            }
            .sumOf { (it.targetCalories ?: 0).toLong() }
        
        // For meal plans, since createdAt is a String, we'll use a simpler approach
        // and distribute meal plans across the week or use completed status
        val dayCaloriesConsumed = if (dayIndex < allMealPlans.size) {
            allMealPlans.getOrNull(dayIndex)?.let { if (it.isCompleted) it.totalCalories else 0 } ?: 0
        } else {
            0
        }
        
        // Add some sample data if no real data exists for better visualization
        val sampleCaloriesBurned = if (dayCaloriesBurned == 0L && completedWorkouts.isEmpty()) {
            when (dayIndex) {
                0 -> 300f // Sunday
                1 -> 150f // Monday
                2 -> 450f // Tuesday
                3 -> 200f // Wednesday
                4 -> 100f // Thursday
                5 -> 380f // Friday
                6 -> 250f // Saturday
                else -> 0f
            }
        } else dayCaloriesBurned.toFloat()
        
        val sampleCaloriesConsumed = if (dayCaloriesConsumed == 0 && allMealPlans.isEmpty()) {
            when (dayIndex) {
                0 -> 1800f // Sunday
                1 -> 1600f // Monday
                2 -> 2100f // Tuesday
                3 -> 1900f // Wednesday
                4 -> 1500f // Thursday
                5 -> 2200f // Friday
                6 -> 1750f // Saturday
                else -> 0f
            }
        } else dayCaloriesConsumed.toFloat()
        
        val sampleWorkouts = if (dayWorkouts == 0 && completedWorkouts.isEmpty()) {
            when (dayIndex) {
                0 -> 0 // Sunday - rest day
                1 -> 1 // Monday
                2 -> 2 // Tuesday
                3 -> 1 // Wednesday
                4 -> 0 // Thursday - rest day
                5 -> 2 // Friday
                6 -> 1 // Saturday
                else -> 0
            }
        } else dayWorkouts
        
        weeklyData.add(
            DayData(
                caloriesBurned = sampleCaloriesBurned,
                caloriesConsumed = sampleCaloriesConsumed,
                workouts = sampleWorkouts
            )
        )
        
        // Reset calendar for next iteration
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    
    return weeklyData
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
