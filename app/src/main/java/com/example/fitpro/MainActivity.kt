package com.example.fitpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitpro.data.AppDatabase
import com.example.fitpro.data.CompletedWorkoutDao
import com.example.fitpro.data.CompletedStepTargetDao
import com.example.fitpro.data.MealPlanDao
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.WorkoutPlanDao
import com.example.fitpro.ui.screens.*
import com.example.fitpro.ui.theme.FitProTheme
import com.example.fitpro.utils.StepCounterManager
import com.example.fitpro.utils.UserSession

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Questions : Screen("questions/{name}/{email}/{password}") {
        fun createRoute(name: String, email: String, password: String) = "questions/$name/$email/$password"
    }
    object Home : Screen("home")
    object BMIDetails : Screen("bmi_details")
    object Plan : Screen("plan")
    object WorkoutPlan : Screen("workout_plan")
    object MealPlan : Screen("meal_plan")
    object Progress : Screen("progress")
    object Account : Screen("account")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep splash screen for proper duration
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        // Hide splash screen after 1.5 seconds (reduced for better performance)
        lifecycleScope.launch {
            delay(1500)
            keepSplashScreen = false
        }

        setContent {
            FitProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitProApp()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Close database connections properly
        AppDatabase.closeDatabase()
    }
}

@Composable
fun FitProApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    
    var userDao by remember { mutableStateOf<UserDao?>(null) }
    var workoutPlanDao by remember { mutableStateOf<WorkoutPlanDao?>(null) }
    var mealPlanDao by remember { mutableStateOf<MealPlanDao?>(null) }
    var completedWorkoutDao by remember { mutableStateOf<CompletedWorkoutDao?>(null) }
    var completedStepTargetDao by remember { mutableStateOf<CompletedStepTargetDao?>(null) }
    var stepCounterManager by remember { mutableStateOf<StepCounterManager?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var initializationComplete by remember { mutableStateOf(false) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    // Initialize components asynchronously with proper error handling
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Initialize database with shorter timeout
                kotlinx.coroutines.withTimeout(5000) { // 5 second timeout
                    val database = AppDatabase.getDatabase(context)
                    
                    // Initialize DAOs
                    userDao = database.userDao()
                    workoutPlanDao = database.workoutPlanDao()
                    mealPlanDao = database.mealPlanDao()
                    completedWorkoutDao = database.completedWorkoutDao()
                    completedStepTargetDao = database.completedStepTargetDao()
                    
                    // Initialize step counter manager
                    stepCounterManager = StepCounterManager(context)
                }
                
                // Update UI on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    initializationComplete = true
                    initializationError = null
                }
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Handle timeout - try once more with fresh database
                try {
                    context.deleteDatabase("fitpro_database")
                    delay(500) // Brief delay before retry
                    
                    val database = AppDatabase.getDatabase(context)
                    userDao = database.userDao()
                    workoutPlanDao = database.workoutPlanDao()
                    mealPlanDao = database.mealPlanDao()
                    completedWorkoutDao = database.completedWorkoutDao()
                    completedStepTargetDao = database.completedStepTargetDao()
                    stepCounterManager = StepCounterManager(context)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoading = false
                        initializationComplete = true
                        initializationError = null
                    }
                } catch (retryException: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoading = false
                        initializationComplete = false
                        initializationError = "Database initialization failed. Please restart the app."
                    }
                }
            } catch (e: Exception) {
                // Handle other initialization errors
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    initializationComplete = false
                    initializationError = "Failed to initialize app: ${e.message}"
                }
            }
        }
    }

    when {
        isLoading -> {
            // Show loading screen during initialization
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Starting FitPro...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        initializationError != null -> {
            // Show error screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = initializationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Restart initialization
                            isLoading = true
                            initializationError = null
                            initializationComplete = false
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        
        initializationComplete && userDao != null && workoutPlanDao != null && mealPlanDao != null && completedWorkoutDao != null && completedStepTargetDao != null && stepCounterManager != null -> {
            val userSession = remember { UserSession(context) }
            var sessionChecked by remember { mutableStateOf(false) }
            
            // Reset sessionChecked when user logs out to ensure fresh auth flow
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    sessionChecked = true // Immediately mark as checked when logged out
                }
            }
            
            // Check session state on startup with background thread
            LaunchedEffect(Unit) {
                if (!sessionChecked) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        delay(300) // Brief delay for UI stability
                        val currentEmail = userSession.getCurrentUserEmail()
                        val shouldRemember = userSession.shouldRememberUser()
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isLoggedIn = currentEmail != null && shouldRemember
                            sessionChecked = true
                        }
                    }
                }
            }
            
            if (!sessionChecked) {
                // Show minimal loading while checking session
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                val currentUserEmail = userSession.getCurrentUserEmail()
                val shouldRememberUser = userSession.shouldRememberUser()
                val userProfileFlow = remember(currentUserEmail) { 
                    if (currentUserEmail != null) {
                        userDao!!.getUserProfile(currentUserEmail)
                    } else {
                        kotlinx.coroutines.flow.flowOf(null)
                    }
                }
                
                // Show main app if user is logged in or should be remembered
                if (isLoggedIn || (shouldRememberUser && currentUserEmail != null)) {
                    MainAppWithBottomNav(
                        userDao = userDao!!,
                        workoutPlanDao = workoutPlanDao!!,
                        mealPlanDao = mealPlanDao!!,
                        completedWorkoutDao = completedWorkoutDao!!,
                        completedStepTargetDao = completedStepTargetDao!!,
                        stepCounterManager = stepCounterManager!!,
                        userEmail = currentUserEmail ?: "",
                        onLogout = { 
                            // Handle logout by clearing all data and session
                            stepCounterManager!!.clearUserData()
                            // Also clear the session in case it wasn't called from AccountScreen
                            val userSession = UserSession(context)
                            userSession.logout()
                            isLoggedIn = false
                        }
                    )
                } else {
                    // Use key to reset navigation state on logout
                    key(isLoggedIn) {
                        AuthNavigation(
                            navController = navController,
                            userDao = userDao!!,
                            userSession = userSession,
                            onLoginSuccess = { isLoggedIn = true }
                        )
                    }
                }
            }
        }
        else -> {
            // Show error screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to initialize database")
            }
        }
    }
}

