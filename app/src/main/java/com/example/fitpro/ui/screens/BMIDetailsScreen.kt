package com.example.fitpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.data.UserProfile
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMIDetailsScreen(
    navController: NavController,
    userProfileFlow: Flow<UserProfile?>
) {
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val bmi = userProfile?.calculateBMI() ?: 0f
    val category = userProfile?.getBMICategory() ?: "Unknown"
    val tips = userProfile?.getBMITips() ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMI Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // BMI Value Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your BMI",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format("%.1f", bmi),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge,
                        color = getBMICategoryColor(category)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BMI Scale
            Text(
                text = "BMI Categories:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            BMIScaleCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Personalized Tips
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Personalized Tips",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tips,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Additional Information
            if (category != "Normal") {
                Spacer(modifier = Modifier.height(16.dp))
                SuggestedActionsCard(category)
            }
        }
    }
}

@Composable
private fun BMIScaleCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            BMICategoryRow("Underweight", "Below 18.5")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            BMICategoryRow("Normal", "18.5 - 24.9")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            BMICategoryRow("Overweight", "25.0 - 29.9")
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            BMICategoryRow("Obese", "30.0 or greater")
        }
    }
}

@Composable
private fun BMICategoryRow(category: String, range: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = category)
        Text(text = range)
    }
}

@Composable
private fun SuggestedActionsCard(category: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Suggested Actions",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (category) {
                "Underweight" -> UnderweightActions()
                "Overweight", "Obese" -> OverweightActions()
            }
        }
    }
}

@Composable
private fun UnderweightActions() {
    Column {
        Text("• Increase caloric intake with nutrient-rich foods")
        Text("• Add protein-rich foods to your diet")
        Text("• Include healthy fats like nuts and avocados")
        Text("• Start strength training exercises")
        Text("• Consider consulting a nutritionist")
    }
}

@Composable
private fun OverweightActions() {
    Column {
        Text("• Create a sustainable caloric deficit")
        Text("• Increase physical activity gradually")
        Text("• Focus on whole, unprocessed foods")
        Text("• Stay hydrated and get adequate sleep")
        Text("• Track your progress regularly")
    }
}

@Composable
private fun getBMICategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category) {
        "Normal" -> MaterialTheme.colorScheme.primary
        "Underweight" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}
