package com.example.fitpro.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.unit.sp
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
    
    // BMI Tips Modal State
    var showBMITipsModal by remember { mutableStateOf(false) }
    
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
                            
                            // Save to completed step targets table - save the actual daily steps achieved
                            val completedTarget = CompletedStepTarget(
                                userEmail = email,
                                targetSteps = profile.stepTarget,
                                actualSteps = profile.dailySteps, // Save actual daily steps achieved
                                completedAt = TimeUtils.getBangladeshTimeMillis()
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        // Welcome Section
        ModernWelcomeSection(
            name = userProfile?.name ?: "User",
            profileImageUri = userProfile?.profileImageUri
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Current Plan Section with Workout Cards
        ModernCurrentPlanSection(
            userEmail = currentUserEmail,
            workoutPlanDao = workoutPlanDao,
            completedWorkoutDao = completedWorkoutDao,
            userDao = userDao,
            onNavigateToPlan = { navController.navigate(Screen.Plan.route) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Activity Stats Section with colorful cards
        ModernActivityStatsSection(
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

        Spacer(modifier = Modifier.height(20.dp))

        // BMI Section with modern styling
        ModernBMICard(
            bmi = userProfile?.calculateBMI() ?: 0f,
            category = userProfile?.getBMICategory() ?: "Unknown",
            onClick = { showBMITipsModal = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Health Assistance Card (replaces both health tips and medical assistance)
        ModernHealthAssistanceCard(
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Health Tips Card with cycling tips
        ModernHealthTipsCard(
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // BMI Tips Modal
    if (showBMITipsModal) {
        BMITipsModal(
            bmi = userProfile?.calculateBMI() ?: 0f,
            category = userProfile?.getBMICategory() ?: "Unknown",
            onDismiss = { showBMITipsModal = false }
        )
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
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)), // Light grey stroke
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // Content section with consistent spacing
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
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button at top right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Workout Info Header
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getWorkoutIcon(workout.type),
                            contentDescription = workout.type,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = workout.categoryName.ifEmpty { workout.type },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Workout Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Duration Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${workout.duration}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Calories Info (if available)
                    workout.targetCalories?.let { calories ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$calories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "calories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Timer Display
                if (isTimerActive) {
                    Card(
                        modifier = Modifier.size(140.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                currentRemainingTime <= 0 -> MaterialTheme.colorScheme.errorContainer
                                WorkoutTimerManager.isTimerRunning(workout.id) -> MaterialTheme.colorScheme.primaryContainer
                                WorkoutTimerManager.isTimerPaused(workout.id) -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentRemainingTime <= 0) {
                                        "TIME UP!"
                                    } else {
                                        formatTime(currentRemainingTime)
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = when {
                                        currentRemainingTime <= 0 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (currentRemainingTime > 0) {
                                    Text(
                                        text = when {
                                            WorkoutTimerManager.isTimerRunning(workout.id) -> "Running"
                                            WorkoutTimerManager.isTimerPaused(workout.id) -> "Paused"
                                            else -> "Ready"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when {
                                !isTimerActive -> "Start Workout"
                                WorkoutTimerManager.isTimerRunning(workout.id) -> "Pause"
                                WorkoutTimerManager.isTimerPaused(workout.id) -> "Resume"
                                currentRemainingTime <= 0 -> "Start Again"
                                else -> "Start Workout"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Completed Button
                    Button(
                        onClick = { showCompleteConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Completed",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Cancel Button
                    OutlinedButton(
                        onClick = { showCancelConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Cancel Workout",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                .padding(8.dp)
                .heightIn(min = 140.dp), // Match StepCounterCard height
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            .heightIn(min = 140.dp)
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
            
            // Show "Done" when target is achieved, otherwise show current steps
            if (isTargetCompleted && steps >= stepTarget) {
                Text(
                    text = "Done",
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
                        "$stepTarget/$stepTarget" // Show completed as target/target
                    } else {
                        "${minOf(steps, stepTarget)}/$stepTarget" // Show actual progress
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
                        Text(
                            text = "Set",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
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
                        Text(
                            text = "Set",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Modern composables for the new design

@Composable
private fun ModernWelcomeSection(
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Profile Picture
        Surface(
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(22.5.dp)),
            color = MaterialTheme.colorScheme.primary
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(22.5.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
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
private fun ModernCurrentPlanSection(
    userEmail: String?,
    workoutPlanDao: WorkoutPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    userDao: UserDao,
    onNavigateToPlan: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val workoutPlans by remember(userEmail) {
        if (userEmail != null) {
            workoutPlanDao.getAllWorkoutPlans(userEmail)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column {
        Text(
            text = "Current Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
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
                    .height(140.dp)
                    .clickable { onNavigateToPlan() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F4FD)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Plan",
                        modifier = Modifier.size(40.dp),
                        tint = Color(0xFF4A90E2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Your First Workout Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A90E2),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A90E2).copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernActivityStatsSection(
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Steps Card with light blue background
        ModernStepCounterCard(
            steps = steps,
            stepTarget = stepTarget,
            userProfile = userProfile,
            onClick = { showStepTargetDialog = true },
            modifier = Modifier.weight(1f)
        )
        
        // Calories Burn Card with light orange background
        ModernActivityStatCard(
            icon = Icons.Default.LocalFireDepartment,
            value = "$calories",
            label = "Calories\nBurn",
            backgroundColor = Color(0xFFFFE5CC),
            iconColor = Color(0xFFFF6B35),
            modifier = Modifier.weight(1f),
            onClick = { showCaloriesResetDialog = true }
        )
        
        // Calories Plan Card with light green background
        ModernActivityStatCard(
            icon = Icons.Default.Restaurant,
            value = if (currentMealPlan != null) {
                if (currentMealPlan.isCompleted) "Done" else "${currentMealPlan.totalCalories}"
            } else {
                if (calorieTarget > 0) "$calorieTarget" else "0"
            },
            label = "Calories\nPlan",
            backgroundColor = Color(0xFFE8F5E8),
            iconColor = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f),
            onClick = { 
                if (currentMealPlan != null && !currentMealPlan.isCompleted) {
                    showMealPlanCompletionDialog = true
                } else {
                    navController.navigate(Screen.MealPlan.route)
                }
            }
        )
    }
    
    // Dialogs (keeping the existing ones)
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
                Text("This will reset your calories burned counter to 0. This action cannot be undone.")
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
private fun ModernStepCounterCard(
    steps: Int,
    stepTarget: Int,
    userProfile: UserProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTargetCompleted = userProfile?.isStepTargetCompleted == true
    val hasTarget = stepTarget > 0
    
    val displaySteps = if (isTargetCompleted && steps >= stepTarget) {
        stepTarget
    } else {
        minOf(steps, stepTarget)
    }
    
    val progress = if (stepTarget > 0) (displaySteps.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f) else 0f
    
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.DirectionsWalk, 
                contentDescription = "Steps",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Show "Done" when target is completed, otherwise show step count
            if (isTargetCompleted && steps >= stepTarget) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50) // Green color for completed
                )
            } else {
                Text(
                    text = "$steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
            
            Text(
                text = "Steps",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2196F3).copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            // Show progress format below steps
            if (hasTarget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTargetCompleted && steps >= stepTarget) {
                        "$stepTarget/$stepTarget"
                    } else {
                        "${minOf(steps, stepTarget)}/$stepTarget"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTargetCompleted && steps >= stepTarget) {
                        Color(0xFF4CAF50) // Green when completed
                    } else {
                        Color(0xFF2196F3)
                    },
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set your\ntarget",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModernActivityStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = iconColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernBMICard(bmi: Float, category: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Increased height
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight() // Fill the card height
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // This will center content vertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BMI Icon from drawable
                Image(
                    painter = painterResource(id = R.drawable.bmi),
                    contentDescription = "BMI Icon",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 8.dp),
                    colorFilter = ColorFilter.tint(
                        when (category) {
                            "Underweight" -> Color(0xFF4CAF50)
                            "Normal" -> Color(0xFF2196F3)
                            "Overweight" -> Color(0xFFFF9800)
                            "Obese" -> Color(0xFFF44336)
                            else -> Color(0xFF9E9E9E)
                        }
                    )
                )
                
                // All text in a single line
                Text(
                    text = "BMI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = String.format("%.1f", bmi),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (category) {
                        "Underweight" -> Color(0xFF4CAF50)
                        "Normal" -> Color(0xFF2196F3)
                        "Overweight" -> Color(0xFFFF9800)
                        "Obese" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "View BMI Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ModernHealthAssistanceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .height(120.dp) // Double the height (was around 60dp, now 120dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:23.8103,90.4125?q=hospital+dhaka+bangladesh")
                    setPackage("com.google.android.apps.maps")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.google.com/maps/search/hospital+dhaka+bangladesh/@23.8103,90.4125,12z")
                    }
                    context.startActivity(webIntent)
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // White background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Map image that fits the entire card
        Image(
            painter = painterResource(R.drawable.map_image),
            contentDescription = "Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ModernHealthTipsCard(modifier: Modifier = Modifier) {
    val healthTips = listOf(
        "Daily Tip: Practice deep breathing for 5 minutes daily to reduce stress and improve overall wellness.",
        "Daily Tip: Take stairs instead of elevators when possible to boost your daily activity.",
        "Daily Tip: Drink at least 8 glasses of water throughout the day to stay properly hydrated.",
        "Daily Tip: Get 7-9 hours of quality sleep each night for optimal physical and mental health.",
        "Daily Tip: Take a 10-minute walk after meals to aid digestion and regulate blood sugar.",
        "Daily Tip: Eat a handful of nuts daily for healthy fats and protein to fuel your body.",
        "Daily Tip: Practice good posture while sitting and standing to prevent back pain.",
        "Daily Tip: Limit screen time before bed to improve sleep quality and reduce eye strain.",
        "Daily Tip: Include colorful fruits and vegetables in every meal for essential vitamins.",
        "Daily Tip: Do 5 minutes of stretching in the morning to increase flexibility and energy."
    )
    
    var currentTipIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }
    
    // Auto-cycle through tips every 4 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            isVisible = false
            delay(300) // Wait for fade out
            currentTipIndex = (currentTipIndex + 1) % healthTips.size
            isVisible = true
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFAFA) // Whitish background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Reduced padding from 16dp to 12dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp), // Reduced icon size from 40dp to 32dp
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFF9800) // Orange background for lightbulb
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Health Tip",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp), // Reduced padding from 8dp to 6dp
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing from 16dp to 12dp
            
            // Animated text with smooth transition
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300)
                ) + androidx.compose.animation.slideInHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(300),
                    initialOffsetX = { it / 3 }
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300)
                ) + androidx.compose.animation.slideOutHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(300),
                    targetOffsetX = { -it / 3 }
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = healthTips[currentTipIndex],
                    style = MaterialTheme.typography.bodySmall, // Changed from bodyMedium to bodySmall
                    color = Color(0xFF424242), // Dark gray text
                    lineHeight = 18.sp, // Reduced from 20.sp to 18.sp
                    fontSize = 13.sp // Explicit smaller font size for mobile
                )
            }
        }
    }
}

@Composable
private fun BMITipsModal(
    bmi: Float,
    category: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp)) // For balance
                    
                    Text(
                        text = "BMI Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // BMI Value and Category - Centered
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%.1f", bmi),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (category) {
                                "Underweight" -> Color(0xFF4CAF50)
                                "Normal" -> Color(0xFF2196F3)
                                "Overweight" -> Color(0xFFFF9800)
                                "Obese" -> Color(0xFFF44336)
                                else -> Color(0xFF9E9E9E)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $category",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Tips Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 Health Tips for You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Dynamic tips based on BMI category
                        val tips = getBMITips(category)
                        tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black.copy(alpha = 0.8f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Close Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2196F3)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Got it!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getBMITips(category: String): List<String> {
    return when (category) {
        "Underweight" -> listOf(
            "Eat nutrient-rich foods with healthy fats like nuts, avocados, and olive oil",
            "Include protein-rich foods like lean meats, fish, eggs, and legumes",
            "Strength training can help build muscle mass",
            "Eat smaller, more frequent meals throughout the day",
            "Consider consulting a nutritionist for a personalized meal plan"
        )
        "Normal" -> listOf(
            "Maintain your current healthy weight with balanced nutrition",
            "Continue regular physical activity (150 minutes per week)",
            "Eat a variety of fruits, vegetables, whole grains, and lean proteins",
            "Stay hydrated with 8-10 glasses of water daily",
            "Get 7-9 hours of quality sleep each night"
        )
        "Overweight" -> listOf(
            "Create a moderate calorie deficit through diet and exercise",
            "Focus on whole foods and reduce processed food intake",
            "Increase physical activity gradually to 300+ minutes per week",
            "Practice portion control and mindful eating",
            "Consider consulting a healthcare provider for guidance"
        )
        "Obese" -> listOf(
            "Consult with a healthcare provider for a comprehensive weight loss plan",
            "Start with low-impact exercises like walking or swimming",
            "Focus on sustainable dietary changes rather than extreme diets",
            "Consider working with a registered dietitian",
            "Monitor your progress and celebrate small victories"
        )
        else -> listOf(
            "Maintain a balanced diet with regular physical activity",
            "Stay hydrated and get adequate sleep",
            "Consider consulting a healthcare provider for personalized advice"
        )
    }
}