@Composable
fun AuthNavigation(
    navController: androidx.navigation.NavHostController,
    userDao: UserDao,
    userSession: UserSession,
    onLoginSuccess: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = Modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                userSession = userSession,
                userDao = userDao,
                onLoginSuccess = onLoginSuccess
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                userDao = userDao
            )
        }
        composable(Screen.Questions.route) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            QuestionsScreen(
                navController = navController,
                userDao = userDao,
                userName = name,
                userEmail = email,
                userPassword = password,
                userSession = userSession,
                onQuestionsComplete = onLoginSuccess
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppWithBottomNav(
    userDao: UserDao,
    workoutPlanDao: WorkoutPlanDao,
    mealPlanDao: MealPlanDao,
    completedWorkoutDao: CompletedWorkoutDao,
    completedStepTargetDao: CompletedStepTargetDao,
    stepCounterManager: StepCounterManager,
    userEmail: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userSession = remember { UserSession(context) }
    val userProfileFlow = remember(userEmail) {
        userDao.getUserProfile(userEmail)
    }
    
    // Set current user for step tracking when component loads
    LaunchedEffect(userEmail) {
        stepCounterManager.setCurrentUser(userEmail)
    }
    
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // State to trigger refresh for each screen
    var homeRefreshKey by remember { mutableStateOf(0) }
    var planRefreshKey by remember { mutableStateOf(0) }
    var progressRefreshKey by remember { mutableStateOf(0) }
    var accountRefreshKey by remember { mutableStateOf(0) }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            icon = Icons.Default.Home,
            label = "Home"
        ),
        BottomNavItem(
            route = Screen.Plan.route,
            icon = Icons.Default.FitnessCenter,
            label = "Plan"
        ),
        BottomNavItem(
            route = Screen.Progress.route,
            icon = Icons.Default.TrendingUp,
            label = "Progress"
        ),
        BottomNavItem(
            route = Screen.Account.route,
            icon = Icons.Default.Person,
            label = "Account"
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            if (currentDestination?.route == item.route) {
                                // Same tab tapped - trigger refresh
                                when (item.route) {
                                    Screen.Home.route -> homeRefreshKey++
                                    Screen.Plan.route -> planRefreshKey++
                                    Screen.Progress.route -> progressRefreshKey++
                                    Screen.Account.route -> accountRefreshKey++
                                }
                            } else {
                                // Different tab - navigate normally
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        key(userSession.getCurrentUserEmail()) { // Add key to reset navigation state
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
            composable(Screen.Home.route) {
                key(homeRefreshKey) { // Add refresh key to force recomposition
                    HomeScreen(
                        navController = navController,
                        userProfileFlow = userProfileFlow,
                        userDao = userDao,
                        workoutPlanDao = workoutPlanDao,
                        mealPlanDao = mealPlanDao,
                        completedWorkoutDao = completedWorkoutDao,
                        completedStepTargetDao = completedStepTargetDao,
                        stepCounterManager = stepCounterManager,
                        onBMICardClick = { navController.navigate(Screen.BMIDetails.route) }
                    )
                }
            }
            composable(Screen.Plan.route) {
                key(planRefreshKey) { // Add refresh key to force recomposition
                    PlanScreen(
                        navController = navController,
                        userProfileFlow = userProfileFlow,
                        userDao = userDao
                    )
                }
            }
            composable(Screen.Progress.route) {
                key(progressRefreshKey) { // Add refresh key to force recomposition
                    ProgressScreen(
                        navController = navController,
                        userProfileFlow = userProfileFlow,
                        workoutPlanDao = workoutPlanDao,
                        mealPlanDao = mealPlanDao,
                        completedWorkoutDao = completedWorkoutDao,
                        completedStepTargetDao = completedStepTargetDao
                    )
                }
            }
            composable(Screen.Account.route) {
                key(accountRefreshKey) { // Add refresh key to force recomposition
                    AccountScreen(
                        navController = navController,
                        userDao = userDao,
                        currentUserEmail = userEmail,
                        userSession = userSession,
                        onLogout = onLogout
                    )
                }
            }
            composable(Screen.BMIDetails.route) {
                BMIDetailsScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow
                )
            }
            composable(Screen.WorkoutPlan.route) {
                WorkoutPlanScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow,
                    workoutPlanDao = workoutPlanDao
                )
            }
            composable(Screen.MealPlan.route) {
                MealPlanScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow,
                    mealPlanDao = mealPlanDao,
                    userDao = userDao
                )
            }
        }
        } // Close key block
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)