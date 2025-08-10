package com.example.fitpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitpro.FitProApp
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.WorkoutPlan
import com.example.fitpro.data.WorkoutPlanDao
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    workoutPlanDao: WorkoutPlanDao
) {
    val context = LocalContext.current
    val userSession = remember { UserSession(context) }
    val currentUserEmail = userSession.getCurrentUserEmail()
    
    var selectedWorkoutType by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var targetCalories by remember { mutableStateOf("300") }

    val workoutTypes = listOf(
        "Cardio" to Icons.Default.DirectionsRun,
        "Strength" to Icons.Default.FitnessCenter,
        "Flexibility" to Icons.Default.SelfImprovement,
        "HIIT" to Icons.Default.Timer,
        "Yoga" to Icons.Default.Accessibility
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Plan") },
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
                    text = "Choose Your Workout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Workout Type Selection
            items(workoutTypes) { (type, icon) ->
                WorkoutTypeCard(
                    type = type,
                    icon = icon,
                    isSelected = selectedWorkoutType == type,
                    onSelect = { selectedWorkoutType = type }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Customize Your Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Duration Slider
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Duration (minutes)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { duration = it.toInt().toString() },
                            valueRange = 15f..120f,
                            steps = 20
                        )
                        Text(text = "$duration minutes")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Calories
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Target Calories",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = targetCalories.toFloat(),
                            onValueChange = { targetCalories = it.toInt().toString() },
                            valueRange = 100f..1000f,
                            steps = 17
                        )
                        Text(text = "$targetCalories calories")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            currentUserEmail?.let { email ->
                                val workoutPlan = WorkoutPlan(
                                    userEmail = email,
                                    type = selectedWorkoutType,
                                    duration = duration.toInt(),
                                    targetCalories = targetCalories.toInt()
                                )
                                workoutPlanDao.insertWorkoutPlan(workoutPlan)
                                navController.navigateUp()
                            }
                        }
                    },
                    enabled = selectedWorkoutType.isNotEmpty() && currentUserEmail != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Create Workout Plan")
                }
            }
        }
    }
}

@Composable
private fun WorkoutTypeCard(
    type: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type,
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = type,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
