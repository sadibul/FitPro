package com.example.fitpro.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.WorkoutPlan
import com.example.fitpro.data.WorkoutPlanDao
import com.example.fitpro.utils.StepCounterManager
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    userDao: UserDao,
    workoutPlanDao: WorkoutPlanDao,
    stepCounterManager: StepCounterManager,
    onBMICardClick: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val scrollState = rememberScrollState()
    val dailySteps by stepCounterManager.dailySteps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val userSession = remember { UserSession(context) }
    val currentUserEmail = userSession.getCurrentUserEmail()

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            stepCounterManager.startListening()
        }
    }

    // Check and request permission when screen loads
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) -> {
                stepCounterManager.startListening()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    // Start step counting when screen is active and stop when inactive
    DisposableEffect(Unit) {
        onDispose {
            stepCounterManager.stopListening()
        }
    }

    // Sync steps with database periodically
    LaunchedEffect(dailySteps, currentUserEmail) {
        currentUserEmail?.let { email ->
            scope.launch {
                userDao.updateSteps(email, dailySteps)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
        // Welcome Section
        WelcomeSection(userProfile?.name ?: "User")

        Spacer(modifier = Modifier.height(24.dp))

        // Current Plan Section with Workout Cards
        CurrentPlanSection(
            planName = userProfile?.currentPlan ?: "Weight Loss Plan",
            userEmail = currentUserEmail,
            workoutPlanDao = workoutPlanDao
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Stats Section
        ActivityStatsSection(
            steps = dailySteps,
            stepTarget = userProfile?.stepTarget ?: 0,
            calories = userProfile?.caloriesBurned ?: 0,
            heartRate = userProfile?.heartRate ?: 0,
            userDao = userDao,
            currentUserEmail = currentUserEmail
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BMI Section
        BMICard(
            bmi = userProfile?.calculateBMI() ?: 0f,
            category = userProfile?.getBMICategory() ?: "Unknown",
            onClick = onBMICardClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Health Tips Section
        HealthTipsCard()
        
        Spacer(modifier = Modifier.height(16.dp))

        // Medical Assistance Card
        MedicalAssistanceCard(
            modifier = Modifier.fillMaxWidth()
        )
        
        // Test button for adding steps (for development/testing)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                stepCounterManager.addStepsForTesting(100)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Add 100 Steps (Test)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Refresh button (floating action button)
    FloatingActionButton(
        onClick = {
            scope.launch {
                stepCounterManager.stopListening()
                delay(500)
                stepCounterManager.startListening()
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh Step Counter",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
}

@Composable
private fun WelcomeSection(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // Profile Picture Placeholder
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(25.dp)),
            color = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun CurrentPlanSection(
    planName: String,
    userEmail: String?,
    workoutPlanDao: WorkoutPlanDao
) {
    val scope = rememberCoroutineScope()
    val workoutPlans by (userEmail?.let { 
        workoutPlanDao.getAllWorkoutPlans(it).collectAsStateWithLifecycle(initialValue = emptyList<WorkoutPlan>())
    } ?: flowOf(emptyList<WorkoutPlan>()).collectAsStateWithLifecycle(initialValue = emptyList<WorkoutPlan>()))

    Column {
        // Main Plan Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = "Fitness Plan"
                )
            }
        }

        // Workout Cards Section
        if (workoutPlans.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Workouts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(workoutPlans) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onDelete = {
                            scope.launch {
                                workoutPlanDao.deleteWorkoutPlan(workout)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutPlan,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getWorkoutIcon(workout.type),
                    contentDescription = workout.type,
                    tint = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete workout",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = workout.type,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${workout.duration} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${workout.targetCalories} cal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getWorkoutIcon(workoutType: String): ImageVector {
    return when (workoutType) {
        "Cardio" -> Icons.Default.DirectionsRun
        "Strength" -> Icons.Default.FitnessCenter
        "Flexibility" -> Icons.Default.SelfImprovement
        "HIIT" -> Icons.Default.Timer
        "Yoga" -> Icons.Default.Accessibility
        else -> Icons.Default.FitnessCenter
    }
}

@Composable
private fun ActivityStatsSection(
    steps: Int, 
    stepTarget: Int, 
    calories: Int, 
    heartRate: Int,
    userDao: UserDao,
    currentUserEmail: String?
) {
    var showStepTargetDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Enhanced Steps Card with Progress Bar
        StepCounterCard(
            steps = steps,
            stepTarget = stepTarget,
            onClick = { showStepTargetDialog = true },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ActivityStatCard(
            icon = Icons.Default.LocalFireDepartment,
            value = "$calories",
            label = "Calories",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ActivityStatCard(
            icon = Icons.Default.Favorite,
            value = "$heartRate",
            label = "BPM",
            modifier = Modifier.weight(1f)
        )
    }
    
    // Step Target Dialog
    if (showStepTargetDialog) {
        StepTargetDialog(
            currentTarget = stepTarget,
            onDismiss = { showStepTargetDialog = false },
            onTargetSet = { newTarget ->
                currentUserEmail?.let { email ->
                    scope.launch {
                        userDao.updateStepTarget(email, newTarget)
                    }
                }
                showStepTargetDialog = false
            }
        )
    }
}

@Composable
private fun ActivityStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BMICard(bmi: Float, category: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
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
                    text = "BMI",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "View BMI Details"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%.1f", bmi),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HealthTipsCard() {
    val healthTips = listOf(
        "💧 Drink at least 8 glasses of water daily for optimal hydration",
        "🥗 Include colorful vegetables in every meal for better nutrition",
        "🚶‍♀️ Take a 10-minute walk after meals to improve digestion",
        "😴 Aim for 7-9 hours of quality sleep each night",
        "🧘‍♂️ Practice deep breathing for 5 minutes daily to reduce stress",
        "🥛 Include protein in every meal to maintain muscle mass",
        "☀️ Get 15 minutes of sunlight daily for vitamin D",
        "🏃‍♂️ Take stairs instead of elevators when possible",
        "🥜 Eat a handful of nuts daily for healthy fats",
        "📱 Take breaks from screens every 20 minutes"
    )
    
    var currentTipIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000) // Change tip every 4 seconds
            currentTipIndex = (currentTipIndex + 1) % healthTips.size
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Health Tip",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = healthTips[currentTipIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun MedicalAssistanceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .clickable {
                // Open Google Maps focused on Dhaka, Bangladesh to search for hospitals/consultancy
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    // Dhaka coordinates: 23.8103, 90.4125
                    data = Uri.parse("geo:23.8103,90.4125?q=hospitals+clinics+medical+consultancy+in+dhaka+bangladesh&z=12")
                    setPackage("com.google.android.apps.maps")
                }
                
                // If Google Maps is not installed, use generic intent
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("geo:23.8103,90.4125?q=hospitals+clinics+medical+consultancy+in+dhaka+bangladesh&z=12")
                    }
                    context.startActivity(genericIntent)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = "Medical Assistance",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Medical Assistance - Dhaka",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Find nearby hospitals, clinics, and medical consultancies in Dhaka, Bangladesh",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tap to open in Maps",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Visual indicator
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Tap indicator",
                modifier = Modifier
                    .size(24.dp)
                    .alpha(0.6f),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StepCounterCard(
    steps: Int,
    stepTarget: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (stepTarget > 0) (steps.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f) else 0f
    val displayTarget = if (stepTarget == 0) 0 else stepTarget
    
    Card(
        modifier = modifier
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DirectionsWalk, 
                contentDescription = "Steps"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$steps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Steps",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Only show progress bar if target is set
            if (stepTarget > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = "$steps / $displayTarget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StepTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onTargetSet: (Int) -> Unit
) {
    var targetText by remember { 
        mutableStateOf(
            if (currentTarget == 0) "" else currentTarget.toString()
        ) 
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DirectionsWalk,
                    contentDescription = "Step Target",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Set Step Target",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Set your daily step goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Step Target") },
                    placeholder = { Text("0000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val target = targetText.toIntOrNull()
                            if (target != null && target > 0) {
                                onTargetSet(target)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Target")
                    }
                }
            }
        }
    }
}
