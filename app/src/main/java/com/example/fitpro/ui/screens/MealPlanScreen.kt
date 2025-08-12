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
    
    var targetCalories by remember { mutableStateOf("1500") }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var createdMealPlan by remember { mutableStateOf<MealPlan?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Get current meal plan status
    val currentMealPlan by mealPlanDao.getCurrentMealPlan(currentUser.email).collectAsState(initial = null)

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
                // Target Calories Input Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Target Daily Calories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = targetCalories,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                    targetCalories = it
                                }
                            },
                            label = { Text("Calories") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Set your target calories for the day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Create Meal Plan Button
                Button(
                    onClick = {
                        if (targetCalories.isNotBlank() && targetCalories.toIntOrNull() != null) {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val calories = targetCalories.toInt()
                                    val mealPlan = MealPlan(
                                        userEmail = currentUser.email,
                                        name = "Daily Meal Plan",
                                        breakfast = """{"calories": ${calories * 0.25}}""",
                                        lunch = """{"calories": ${calories * 0.4}}""",
                                        dinner = """{"calories": ${calories * 0.35}}""",
                                        totalCalories = calories,
                                        isCompleted = false
                                    )
                                    mealPlanDao.insertMealPlan(mealPlan)
                                    
                                    // Update user's calorie target
                                    userDao.updateCalorieTarget(currentUser.email, calories)
                                    
                                    // Get the created meal plan
                                    val newMealPlan = mealPlanDao.getCurrentMealPlan(currentUser.email)
                                    createdMealPlan = mealPlan
                                }
                                showCompletionDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    enabled = targetCalories.isNotBlank() && targetCalories.toIntOrNull() != null
                ) {
                    Text("Create Meal Plan")
                }
            }

            // Show current meal plan status if exists
            currentMealPlan?.let { mealPlan ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mealPlan.isCompleted) 
                                MaterialTheme.colorScheme.primaryContainer
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Current Meal Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Target: ${mealPlan.totalCalories} calories",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Text(
                                text = "Status: ${if (mealPlan.isCompleted) "Completed" else "In Progress"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mealPlan.isCompleted) 
                                    MaterialTheme.colorScheme.primary
                                else 
                                    MaterialTheme.colorScheme.secondary
                            )
                            
                            if (!mealPlan.isCompleted) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                mealPlanDao.updateMealPlanCompletion(mealPlan.id, true)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark as Complete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Completion Dialog
    if (showCompletionDialog) {
        Dialog(onDismissRequest = { showCompletionDialog = false }) {
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
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Meal Plan Created!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Do you want to mark it as Complete?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showCompletionDialog = false
                                navController.navigateUp()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }
                        
                        Button(
                            onClick = {
                                createdMealPlan?.let { mealPlan ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            mealPlanDao.updateMealPlanCompletion(mealPlan.id, true)
                                        }
                                        showCompletionDialog = false
                                        navController.navigateUp()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
