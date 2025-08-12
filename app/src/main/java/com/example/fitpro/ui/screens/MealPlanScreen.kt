package com.example.fitpro.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fitpro.data.MealPlan
import com.example.fitpro.data.MealPlanDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.UserDao
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
                title = { Text("Meal Plan") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
            item {
                Text(
                    text = "Plan Your Meals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                // Breakfast Card
                MealSliderCard(
                    mealType = "Breakfast",
                    calories = breakfastCalories,
                    icon = Icons.Default.WbSunny,
                    onCaloriesChange = { breakfastCalories = it }
                )
            }

            item {
                // Lunch Card
                MealSliderCard(
                    mealType = "Lunch",
                    calories = lunchCalories,
                    icon = Icons.Default.Restaurant,
                    onCaloriesChange = { lunchCalories = it }
                )
            }

            item {
                // Dinner Card
                MealSliderCard(
                    mealType = "Dinner",
                    calories = dinnerCalories,
                    icon = Icons.Default.Restaurant,
                    onCaloriesChange = { dinnerCalories = it }
                )
            }

            item {
                // Total Daily Calories Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Daily Calories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = totalCalories.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Create Meal Plan Button
                Button(
                    onClick = {
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
                                        isCompleted = false
                                    )
                                    mealPlanDao.insertMealPlan(mealPlan)
                                    
                                    // Update user's calorie target
                                    userDao.updateCalorieTarget(currentUser.email, totalCalories)
                                }
                                // Navigate back to home after creating meal plan
                                navController.navigateUp()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Create Meal Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
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
                                            isCompleted = false
                                        )
                                        mealPlanDao.insertMealPlan(mealPlan)
                                        
                                        // Update user's calorie target
                                        userDao.updateCalorieTarget(currentUser.email, totalCalories)
                                    }
                                    showOverwriteDialog = false
                                    // Navigate back to home after creating meal plan
                                    navController.navigateUp()
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
private fun MealSliderCard(
    mealType: String,
    calories: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCaloriesChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    Icon(
                        icon, 
                        contentDescription = mealType,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = mealType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${calories.toInt()} cal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Target Calories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = calories,
                onValueChange = onCaloriesChange,
                valueRange = 100f..1000f,
                steps = 18,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}
