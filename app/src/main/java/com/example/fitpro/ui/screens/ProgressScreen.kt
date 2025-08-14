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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.*
import com.example.fitpro.ui.theme.ChartBlue
import com.example.fitpro.utils.UserSession
import com.example.fitpro.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
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

            // Steps Chart
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Steps",
                    data = weeklyData.map { it.steps },
                    maxValue = 15000f,
                    yAxisLabels = listOf("0", "5K", "10K", "15K"),
                    color = MaterialTheme.colorScheme.primary,
                    selectedPeriod = "Weekly"
                )
            }

            // Calories Burn Chart
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Calories Burn",
                    data = weeklyData.map { it.caloriesBurned },
                    maxValue = 3000f,
                    yAxisLabels = listOf("0", "1K", "2K", "3K"),
                    color = MaterialTheme.colorScheme.error,
                    selectedPeriod = "Weekly"
                )
            }

            // Calories Consume Chart
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Calories Consume",
                    data = weeklyData.map { it.caloriesConsumed },
                    maxValue = 3000f,
                    yAxisLabels = listOf("0", "1K", "2K", "3K"),
                    color = MaterialTheme.colorScheme.secondary,
                    selectedPeriod = "Weekly"
                )
            }

            // Workout Chart (in minutes)
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Workout",
                    data = weeklyData.map { it.workouts.toFloat() * 30 }, // Convert to minutes (assuming 30 min per workout)
                    maxValue = 200f,
                    yAxisLabels = listOf("0", "50", "100", "150", "200"),
                    color = MaterialTheme.colorScheme.tertiary,
                    selectedPeriod = "Weekly"
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
private fun SandowScoreChart(
    title: String,
    data: List<Float>,
    maxValue: Float,
    yAxisLabels: List<String>,
    color: Color,
    selectedPeriod: String
) {
    var showDropdown by remember { mutableStateOf(false) }
    val periods = listOf("Daily", "Weekly", "Monthly")
    var currentPeriod by remember { mutableStateOf(selectedPeriod) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with title and dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                // Dropdown selector
                Box {
                    Card(
                        modifier = Modifier.clickable { showDropdown = true },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentPeriod,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        periods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period) },
                                onClick = {
                                    currentPeriod = period
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Chart with Y-axis labels and bars
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier.width(40.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    yAxisLabels.reversed().forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Chart area
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Grid lines and bars
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        // Grid lines
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val gridColor = Color.LightGray.copy(alpha = 0.3f)
                            val gridLines = yAxisLabels.size
                            
                            for (i in 0 until gridLines) {
                                val y = size.height * (i.toFloat() / (gridLines - 1))
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                        
                        // Bars
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val maxDataValue = data.maxOrNull() ?: 1f
                            val currentMaxValue = maxOf(maxDataValue, maxValue * 0.3f) // Ensure some minimum scale
                            
                            data.forEachIndexed { index, value ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Highlight bar (Tuesday is highlighted in your image)
                                    val isHighlighted = index == 1 // Tuesday
                                    val barHeight = (value / currentMaxValue * 160).dp
                                    val barColor = if (isHighlighted) Color.Black else Color.LightGray
                                    
                                    // Value label on highlighted bar
                                    if (isHighlighted && value > 0) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = value.toInt().toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    
                                    // Bar
                                    Card(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(maxOf(barHeight, 8.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = barColor
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 2.dp,
                                            bottomEnd = 2.dp
                                        )
                                    ) {}
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Day label
                                    Text(
                                        text = days.getOrNull(index) ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class for weekly statistics
data class DayData(
    val steps: Float,
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
    val bangladeshCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
    
    // Get current date in Bangladesh timezone
    val currentDate = bangladeshCalendar.time
    val currentDayOfWeek = bangladeshCalendar.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
    
    // Calculate start of current week (Sunday)
    val daysFromSunday = when (currentDayOfWeek) {
        java.util.Calendar.SUNDAY -> 0
        java.util.Calendar.MONDAY -> 1
        java.util.Calendar.TUESDAY -> 2
        java.util.Calendar.WEDNESDAY -> 3
        java.util.Calendar.THURSDAY -> 4
        java.util.Calendar.FRIDAY -> 5
        java.util.Calendar.SATURDAY -> 6
        else -> 0
    }
    
    // Set to start of current week (Sunday)
    val weekStartCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
    weekStartCalendar.add(java.util.Calendar.DAY_OF_YEAR, -daysFromSunday)
    weekStartCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    weekStartCalendar.set(java.util.Calendar.MINUTE, 0)
    weekStartCalendar.set(java.util.Calendar.SECOND, 0)
    weekStartCalendar.set(java.util.Calendar.MILLISECOND, 0)
    
    // Generate the 7 days of current week
    val weekDates = mutableListOf<String>()
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    dateFormat.timeZone = TimeUtils.getBangladeshTimeZone()
    
    for (dayIndex in 0..6) {
        val dayDate = dateFormat.format(Date(weekStartCalendar.timeInMillis))
        weekDates.add(dayDate)
        weekStartCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    
    // Group workouts by Bangladesh date
    val workoutsByDate = completedWorkouts.groupBy { workout ->
        TimeUtils.convertToBangladeshDay(workout.completedAt)
    }
    
    // Group meal plans by Bangladesh date (only completed ones)
    val mealPlansByDate = allMealPlans
        .filter { it.isCompleted }
        .groupBy { it.createdAt } // createdAt is already in Bangladesh date format
    
    val weeklyData = mutableListOf<DayData>()
    
    // Generate data for each day of the week
    for (dayIndex in 0..6) {
        val dayDate = weekDates[dayIndex]
        
        // Count workouts for this day
        val dayWorkouts = workoutsByDate[dayDate]?.size ?: 0
        
        // Calculate calories burned from workouts for this day
        val dayCaloriesBurned = workoutsByDate[dayDate]
            ?.sumOf { (it.targetCalories ?: 0).toLong() } ?: 0L
        
        // Calculate calories consumed from meal plans for this day
        val dayCaloriesConsumed = mealPlansByDate[dayDate]
            ?.sumOf { it.totalCalories } ?: 0
        
        // Calculate actual steps for this day
        // For current day, use the dailySteps from userProfile
        // For other days, we don't have historical data yet, so show 0
        val actualSteps = if (dayIndex == (currentDayOfWeek - 1)) { // currentDayOfWeek is 1-based, dayIndex is 0-based
            (userProfile?.dailySteps ?: 0).toFloat()
        } else {
            0f // No historical step data available for other days
        }
        
        weeklyData.add(
            DayData(
                steps = actualSteps,
                caloriesBurned = dayCaloriesBurned.toFloat(),
                caloriesConsumed = dayCaloriesConsumed.toFloat(),
                workouts = dayWorkouts
            )
        )
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
