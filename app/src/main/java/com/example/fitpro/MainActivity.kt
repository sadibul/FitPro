package com.example.fitpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitpro.data.AppDatabase
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
        
        // Keep splash screen longer
        splashScreen.setKeepOnScreenCondition { 
            // Keep splash for at least 1 second
            false 
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
}

@Composable
fun FitProApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    
    var userDao by remember { mutableStateOf<UserDao?>(null) }
    var workoutPlanDao by remember { mutableStateOf<WorkoutPlanDao?>(null) }
    var mealPlanDao by remember { mutableStateOf<MealPlanDao?>(null) }
    var stepCounterManager by remember { mutableStateOf<StepCounterManager?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Initialize database asynchronously in background thread
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                userDao = database.userDao()
                workoutPlanDao = database.workoutPlanDao()
                mealPlanDao = database.mealPlanDao()
                stepCounterManager = StepCounterManager(context)
            } catch (e: Exception) {
                // Handle database initialization error
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    when {
        isLoading -> {
            // Show loading screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        userDao != null && workoutPlanDao != null && mealPlanDao != null && stepCounterManager != null -> {
            val userSession = remember { UserSession(context) }
            
            // Check session state on startup
            LaunchedEffect(Unit) {
                val currentEmail = userSession.getCurrentUserEmail()
                val shouldRemember = userSession.shouldRememberUser()
                isLoggedIn = currentEmail != null && shouldRemember
            }
            
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
                    stepCounterManager = stepCounterManager!!,
                    userProfileFlow = userProfileFlow,
                    userSession = userSession
                )
            } else {
                AuthNavigation(
                    navController = navController,
                    userDao = userDao!!,
                    userSession = userSession,
                    onLoginSuccess = { isLoggedIn = true }
                )
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
    stepCounterManager: StepCounterManager,
    userProfileFlow: kotlinx.coroutines.flow.Flow<com.example.fitpro.data.UserProfile?>,
    userSession: UserSession
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow,
                    userDao = userDao,
                    workoutPlanDao = workoutPlanDao,
                    stepCounterManager = stepCounterManager,
                    onBMICardClick = { navController.navigate(Screen.BMIDetails.route) }
                )
            }
            composable(Screen.Plan.route) {
                PlanScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow,
                    workoutPlanDao = workoutPlanDao,
                    mealPlanDao = mealPlanDao
                )
            }
            composable(Screen.Account.route) {
                val currentUserEmail = userSession.getCurrentUserEmail()
                if (currentUserEmail != null) {
                    AccountScreen(
                        navController = navController,
                        userDao = userDao,
                        currentUserEmail = currentUserEmail,
                        userSession = userSession
                    )
                } else {
                    // If no user logged in, navigate to login
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                    mealPlanDao = mealPlanDao
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)