package com.example.fitpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    userDao: UserDao,
    userName: String,
    userEmail: String,
    userSession: UserSession,
    onQuestionsComplete: () -> Unit = {}
) {
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        // Gender Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = gender == "Male",
                        onClick = { gender = "Male" }
                    )
                    Text("Male")
                    RadioButton(
                        selected = gender == "Female",
                        onClick = { gender = "Female" }
                    )
                    Text("Female")
                }
            }
        }

        // Age Input
        OutlinedTextField(
            value = age,
            onValueChange = { if (it.length <= 3) age = it.filter { char -> char.isDigit() } },
            label = { Text("Age") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )

        // Weight Input
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Weight (kg)") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )

        // Height Input
        OutlinedTextField(
            value = height,
            onValueChange = { height = it.filter { char -> char.isDigit() } },
            label = { Text("Height (cm)") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val userProfile = UserProfile(
                            email = userEmail,
                            name = userName,
                            gender = gender,
                            age = age.toIntOrNull() ?: 25,
                            weight = weight.toFloatOrNull() ?: 70f,
                            height = height.toIntOrNull() ?: 170
                        )
                        userDao.insertOrUpdateUser(userProfile)
                        
                        // Save user session
                        userSession.saveUserSession(userEmail)
                        
                        onQuestionsComplete()
                    } catch (e: Exception) {
                        // Handle error - still complete to avoid getting stuck
                        onQuestionsComplete()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(8.dp, RoundedCornerShape(25.dp)),
            shape = RoundedCornerShape(25.dp),
            enabled = gender.isNotEmpty() && age.isNotEmpty() && 
                     weight.isNotEmpty() && height.isNotEmpty()
        ) {
            Text("Continue")
        }
    }
}
