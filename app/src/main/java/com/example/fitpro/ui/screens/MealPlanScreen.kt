package com.example.fitpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fitpro.data.MealPlan
import com.example.fitpro.data.MealPlanDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.UserDao
import com.example.fitpro.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    mealPlanDao: MealPlanDao,
    userDao: UserDao
) {
    val userProfile by userProfileFlow.collectAsState(initial = null)
    val currentUser = userProfile ?: return
    
    var breakfastCalories by remember { mutableStateOf(400f) }
    var lunchCalories by remember { mutableStateOf(600f) }
    var dinnerCalories by remember { mutableStateOf(500f) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Get current meal plan status
    val currentMealPlan by mealPlanDao.getCurrentMealPlan(currentUser.email).collectAsState(initial = null)
    
    // Calculate total calories
    val totalCalories = (breakfastCalories + lunchCalories + dinnerCalories).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Meal Plan",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ) 
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        IconButton(onClick = { 
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Plan Your Daily Meals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            item {
                // Breakfast Card
                ModernMealSliderCard(
                    mealType = "Breakfast",
                    calories = breakfastCalories,
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFF4A90E2),
                    onCaloriesChange = { breakfastCalories = it }
                )
            }

            item {
                // Lunch Card
                ModernMealSliderCard(
                    mealType = "Lunch",
                    calories = lunchCalories,
                    icon = Icons.Default.Restaurant,
                    iconColor = Color(0xFF4A90E2),
                    onCaloriesChange = { lunchCalories = it }
                )
            }

            item {
                // Dinner Card
                ModernMealSliderCard(
                    mealType = "Dinner",
                    calories = dinnerCalories,
                    icon = Icons.Default.Restaurant,
                    iconColor = Color(0xFF4A90E2),
                    onCaloriesChange = { dinnerCalories = it }
                )
            }

            item {
                // Total Daily Calories Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F4FD)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Daily Calories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = totalCalories.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A90E2),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Modern Create Meal Plan Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            // Check if user has an incomplete meal plan
                            val mealPlan = currentMealPlan
                            if (mealPlan != null && !mealPlan.isCompleted) {
                                // Show confirmation dialog for overwriting incomplete plan
                                showOverwriteDialog = true
                            } else {
                                // No meal plan or completed meal plan - create directly
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val mealPlan = MealPlan(
                                            userEmail = currentUser.email,
                                            name = "Daily Meal Plan",
                                            breakfast = """{"calories": ${breakfastCalories.toInt()}}""",
                                            lunch = """{"calories": ${lunchCalories.toInt()}}""",
                                            dinner = """{"calories": ${dinnerCalories.toInt()}}""",
                                            totalCalories = totalCalories,
                                            isCompleted = false,
                                            createdAt = TimeUtils.getBangladeshDateString()
                                        )
                                        mealPlanDao.insertMealPlan(mealPlan)
                                        
                                        // Update user's calorie target
                                        userDao.updateCalorieTarget(currentUser.email, totalCalories)
                                    }
                                    // Navigate back to home after creating meal plan
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF4A90E2),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Create Meal Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Overwrite Confirmation Dialog
    if (showOverwriteDialog) {
        Dialog(onDismissRequest = { showOverwriteDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Replace Meal Plan?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Are you sure you want to create this meal plan? If you do this, your previous meal plan data will be lost!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showOverwriteDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                // User confirmed - create the new meal plan
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val mealPlan = MealPlan(
                                            userEmail = currentUser.email,
                                            name = "Daily Meal Plan",
                                            breakfast = """{"calories": ${breakfastCalories.toInt()}}""",
                                            lunch = """{"calories": ${lunchCalories.toInt()}}""",
                                            dinner = """{"calories": ${dinnerCalories.toInt()}}""",
                                            totalCalories = totalCalories,
                                            isCompleted = false,
                                            createdAt = TimeUtils.getBangladeshDateString()
                                        )
                                        mealPlanDao.insertMealPlan(mealPlan)
                                        
                                        // Update user's calorie target
                                        userDao.updateCalorieTarget(currentUser.email, totalCalories)
                                    }
                                    showOverwriteDialog = false
                                    // Navigate back to home after creating meal plan
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Replace")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernMealSliderCard(
    mealType: String,
    calories: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onCaloriesChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = iconColor.copy(alpha = 0.15f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, 
                                contentDescription = mealType,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = mealType,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${calories.toInt()} cal",
                        style = MaterialTheme.typography.titleMedium,
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Target Calories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom Modern Slider Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Background track
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE8F4FD)
                ) {}
                
                // Slider
                Slider(
                    value = calories,
                    onValueChange = onCaloriesChange,
                    valueRange = 100f..1000f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = iconColor,
                        activeTrackColor = iconColor,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }
    }
}
