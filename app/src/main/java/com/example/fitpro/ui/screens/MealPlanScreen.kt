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
import androidx.navigation.NavController
import com.example.fitpro.data.MealPlan
import com.example.fitpro.data.MealPlanDao
import com.example.fitpro.data.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    mealPlanDao: MealPlanDao
) {
    var breakfastCalories by remember { mutableStateOf("400") }
    var lunchCalories by remember { mutableStateOf("600") }
    var dinnerCalories by remember { mutableStateOf("500") }
    val coroutineScope = rememberCoroutineScope()

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
                
                Spacer(modifier = Modifier.height(16.dp))

                // Daily calorie distribution
                Text(
                    text = "Daily Calorie Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                // Breakfast Section
                MealCard(
                    mealType = "Breakfast",
                    calories = breakfastCalories,
                    icon = Icons.Default.WbSunny,
                    onCaloriesChange = { breakfastCalories = it }
                )
            }

            item {
                // Lunch Section
                MealCard(
                    mealType = "Lunch",
                    calories = lunchCalories,
                    icon = Icons.Default.Restaurant,
                    onCaloriesChange = { lunchCalories = it }
                )
            }

            item {
                // Dinner Section
                MealCard(
                    mealType = "Dinner",
                    calories = dinnerCalories,
                    icon = Icons.Default.Restaurant,
                    onCaloriesChange = { dinnerCalories = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Total Calories Card
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
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Daily Calories",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${breakfastCalories.toInt() + lunchCalories.toInt() + dinnerCalories.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val totalCalories = breakfastCalories.toInt() +
                                              lunchCalories.toInt() +
                                              dinnerCalories.toInt()
                            val mealPlan = MealPlan(
                                userId = 1,
                                name = "Daily Meal Plan",
                                breakfast = """{"calories": ${breakfastCalories}}""",
                                lunch = """{"calories": ${lunchCalories}}""",
                                dinner = """{"calories": ${dinnerCalories}}""",
                                totalCalories = totalCalories
                            )
                            mealPlanDao.insertMealPlan(mealPlan)
                            navController.navigateUp()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Create Meal Plan")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(
    mealType: String,
    calories: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCaloriesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
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
                    Icon(icon, contentDescription = mealType)
                    Text(
                        text = mealType,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "$calories cal",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Target Calories",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = calories.toFloat(),
                onValueChange = { onCaloriesChange(it.toInt().toString()) },
                valueRange = 100f..1000f,
                steps = 17
            )
        }
    }
}
