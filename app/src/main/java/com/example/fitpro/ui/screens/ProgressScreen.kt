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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    completedWorkoutDao: CompletedWorkoutDao,
    completedStepTargetDao: CompletedStepTargetDao
) {
    val context = LocalContext.current
    val userSession = remember { UserSession(context) }
    val currentUserEmail = userSession.getCurrentUserEmail()
    
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    
    // Get completed workouts for chart data
    val completedWorkouts by (currentUserEmail?.let {
        completedWorkoutDao.getCompletedWorkouts(it).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: flowOf(emptyList<CompletedWorkout>()).collectAsStateWithLifecycle(initialValue = emptyList()))
    
    // Get completed step targets for chart data
    val completedStepTargets by (currentUserEmail?.let {
        completedStepTargetDao.getCompletedStepTargets(it).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: flowOf(emptyList<CompletedStepTarget>()).collectAsStateWithLifecycle(initialValue = emptyList()))
    
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
            // Steps Chart
            item {
                val weeklyData = remember(completedStepTargets, allMealPlans, userProfile) {
                    generateWeeklyStepsData(completedStepTargets, userProfile)
                }
                val yearlyData = remember(completedStepTargets, allMealPlans, userProfile) {
                    generateYearlyStepsData(completedStepTargets, userProfile)
                }
                SandowScoreChart(
                    title = "Steps",
                    weeklyData = weeklyData,
                    yearlyData = yearlyData,
                    maxValue = 15000f,
                    yAxisLabels = listOf("0", "5K", "10K", "15K"),
                    color = MaterialTheme.colorScheme.primary,
                    selectedPeriod = "Weekly",
                    dataSelector = { it.steps }
                )
            }

            // Calories Burn Chart
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                val yearlyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateYearlyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Calories Burn",
                    weeklyData = weeklyData,
                    yearlyData = yearlyData,
                    maxValue = 3000f,
                    yAxisLabels = listOf("0", "1K", "2K", "3K"),
                    color = MaterialTheme.colorScheme.error,
                    selectedPeriod = "Weekly",
                    dataSelector = { it.caloriesBurned }
                )
            }

            // Calories Consume Chart
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                val yearlyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateYearlyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Calories Consume",
                    weeklyData = weeklyData,
                    yearlyData = yearlyData,
                    maxValue = 3000f,
                    yAxisLabels = listOf("0", "1K", "2K", "3K"),
                    color = MaterialTheme.colorScheme.secondary,
                    selectedPeriod = "Weekly",
                    dataSelector = { it.caloriesConsumed }
                )
            }

            // Workout Chart (in minutes)
            item {
                val weeklyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateWeeklyData(completedWorkouts, allMealPlans, userProfile)
                }
                val yearlyData = remember(completedWorkouts, allMealPlans, userProfile) {
                    generateYearlyData(completedWorkouts, allMealPlans, userProfile)
                }
                SandowScoreChart(
                    title = "Workout",
                    weeklyData = weeklyData,
                    yearlyData = yearlyData, // No need to multiply, it's already in minutes
                    maxValue = 200f,
                    yAxisLabels = listOf("0", "50", "100", "150", "200"),
                    color = MaterialTheme.colorScheme.tertiary,
                    selectedPeriod = "Weekly",
                    dataSelector = { it.workouts.toFloat() }
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
    weeklyData: List<DayData>,
    yearlyData: List<DayData>,
    maxValue: Float,
    yAxisLabels: List<String>,
    color: Color,
    selectedPeriod: String,
    dataSelector: (DayData) -> Float
) {
    var showDropdown by remember { mutableStateOf(false) }
    val periods = listOf("Weekly", "Yearly")
    var currentPeriod by remember { mutableStateOf(selectedPeriod) }
    
    // Choose data based on current period
    val data = if (currentPeriod == "Weekly") {
        weeklyData.map(dataSelector)
    } else {
        yearlyData.map(dataSelector)
    }
    
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
                // Y-axis labels (only show for Weekly view)
                if (currentPeriod == "Weekly") {
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .height(200.dp)
                            .padding(vertical = 8.dp), // Match chart padding
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        yAxisLabels.reversed().forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.wrapContentHeight()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
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
                        // Grid lines (only for Weekly view)
                        if (currentPeriod == "Weekly") {
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val gridColor = Color.LightGray.copy(alpha = 0.3f)
                                val gridLines = yAxisLabels.size
                                val chartHeight = size.height - 16.dp.toPx() // Account for vertical padding
                                val chartTop = 8.dp.toPx() // Top padding
                                
                                // Draw horizontal grid lines aligned with Y-axis labels
                                for (i in 0 until gridLines) {
                                    // Calculate Y position from bottom to top
                                    val y = chartTop + chartHeight - (i.toFloat() / (gridLines - 1) * chartHeight)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
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
                            // Different labels for Weekly vs Yearly
                            val xAxisLabels = if (currentPeriod == "Weekly") {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            } else {
                                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            }
                            
                            // Use the maxValue parameter which represents the top Y-axis value for Weekly
                            // For Yearly, use the max value from data for natural scaling
                            val chartMaxValue = if (currentPeriod == "Weekly") {
                                maxValue
                            } else {
                                val maxDataValue = data.maxOrNull() ?: 1f
                                maxOf(maxDataValue, 1f) // Ensure minimum scale
                            }
                            val chartHeight = 184f // Available height for bars (200dp - 16dp padding)
                            
                            val displayData = if (currentPeriod == "Weekly") {
                                data.take(7) // Show 7 days for weekly
                            } else {
                                // Generate yearly data (12 months) - for now use sample data or extend data
                                val yearlyData = data.take(12).toMutableList()
                                while (yearlyData.size < 12) {
                                    yearlyData.add(0f)
                                }
                                yearlyData
                            }
                            
                            displayData.forEachIndexed { index, value ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Highlight bar (current day for Weekly, current month for Yearly)
                                    val isHighlighted = if (currentPeriod == "Weekly") {
                                        // Calculate current day index dynamically
                                        val bangladeshCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
                                        val currentDayOfWeek = bangladeshCalendar.get(java.util.Calendar.DAY_OF_WEEK)
                                        val currentDayIndex = when (currentDayOfWeek) {
                                            java.util.Calendar.SUNDAY -> 6 // Sunday is last in our Mon-Sun order
                                            java.util.Calendar.MONDAY -> 0
                                            java.util.Calendar.TUESDAY -> 1
                                            java.util.Calendar.WEDNESDAY -> 2
                                            java.util.Calendar.THURSDAY -> 3
                                            java.util.Calendar.FRIDAY -> 4
                                            java.util.Calendar.SATURDAY -> 5
                                            else -> 0
                                        }
                                        index == currentDayIndex
                                    } else {
                                        index == 7 // August (current month)
                                    }
                                    
                                    // Calculate bar height based on ratio to maxValue
                                    val barHeightRatio = if (chartMaxValue > 0) (value / chartMaxValue) else 0f
                                    val barHeight = (barHeightRatio * chartHeight).dp
                                    val barColor = if (isHighlighted) Color.Black else Color.LightGray
                                    
                                    // Value label on highlighted bar
                                    if (isHighlighted && value > 0) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .widthIn(min = 32.dp) // Ensure minimum width for larger numbers
                                        ) {
                                            Text(
                                                text = value.toInt().toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp, // Explicit font size
                                                modifier = Modifier.padding(
                                                    horizontal = if (currentPeriod == "Weekly") 8.dp else 6.dp,
                                                    vertical = 4.dp
                                                ),
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Visible, // Allow text to be fully visible
                                                softWrap = false, // Keep on single line
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    
                                    // Bar
                                    Card(
                                        modifier = Modifier
                                            .width(if (currentPeriod == "Weekly") 20.dp else 16.dp)
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
                                    
                                    // X-axis label
                                    Text(
                                        text = xAxisLabels.getOrNull(index) ?: "",
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
    val workouts: Int // workout duration in minutes
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
        
        // Calculate total workout duration for this day (in minutes)
        val dayWorkoutDuration = workoutsByDate[dayDate]
            ?.sumOf { it.actualDuration } ?: 0
        
        // Calculate calories burned from workouts for this day
        val dayCaloriesBurned = workoutsByDate[dayDate]
            ?.sumOf { (it.targetCalories ?: 0).toLong() } ?: 0L
        
        // Calculate calories consumed from meal plans for this day
        val dayCaloriesConsumed = mealPlansByDate[dayDate]
            ?.sumOf { it.totalCalories } ?: 0
        
        // Calculate actual steps for this day
        // For current day, use the dailySteps from userProfile
        // For other days, we don't have historical data yet, so show 0
        // Map Calendar day of week to our array index (0=Sunday, 1=Monday, etc.)
        val currentDayIndex = when (currentDayOfWeek) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 0
        }
        
        val actualSteps = if (dayIndex == currentDayIndex) {
            (userProfile?.dailySteps ?: 0).toFloat()
        } else {
            0f // No historical step data available for other days
        }
        
        // Add current user's calories burned for today
        val finalCaloriesBurned = if (dayIndex == currentDayIndex && userProfile != null) {
            dayCaloriesBurned + userProfile.caloriesBurned
        } else {
            dayCaloriesBurned
        }
        
        weeklyData.add(
            DayData(
                steps = actualSteps,
                caloriesBurned = finalCaloriesBurned.toFloat(),
                caloriesConsumed = dayCaloriesConsumed.toFloat(),
                workouts = dayWorkoutDuration // Use duration instead of count
            )
        )
    }
    
    // Reorder data to match chart's Monday-Sunday order
    // weeklyData is currently in Sunday-Saturday order (0=Sun, 1=Mon, ..., 6=Sat)
    // Chart expects Monday-Sunday order (0=Mon, 1=Tue, ..., 6=Sun)
    // So we need to move Sunday (index 0) to the end
    val reorderedData = mutableListOf<DayData>()
    
    // Add Monday through Saturday (indices 1-6 from original data)
    for (i in 1..6) {
        reorderedData.add(weeklyData[i])
    }
    // Add Sunday (index 0 from original data) to the end
    reorderedData.add(weeklyData[0])
    
    // Update current day index for chart highlighting
    // Convert from Calendar day to chart index (Monday=0, Tuesday=1, ..., Sunday=6)
    val chartCurrentDayIndex = when (currentDayOfWeek) {
        java.util.Calendar.MONDAY -> 0    // Monday = chart index 0
        java.util.Calendar.TUESDAY -> 1   // Tuesday = chart index 1
        java.util.Calendar.WEDNESDAY -> 2 // Wednesday = chart index 2
        java.util.Calendar.THURSDAY -> 3  // Thursday = chart index 3
        java.util.Calendar.FRIDAY -> 4    // Friday = chart index 4
        java.util.Calendar.SATURDAY -> 5  // Saturday = chart index 5
        java.util.Calendar.SUNDAY -> 6    // Sunday = chart index 6
        else -> 0
    }
    
    // Debug logging
    val chartDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    android.util.Log.d("WeeklyData", "Current day: ${chartDayLabels[chartCurrentDayIndex]}, chart index: $chartCurrentDayIndex")
    android.util.Log.d("WeeklyData", "Generated ${reorderedData.size} days of data (reordered for Mon-Sun chart)")
    android.util.Log.d("WeeklyData", "Reordered weekly calories data: ${reorderedData.map { it.caloriesBurned }}")
    
    return reorderedData
}

// Function to generate yearly data (12 months)
private fun generateYearlyData(
    completedWorkouts: List<CompletedWorkout>,
    allMealPlans: List<MealPlan>,
    userProfile: UserProfile?
): List<DayData> {
    val bangladeshCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
    
    // Get current year
    val currentYear = bangladeshCalendar.get(java.util.Calendar.YEAR)
    val currentMonth = bangladeshCalendar.get(java.util.Calendar.MONTH) // 0-based (0 = January)
    
    val yearlyData = mutableListOf<DayData>()
    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    dateFormat.timeZone = TimeUtils.getBangladeshTimeZone()
    
    // Generate data for each month of the current year
    for (monthIndex in 0..11) { // 0 = January, 11 = December
        // Set to first day of this month
        val monthCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
        monthCalendar.set(currentYear, monthIndex, 1, 0, 0, 0)
        monthCalendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val monthString = dateFormat.format(monthCalendar.time)
        
        // Group workouts by month
        val monthWorkouts = completedWorkouts.filter { workout ->
            val workoutDate = TimeUtils.convertToBangladeshDay(workout.completedAt)
            workoutDate.startsWith(monthString)
        }
        
        // Group meal plans by month
        val monthMealPlans = allMealPlans.filter { mealPlan ->
            mealPlan.isCompleted && mealPlan.createdAt.startsWith(monthString)
        }
        
        // Calculate totals for this month
        val monthWorkoutCount = monthWorkouts.size
        val monthWorkoutDuration = monthWorkouts.sumOf { it.actualDuration } // Total duration in minutes
        val monthCaloriesBurned = monthWorkouts.sumOf { (it.targetCalories ?: 0).toLong() }
        val monthCaloriesConsumed = monthMealPlans.sumOf { it.totalCalories }
        
        // For steps, only current month uses actual data, others are 0
        val monthSteps = if (monthIndex == currentMonth) {
            (userProfile?.dailySteps ?: 0).toFloat() * bangladeshCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        } else {
            0f
        }
        
        yearlyData.add(
            DayData(
                steps = monthSteps,
                caloriesBurned = monthCaloriesBurned.toFloat(),
                caloriesConsumed = monthCaloriesConsumed.toFloat(),
                workouts = monthWorkoutDuration // Use duration instead of count
            )
        )
    }
    
    return yearlyData
}

private fun generateWeeklyStepsData(
    completedStepTargets: List<CompletedStepTarget>,
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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    dateFormat.timeZone = TimeUtils.getBangladeshTimeZone()
    
    for (dayIndex in 0..6) {
        val dayDate = dateFormat.format(Date(weekStartCalendar.timeInMillis))
        weekDates.add(dayDate)
        weekStartCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    
    // Group completed step targets by Bangladesh date
    val stepTargetsByDate = completedStepTargets.groupBy { target ->
        TimeUtils.convertToBangladeshDay(target.completedAt)
    }
    
    val weeklyData = mutableListOf<DayData>()
    
    // Generate data for each day of the week
    for (dayIndex in 0..6) {
        val dayDate = weekDates[dayIndex]
        
        // Sum all completed step targets for this day
        val completedSteps = stepTargetsByDate[dayDate]
            ?.sumOf { it.actualSteps } ?: 0
        
        // Map Calendar day of week to our array index (0=Sunday, 1=Monday, etc.)
        val currentDayIndex = when (currentDayOfWeek) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 0
        }
        
        // Add current user's daily steps if this is today
        val finalSteps = if (dayIndex == currentDayIndex && userProfile != null) {
            completedSteps + userProfile.dailySteps
        } else {
            completedSteps
        }
        
        weeklyData.add(
            DayData(
                steps = finalSteps.toFloat(),
                caloriesBurned = 0f, // Not used for step chart
                caloriesConsumed = 0f, // Not used for step chart
                workouts = 0 // Not used for step chart
            )
        )
    }
    
    // Reorder data to match chart's Monday-Sunday order
    // weeklyData is currently in Sunday-Saturday order (0=Sun, 1=Mon, ..., 6=Sat)
    // Chart expects Monday-Sunday order (0=Mon, 1=Tue, ..., 6=Sun)
    // So we need to move Sunday (index 0) to the end
    val reorderedData = mutableListOf<DayData>()
    
    // Add Monday through Saturday (indices 1-6 from original data)
    for (i in 1..6) {
        reorderedData.add(weeklyData[i])
    }
    // Add Sunday (index 0 from original data) to the end
    reorderedData.add(weeklyData[0])
    
    // Update current day index for chart highlighting
    // Convert from Calendar day to chart index (Monday=0, Tuesday=1, ..., Sunday=6)
    val chartCurrentDayIndex = when (currentDayOfWeek) {
        java.util.Calendar.MONDAY -> 0    // Monday = chart index 0
        java.util.Calendar.TUESDAY -> 1   // Tuesday = chart index 1
        java.util.Calendar.WEDNESDAY -> 2 // Wednesday = chart index 2
        java.util.Calendar.THURSDAY -> 3  // Thursday = chart index 3
        java.util.Calendar.FRIDAY -> 4    // Friday = chart index 4
        java.util.Calendar.SATURDAY -> 5  // Saturday = chart index 5
        java.util.Calendar.SUNDAY -> 6    // Sunday = chart index 6
        else -> 0
    }
    
    // Debug logging
    val chartDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    android.util.Log.d("WeeklyStepsData", "Current day: ${chartDayLabels[chartCurrentDayIndex]}, chart index: $chartCurrentDayIndex")
    android.util.Log.d("WeeklyStepsData", "Generated ${reorderedData.size} days of step data (reordered for Mon-Sun chart)")
    android.util.Log.d("WeeklyStepsData", "Reordered weekly steps data: ${reorderedData.map { it.steps }}")
    
    return reorderedData
}

private fun generateYearlyStepsData(
    completedStepTargets: List<CompletedStepTarget>,
    userProfile: UserProfile?
): List<DayData> {
    val bangladeshCalendar = java.util.Calendar.getInstance(TimeUtils.getBangladeshTimeZone())
    
    // Get current year
    val currentYear = bangladeshCalendar.get(java.util.Calendar.YEAR)
    val currentMonth = bangladeshCalendar.get(java.util.Calendar.MONTH) // 0-based (0 = January)
    
    val yearlyData = mutableListOf<DayData>()
    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    dateFormat.timeZone = TimeUtils.getBangladeshTimeZone()
    
    // Group completed step targets by month
    val stepTargetsByMonth = completedStepTargets.groupBy { target ->
        val targetDate = Date(target.completedAt)
        dateFormat.format(targetDate) // Format as "yyyy-MM"
    }
    
    // Generate 12 months of data
    for (monthIndex in 0..11) {
        val monthCalendar = bangladeshCalendar.clone() as java.util.Calendar
        monthCalendar.set(java.util.Calendar.YEAR, currentYear)
        monthCalendar.set(java.util.Calendar.MONTH, monthIndex)
        monthCalendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        monthCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        monthCalendar.set(java.util.Calendar.MINUTE, 0)
        monthCalendar.set(java.util.Calendar.SECOND, 0)
        monthCalendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val monthString = dateFormat.format(monthCalendar.time)
        
        // Sum all completed step targets for this month
        val monthSteps = stepTargetsByMonth[monthString]
            ?.sumOf { it.actualSteps } ?: 0
        
        yearlyData.add(
            DayData(
                steps = monthSteps.toFloat(),
                caloriesBurned = 0f, // Not used for step chart
                caloriesConsumed = 0f, // Not used for step chart
                workouts = 0 // Not used for step chart
            )
        )
    }
    
    return yearlyData
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
