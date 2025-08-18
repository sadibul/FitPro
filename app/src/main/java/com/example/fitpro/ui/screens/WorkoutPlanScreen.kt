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
                title = { 
                    Text(
                        "Workout Plan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Choose Your Workout",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
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
private fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "cardio" -> Color(0xFFE74C3C)
        "strength" -> Color(0xFF3498DB)
        "strength training" -> Color(0xFF3498DB)
        "flexibility" -> Color(0xFF2ECC71)
        "balance" -> Color(0xFF9B59B6)
        "endurance" -> Color(0xFFE67E22)
        "sports" -> Color(0xFFF39C12)
        "yoga" -> Color(0xFF27AE60)
        "pilates" -> Color(0xFF8E44AD)
        "swimming" -> Color(0xFF3498DB)
        "running" -> Color(0xFFE74C3C)
        "cycling" -> Color(0xFFF39C12)
        "walking" -> Color(0xFF27AE60)
        "bodyweight circuit" -> Color(0xFFFF6B6B)
        "hiking" -> Color(0xFF4ECDC4)
        "boxing" -> Color(0xFFFF8E53)
        "rowing" -> Color(0xFF4A90E2)
        "crossfit" -> Color(0xFFD63384)
        "stretching" -> Color(0xFF20C997)
        "dance" -> Color(0xFFE83E8C)
        "hiit" -> Color(0xFFDC3545)
        "hit" -> Color(0xFFDC3545)
        else -> Color(0xFF95A5A6)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with colored background
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    color = getCategoryColor(category.icon)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.icon),
                        contentDescription = category.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${category.minDuration}-${category.maxDuration} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
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
                .padding(20.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with category icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Empty space for balance
                    Spacer(modifier = Modifier.size(36.dp))
                    
                    // Category Icon - Centered
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = getCategoryColor(category.name).copy(alpha = 0.15f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (category.name.lowercase()) {
                                    "strength", "strength training" -> Icons.Default.FitnessCenter
                                    "cardio", "running" -> Icons.Default.DirectionsRun
                                    "cycling" -> Icons.Default.DirectionsBike
                                    "walking", "hiking" -> Icons.Default.DirectionsWalk
                                    else -> Icons.Default.SportsGymnastics
                                },
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = getCategoryColor(category.name)
                            )
                        }
                    }
                    
                    // Close Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category Title
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Duration Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
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
                            value = duration,
                            onValueChange = { duration = it },
                            valueRange = category.minDuration.toFloat()..category.maxDuration.toFloat(),
                            steps = ((category.maxDuration - category.minDuration) / 5).coerceAtLeast(1),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4A90E2),
                                activeTrackColor = Color(0xFF4A90E2),
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Duration Value Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF4A90E2).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${duration.roundToInt()} minutes",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4A90E2),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Calories Section (only if category has calories)
                if (category.hasCalories) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Target Calories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Custom Modern Slider Track for Calories
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
                                onValueChange = { calories = it },
                                valueRange = (category.minCalories ?: 0).toFloat()..(category.maxCalories ?: 500).toFloat(),
                                steps = ((category.maxCalories ?: 500) - (category.minCalories ?: 0)) / 25,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A90E2),
                                    activeTrackColor = Color(0xFF4A90E2),
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Calories Value Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF4A90E2).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${calories.roundToInt()} calories",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4A90E2),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(36.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Modern Add Button with Gradient Effect
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            onAdd(
                                duration.roundToInt(),
                                if (category.hasCalories) calories.roundToInt() else null
                            )
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
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Add to Plan", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
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
