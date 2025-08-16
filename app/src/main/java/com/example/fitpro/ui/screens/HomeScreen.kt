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
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fitpro.R
import com.example.fitpro.Screen
import com.example.fitpro.data.UserProfile
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.WorkoutPlan
import com.example.fitpro.data.WorkoutPlanDao
import com.example.fitpro.data.MealPlan
import com.example.fitpro.data.MealPlanDao
import com.example.fitpro.data.CompletedWorkoutDao
import com.example.fitpro.data.CompletedWorkout
import com.example.fitpro.data.CompletedStepTargetDao
import com.example.fitpro.data.CompletedStepTarget
import com.example.fitpro.utils.StepCounterManager
import com.example.fitpro.utils.UserSession
import com.example.fitpro.utils.WorkoutTimerManager
import com.example.fitpro.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    userDao: UserDao,
    workoutPlanDao: WorkoutPlanDao,
    mealPlanDao: MealPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    completedStepTargetDao: CompletedStepTargetDao,
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
    
    // Get current meal plan for the user
    val currentMealPlan by (currentUserEmail?.let { email ->
        mealPlanDao.getCurrentMealPlan(email)
    } ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

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

    // Sync steps with database periodically and check target completion
    LaunchedEffect(dailySteps, currentUserEmail, userProfile?.stepTarget, userProfile?.isStepTargetCompleted) {
        currentUserEmail?.let { email ->
            scope.launch(Dispatchers.IO) {
                try {
                    userDao.updateSteps(email, dailySteps)
                    
                    // Check if step target is achieved and not yet marked as completed
                    userProfile?.let { profile ->
                        if (profile.stepTarget > 0 && 
                            dailySteps >= profile.stepTarget && 
                            !profile.isStepTargetCompleted) {
                            
                            // Mark target as completed
                            userDao.updateStepTargetCompleted(email, true)
                            
                            // Save to completed step targets table - only save the target amount, not actual steps
                            val completedTarget = CompletedStepTarget(
                                userEmail = email,
                                targetSteps = profile.stepTarget,
                                actualSteps = profile.stepTarget, // Save target amount, not actual steps
                                completedAt = System.currentTimeMillis()
                            )
                            completedStepTargetDao.insertCompletedStepTarget(completedTarget)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
        WelcomeSection(
            name = userProfile?.name ?: "User",
            profileImageUri = userProfile?.profileImageUri
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Current Plan Section with Workout Cards
        CurrentPlanSection(
            userEmail = currentUserEmail,
            workoutPlanDao = workoutPlanDao,
            completedWorkoutDao = completedWorkoutDao,
            userDao = userDao,
            onNavigateToPlan = { navController.navigate(Screen.Plan.route) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Stats Section
        ActivityStatsSection(
            steps = dailySteps,
            stepTarget = userProfile?.stepTarget ?: 0,
            calories = userProfile?.caloriesBurned ?: 0,
            heartRate = userProfile?.heartRate ?: 0,
            userDao = userDao,
            completedStepTargetDao = completedStepTargetDao,
            currentUserEmail = currentUserEmail,
            calorieTarget = userProfile?.calorieTarget ?: 0,
            currentMealPlan = currentMealPlan,
            navController = navController,
            mealPlanDao = mealPlanDao,
            userProfile = userProfile,
            stepCounterManager = stepCounterManager
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
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
}

@Composable
private fun WelcomeSection(
    name: String,
    profileImageUri: String? = null
) {
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
        // Profile Picture
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(25.dp)),
            color = MaterialTheme.colorScheme.primary
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(25.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground), // Fallback on error
                    onError = { 
                        android.util.Log.e("HomeScreen", "Failed to load profile image: $profileImageUri")
                    }
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CurrentPlanSection(
    userEmail: String?,
    workoutPlanDao: WorkoutPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    userDao: UserDao,
    onNavigateToPlan: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Use more efficient query - limit to recent workouts only
    val workoutPlans by remember(userEmail) {
        if (userEmail != null) {
            workoutPlanDao.getAllWorkoutPlans(userEmail)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column {
        // Section Title
        Text(
            text = "Current Plan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Workout Cards or Add Button
        if (workoutPlans.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = workoutPlans,
                    key = { it.id }
                ) { workout ->
                    WorkoutCard(
                        workout = workout,
                        workoutPlanDao = workoutPlanDao,
                        completedWorkoutDao = completedWorkoutDao,
                        userDao = userDao,
                        userEmail = userEmail ?: "",
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    workoutPlanDao.deleteWorkoutPlan(workout)
                                } catch (e: Exception) {

                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }
        } else {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToPlan() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Plan",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Your First Workout Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutPlan,
    workoutPlanDao: WorkoutPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    userDao: UserDao,
    userEmail: String,
    onDelete: () -> Unit
) {
    var showModal by remember { mutableStateOf(false) }
    
    // Get timer status
    val activeTimers by WorkoutTimerManager.activeTimers.collectAsStateWithLifecycle()
    val isTimerActive = WorkoutTimerManager.isTimerActive(workout.id)
    val isTimerRunning = WorkoutTimerManager.isTimerRunning(workout.id)
    
    // Real-time timer updates for card display
    var currentRemainingTime by remember { mutableStateOf(0) }
    
    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (WorkoutTimerManager.isTimerActive(workout.id)) {
                currentRemainingTime = WorkoutTimerManager.getCurrentRemainingTime(workout.id)
                delay(1000)
            }
        }
    }
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(140.dp) // Fixed height for consistency
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable { showModal = true }, // Make card clickable to open modal
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTimerRunning -> MaterialTheme.colorScheme.primaryContainer
                isTimerActive -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section with icon and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getWorkoutIcon(workout.type),
                        contentDescription = workout.type,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp) // Consistent icon size
                    )
                    
                    // Timer indicator
                    if (isTimerActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Timer status",
                            modifier = Modifier.size(12.dp),
                            tint = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
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
            
            // Content section with consistent spacing
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = workout.categoryName.ifEmpty { workout.type },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Show timer or duration
                if (isTimerActive && currentRemainingTime > 0) {
                    Text(
                        text = formatTime(currentRemainingTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                } else if (isTimerActive && currentRemainingTime <= 0) {
                    Text(
                        text = "TIME UP!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "${workout.duration} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Always show calories section for consistency
                Text(
                    text = workout.targetCalories?.let { "$it cal" } ?: "No calories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
    
    // Workout Action Modal
    if (showModal) {
        WorkoutActionModal(
            workout = workout,
            workoutPlanDao = workoutPlanDao,
            completedWorkoutDao = completedWorkoutDao,
            userDao = userDao,
            userEmail = userEmail,
            onDismiss = { showModal = false }
        )
    }
}

@Composable
private fun WorkoutActionModal(
    workout: WorkoutPlan,
    workoutPlanDao: WorkoutPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    userDao: UserDao,
    userEmail: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Get timer state from persistent manager
    val activeTimers by WorkoutTimerManager.activeTimers.collectAsStateWithLifecycle()
    val timerState = activeTimers[workout.id]
    val isTimerActive = WorkoutTimerManager.isTimerActive(workout.id)
    
    // Real-time timer updates
    var currentRemainingTime by remember { mutableStateOf(0) }
    
    // Update remaining time every second
    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (WorkoutTimerManager.isTimerActive(workout.id)) {
                currentRemainingTime = WorkoutTimerManager.getCurrentRemainingTime(workout.id)
                delay(1000)
            }
        } else {
            currentRemainingTime = workout.duration * 60
        }
    }
    
    // Confirmation dialog states
    var showCompleteConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
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
                // Workout Info
                Icon(
                    imageVector = getWorkoutIcon(workout.type),
                    contentDescription = workout.type,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = workout.categoryName.ifEmpty { workout.type },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "${workout.duration} minutes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                workout.targetCalories?.let { calories ->
                    Text(
                        text = "$calories calories target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Timer Display
                if (isTimerActive) {
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                currentRemainingTime <= 0 -> MaterialTheme.colorScheme.errorContainer
                                WorkoutTimerManager.isTimerRunning(workout.id) -> MaterialTheme.colorScheme.primaryContainer
                                WorkoutTimerManager.isTimerPaused(workout.id) -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentRemainingTime <= 0) {
                                    "TIME UP!"
                                } else {
                                    formatTime(currentRemainingTime)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = when {
                                    currentRemainingTime <= 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start/Pause/Resume Button
                    Button(
                        onClick = {
                            when {
                                !isTimerActive -> {
                                    WorkoutTimerManager.startTimer(workout.id, workout.duration)
                                }
                                WorkoutTimerManager.isTimerRunning(workout.id) -> {
                                    WorkoutTimerManager.pauseTimer(workout.id)
                                }
                                WorkoutTimerManager.isTimerPaused(workout.id) -> {
                                    WorkoutTimerManager.resumeTimer(workout.id)
                                }
                                currentRemainingTime <= 0 -> {
                                    WorkoutTimerManager.removeTimer(workout.id)
                                    WorkoutTimerManager.startTimer(workout.id, workout.duration)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = when {
                                !isTimerActive -> Icons.Default.PlayArrow
                                WorkoutTimerManager.isTimerRunning(workout.id) -> Icons.Default.Pause
                                WorkoutTimerManager.isTimerPaused(workout.id) -> Icons.Default.PlayArrow
                                currentRemainingTime <= 0 -> Icons.Default.Refresh
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                !isTimerActive -> "Start Workout"
                                WorkoutTimerManager.isTimerRunning(workout.id) -> "Pause"
                                WorkoutTimerManager.isTimerPaused(workout.id) -> "Resume"
                                currentRemainingTime <= 0 -> "Start Again"
                                else -> "Start Workout"
                            }
                        )
                    }
                    
                    // Completed Button
                    Button(
                        onClick = { showCompleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Completed")
                    }
                    
                    // Cancel Button
                    OutlinedButton(
                        onClick = { showCancelConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cancel Workout",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Close Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
    
    // Completion Confirmation Dialog
    if (showCompleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text("Complete Workout?")
            },
            text = {
                Text("Are you sure you want to mark this workout as completed? This will save your progress and remove the workout from your current plan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                // Save to completed workouts
                                val actualDuration = if (isTimerActive) {
                                    WorkoutTimerManager.getActualDurationMinutes(workout.id)
                                } else {
                                    workout.duration
                                }
                                
                                val completedWorkout = CompletedWorkout(
                                    userEmail = userEmail,
                                    workoutType = workout.type,
                                    categoryName = workout.categoryName.ifEmpty { workout.type },
                                    duration = workout.duration,
                                    targetCalories = workout.targetCalories,
                                    actualDuration = actualDuration,
                                    completedAt = TimeUtils.getBangladeshTimeMillis()
                                )
                                completedWorkoutDao.insertCompletedWorkout(completedWorkout)
                                
                                // Add calories to user's burned total if workout has calories
                                workout.targetCalories?.let { calories ->
                                    userDao.addCaloriesBurned(userEmail, calories)
                                }
                                
                                // Remove timer and workout plan
                                WorkoutTimerManager.removeTimer(workout.id)
                                workoutPlanDao.deleteWorkoutPlan(workout)
                                
                                withContext(Dispatchers.Main) {
                                    showCompleteConfirmation = false
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Cancel Confirmation Dialog
    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text("Cancel Workout?")
            },
            text = {
                Text("Are you sure you want to cancel this workout? This will remove it from your plan without saving any progress.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                // Remove timer and workout plan without saving
                                WorkoutTimerManager.removeTimer(workout.id)
                                workoutPlanDao.deleteWorkoutPlan(workout)
                                
                                withContext(Dispatchers.Main) {
                                    showCancelConfirmation = false
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text("Keep Workout")
                }
            }
        )
    }
}
// Helper function to format time
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun getWorkoutIcon(workoutType: String): ImageVector {
    return when (workoutType) {
        "Cardio" -> Icons.Default.DirectionsRun
        "Strength Training" -> Icons.Default.FitnessCenter
        "Flexibility" -> Icons.Default.SelfImprovement
        "Flexibility Training" -> Icons.Default.SelfImprovement
        "HIIT" -> Icons.Default.Timer
        "Yoga" -> Icons.Default.SelfImprovement
        "Pilates" -> Icons.Default.Accessibility
        "Dance Fitness / Zumba" -> Icons.Default.MusicNote
        "Stretching / Mobility" -> Icons.Default.SelfImprovement
        "CrossFit / Functional Training" -> Icons.Default.FitnessCenter
        "Swimming" -> Icons.Default.Pool
        "Cycling" -> Icons.Default.DirectionsBike
        "Walking (Brisk)" -> Icons.Default.DirectionsWalk
        "Rowing" -> Icons.Default.FitnessCenter
        "Boxing / Kickboxing" -> Icons.Default.SportsMma
        "Hiking" -> Icons.Default.Terrain
        "Bodyweight Circuit" -> Icons.Default.FitnessCenter
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
    completedStepTargetDao: CompletedStepTargetDao,
    currentUserEmail: String?,
    calorieTarget: Int = 0,
    currentMealPlan: MealPlan? = null,
    navController: NavController,
    mealPlanDao: MealPlanDao,
    userProfile: UserProfile?,
    stepCounterManager: StepCounterManager
) {
    var showStepTargetDialog by remember { mutableStateOf(false) }
    var showCaloriesResetDialog by remember { mutableStateOf(false) }
    var showMealPlanCompletionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Enhanced Steps Card with Progress Bar
        StepCounterCard(
            steps = steps,
            stepTarget = stepTarget,
            userProfile = userProfile,
            onClick = { showStepTargetDialog = true },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        // Enhanced Calories Burn Card
        ActivityStatCardEnhanced(
            icon = Icons.Default.LocalFireDepartment,
            value = "$calories",
            label = "Calories Burn",
            modifier = Modifier.weight(1f),
            onClick = { showCaloriesResetDialog = true }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Enhanced Calories Plan Card (shows status or target)
        ActivityStatCardEnhanced(
            icon = Icons.Default.Restaurant,
            value = if (currentMealPlan != null) {
                if (currentMealPlan.isCompleted) "Completed" else "${currentMealPlan.totalCalories}"
            } else {
                if (calorieTarget > 0) "$calorieTarget" else "0"
            },
            label = "Calories Plan",
            modifier = Modifier.weight(1f),
            onClick = { 
                // If there's an incomplete meal plan, show completion dialog
                if (currentMealPlan != null && !currentMealPlan.isCompleted) {
                    showMealPlanCompletionDialog = true
                } else {
                    // Otherwise navigate to meal plan screen
                    navController.navigate(Screen.MealPlan.route)
                }
            }
        )
    }
    
    // Step Target Dialog
    if (showStepTargetDialog) {
        StepTargetDialog(
            currentTarget = stepTarget,
            userProfile = userProfile,
            userDao = userDao,
            completedStepTargetDao = completedStepTargetDao,
            currentUserEmail = currentUserEmail,
            stepCounterManager = stepCounterManager,
            onDismiss = { showStepTargetDialog = false }
        )
    }
    
    // Calories Reset Dialog
    if (showCaloriesResetDialog) {
        AlertDialog(
            onDismissRequest = { showCaloriesResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text("Reset Calories Burned?")
            },
            text = {
                Text("Are you sure you want to reset your calories burned counter to 0? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUserEmail?.let { email ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    userDao.resetCaloriesBurned(email)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        showCaloriesResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaloriesResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Meal Plan Completion Dialog
    if (showMealPlanCompletionDialog && currentMealPlan != null) {
        Dialog(onDismissRequest = { showMealPlanCompletionDialog = false }) {
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
                        text = "Meal Plan Ready!",
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
                                showMealPlanCompletionDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        mealPlanDao.updateMealPlanCompletion(currentMealPlan.id, true)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                showMealPlanCompletionDialog = false
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
private fun ActivityStatCardEnhanced(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(min = 100.dp), // Match StepCounterCard height approximately
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
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
    userProfile: UserProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTargetCompleted = userProfile?.isStepTargetCompleted == true
    val hasTarget = stepTarget > 0
    
    // Calculate display values based on completion status
    val displaySteps = if (isTargetCompleted && steps >= stepTarget) {
        stepTarget // Show target amount when completed
    } else {
        minOf(steps, stepTarget) // Cap at target if not completed
    }
    
    val progress = if (stepTarget > 0) (displaySteps.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f) else 0f
    
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
                contentDescription = "Steps",
                tint = if (isTargetCompleted && steps >= stepTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Show "Completed" when target is achieved, otherwise show current steps
            if (isTargetCompleted && steps >= stepTarget) {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "$steps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Steps",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (hasTarget) {
                // Show progress bar and target info
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (isTargetCompleted && steps >= stepTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isTargetCompleted && steps >= stepTarget) {
                        "$stepTarget / $stepTarget" // Show completed as target/target
                    } else {
                        "$displaySteps / $stepTarget" // Show progress
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Show achievement message only when target is just reached but not yet marked as completed
                if (steps >= stepTarget && !isTargetCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🎉 Target Achieved!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Show "Set your target" message
                Text(
                    text = "Set your target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StepTargetDialog(
    currentTarget: Int,
    userProfile: UserProfile?,
    userDao: UserDao,
    completedStepTargetDao: CompletedStepTargetDao,
    currentUserEmail: String?,
    stepCounterManager: StepCounterManager,
    onDismiss: () -> Unit
) {
    var targetText by remember { 
        mutableStateOf(
            if (currentTarget == 0) "" else currentTarget.toString()
        ) 
    }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var newTargetValue by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    val hasActiveTarget = currentTarget > 0 && userProfile?.isStepTargetCompleted == false
    
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
                    text = if (currentTarget == 0) "Set Step Target" else "Change Step Target",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (hasActiveTarget) {
                        "You have an active target. Changing it will reset your progress."
                    } else {
                        "Set your daily step goal"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasActiveTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Step Target") },
                    placeholder = { Text("10000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (currentTarget > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Current target: $currentTarget steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
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
                                newTargetValue = target
                                if (hasActiveTarget && target != currentTarget) {
                                    // Show confirmation dialog for active target
                                    showConfirmationDialog = true
                                } else {
                                    // Set target directly
                                    setStepTarget(
                                        target = target,
                                        userDao = userDao,
                                        currentUserEmail = currentUserEmail,
                                        stepCounterManager = stepCounterManager,
                                        scope = scope,
                                        onComplete = onDismiss
                                    )
                                }
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
    
    // Confirmation dialog for overwriting active target
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text("Replace Active Target?")
            },
            text = {
                Text("You have an active step target. Changing it will reset your progress. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        setStepTarget(
                            target = newTargetValue,
                            userDao = userDao,
                            currentUserEmail = currentUserEmail,
                            stepCounterManager = stepCounterManager,
                            scope = scope,
                            onComplete = {
                                showConfirmationDialog = false
                                onDismiss()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Replace")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun setStepTarget(
    target: Int,
    userDao: UserDao,
    currentUserEmail: String?,
    stepCounterManager: StepCounterManager,
    scope: CoroutineScope,
    onComplete: () -> Unit
) {
    currentUserEmail?.let { email ->
        scope.launch(Dispatchers.IO) {
            try {
                userDao.updateStepTarget(email, target)
                userDao.updateStepTargetCompleted(email, false) // Reset completion status
                userDao.updateSteps(email, 0) // Reset step count to 0 for new target
                
                // Reset the step counter manager as well
                stepCounterManager.resetSteps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    onComplete()
}

@Composable
private fun CalorieTargetDialog(
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
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Daily Calorie Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Set your daily calorie consumption goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Calorie Target") },
                    placeholder = { Text("2000") },
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
