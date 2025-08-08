package com.example.fitpro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.UserProfile
import kotlinx.coroutines.delay
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

        Spacer(modifier = Modifier.height(16.dp))

        // Health Tips Section
        HealthTipsCard()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Medical Assistance Card - takes remaining space
        MedicalAssistanceCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // This will take all remaining space
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
