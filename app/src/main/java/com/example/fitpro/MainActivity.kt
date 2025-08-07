package com.example.fitpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
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

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Questions : Screen("questions")
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

        val database = AppDatabase.getDatabase(this)
        val userDao = database.userDao()
        val workoutPlanDao = database.workoutPlanDao()
        val mealPlanDao = database.mealPlanDao()

        setContent {
            FitProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitProApp(
                        userDao = userDao,
                        workoutPlanDao = workoutPlanDao,
                        mealPlanDao = mealPlanDao
                    )
                }
            }
        }
    }
}

@Composable
fun FitProApp(
    userDao: UserDao,
    workoutPlanDao: WorkoutPlanDao,
    mealPlanDao: MealPlanDao
) {
    val navController = rememberNavController()
    val userProfileFlow = remember { userDao.getUserProfile() }
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        MainAppWithBottomNav(
            userDao = userDao,
            workoutPlanDao = workoutPlanDao,
            mealPlanDao = mealPlanDao,
            userProfileFlow = userProfileFlow
        )
    } else {
        AuthNavigation(
            navController = navController,
            userDao = userDao,
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}

@Composable
fun AuthNavigation(
    navController: androidx.navigation.NavHostController,
    userDao: UserDao,
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
                onLoginSuccess = onLoginSuccess
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }
        composable(Screen.Questions.route) {
            QuestionsScreen(
                navController = navController,
                userDao = userDao,
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
    userProfileFlow: kotlinx.coroutines.flow.Flow<com.example.fitpro.data.UserProfile?>
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
                AccountScreen(
                    navController = navController,
                    userProfileFlow = userProfileFlow,
                    userDao = userDao
                )
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