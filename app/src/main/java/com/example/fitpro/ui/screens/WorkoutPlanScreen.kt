package com.example.fitpro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.WorkoutCategory
import com.example.fitpro.data.WorkoutCategories
import com.example.fitpro.data.WorkoutPlan
import com.example.fitpro.data.WorkoutPlanDao
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    
    var showModal by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<WorkoutCategory?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Plan") },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Choose Your Workout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Workout Categories
            items(WorkoutCategories.categories) { category ->
                WorkoutCategoryCard(
                    category = category,
                    onClick = {
                        selectedCategory = category
                        showModal = true
                    }
                )
            }
        }
    }

    // Modal Dialog for customization
    if (showModal && selectedCategory != null) {
        WorkoutCustomizationModal(
            category = selectedCategory!!,
            onDismiss = { 
                showModal = false
                selectedCategory = null
            },
            onAdd = { duration, calories ->
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val workoutPlan = WorkoutPlan(
                                userEmail = currentUserEmail ?: "",
                                type = selectedCategory!!.name,
                                categoryId = selectedCategory!!.id,
                                categoryName = selectedCategory!!.name,
                                duration = duration,
                                targetCalories = calories
                            )
                            workoutPlanDao.insertWorkoutPlan(workoutPlan)
                        }
                        
                        showModal = false
                        selectedCategory = null
                        
                        // Navigate back to home to see the added workout
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
private fun WorkoutCategoryCard(
    category: WorkoutCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.icon),
                    contentDescription = category.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${category.minDuration}-${category.maxDuration} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkoutCustomizationModal(
    category: WorkoutCategory,
    onDismiss: () -> Unit,
    onAdd: (duration: Int, calories: Int?) -> Unit
) {
    var duration by remember { mutableStateOf(category.minDuration.toFloat()) }
    var calories by remember { mutableStateOf((category.minCalories ?: 0).toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Slider
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = category.minDuration.toFloat()..category.maxDuration.toFloat(),
                    steps = ((category.maxDuration - category.minDuration) / 5).coerceAtLeast(1)
                )
                Text(
                    text = "${duration.roundToInt()} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calories Slider (only if category has calories)
                if (category.hasCalories) {
                    Text(
                        text = "Target Calories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = calories,
                        onValueChange = { calories = it },
                        valueRange = (category.minCalories ?: 0).toFloat()..(category.maxCalories ?: 500).toFloat(),
                        steps = ((category.maxCalories ?: 500) - (category.minCalories ?: 0)) / 25
                    )
                    Text(
                        text = "${calories.roundToInt()} calories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add Button
                Button(
                    onClick = {
                        onAdd(
                            duration.roundToInt(),
                            if (category.hasCalories) calories.roundToInt() else null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Plan", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// Helper function to get icons for categories
@Composable
private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "strength" -> Icons.Default.FitnessCenter
        "cardio" -> Icons.Default.DirectionsRun
        "yoga" -> Icons.Default.SelfImprovement
        "pilates" -> Icons.Default.Accessibility
        "hiit" -> Icons.Default.Timer
        "dance" -> Icons.Default.MusicNote
        "stretching" -> Icons.Default.SelfImprovement
        "flexibility" -> Icons.Default.SelfImprovement
        "crossfit" -> Icons.Default.FitnessCenter
        "swimming" -> Icons.Default.Pool
        "cycling" -> Icons.Default.DirectionsBike
        "walking" -> Icons.Default.DirectionsWalk
        "rowing" -> Icons.Default.FitnessCenter
        "boxing" -> Icons.Default.SportsMma
        "hiking" -> Icons.Default.Terrain
        "bodyweight" -> Icons.Default.FitnessCenter
        else -> Icons.Default.FitnessCenter
    }
}
