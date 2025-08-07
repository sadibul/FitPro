package com.example.fitpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.UserProfile
import kotlinx.coroutines.flow.Flow

@Composable
fun HomeScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>,
    onBMICardClick: () -> Unit
) {
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Section
        WelcomeSection(userProfile?.name ?: "User")

        Spacer(modifier = Modifier.height(24.dp))

        // Current Plan Section
        CurrentPlanCard(userProfile?.currentPlan ?: "Weight Loss Plan")

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Stats Section
        ActivityStatsSection(
            steps = userProfile?.dailySteps ?: 0,
            calories = userProfile?.caloriesBurned ?: 0,
            heartRate = userProfile?.heartRate ?: 0
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BMI Section
        BMICard(
            bmi = userProfile?.calculateBMI() ?: 0f,
            category = userProfile?.getBMICategory() ?: "Unknown",
            onClick = onBMICardClick
        )
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
private fun CurrentPlanCard(planName: String) {
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
}

@Composable
private fun ActivityStatsSection(steps: Int, calories: Int, heartRate: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ActivityStatCard(
            icon = Icons.Default.DirectionsWalk,
            value = "$steps",
            label = "Steps",
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
