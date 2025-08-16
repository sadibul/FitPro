package com.example.fitpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    userDao: UserDao,
    userName: String,
    userEmail: String,
    userPassword: String,
    userSession: UserSession,
    onQuestionsComplete: () -> Unit = {}
) {
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var registrationComplete by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Handle navigation when registration is complete
    LaunchedEffect(registrationComplete) {
        if (registrationComplete) {
            // Try the callback first
            onQuestionsComplete()
            
            // Give callback a chance to work
            kotlinx.coroutines.delay(500)
            
            // If we're still here, force activity restart for reliable navigation
            val activity = context as? Activity
            activity?.let {
                val intent = it.intent
                it.finish()
                it.startActivity(intent)
            }
        }
    }

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

        // Error message display
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        val allFieldsFilled = gender.isNotEmpty() && age.isNotEmpty() && weight.isNotEmpty() && height.isNotEmpty()
        
        Button(
            onClick = {
                if (allFieldsFilled) {
                    isLoading = true
                    errorMessage = ""
                    
                    coroutineScope.launch {
                        try {
                            val ageInt = age.toIntOrNull()
                            val weightFloat = weight.toFloatOrNull()
                            val heightInt = height.toIntOrNull()
                            
                            // Validate input
                            if (ageInt == null || ageInt <= 0 || ageInt > 120) {
                                errorMessage = "Please enter a valid age (1-120)"
                                isLoading = false
                                return@launch
                            }
                            
                            if (weightFloat == null || weightFloat <= 0 || weightFloat > 500) {
                                errorMessage = "Please enter a valid weight (1-500 kg)"
                                isLoading = false
                                return@launch
                            }
                            
                            if (heightInt == null || heightInt <= 0 || heightInt > 300) {
                                errorMessage = "Please enter a valid height (1-300 cm)"
                                isLoading = false
                                return@launch
                            }
                            
                            val userProfile = UserProfile(
                                email = userEmail,
                                name = userName,
                                password = userPassword,
                                gender = gender,
                                age = ageInt,
                                weight = weightFloat,
                                height = heightInt
                            )
                            
                            // Database operations
                            withContext(Dispatchers.IO) {
                                try {
                                    // Check if user exists and delete if incomplete registration
                                    val existingUser = userDao.getUserByEmail(userEmail)
                                    if (existingUser != null) {
                                        userDao.deleteUser(userEmail)
                                    }
                                    
                                    // Insert the new user
                                    userDao.insertUser(userProfile)
                                } catch (e: Exception) {
                                    throw e
                                }
                            }
                            
                            // Save user session with remember me enabled for new users
                            // Add small delay to ensure database operations are complete
                            kotlinx.coroutines.delay(200)
                            userSession.saveUserSession(userEmail, true)
                            
                            // Complete registration and navigate to main app immediately
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                registrationComplete = true
                                // Show success message
                                errorMessage = "Registration successful! Navigating..."
                                // Also call the callback directly
                                onQuestionsComplete()
                            }
                            
                        } catch (e: Exception) {
                            errorMessage = "Registration failed: ${e.message ?: "Unknown error"}"
                            isLoading = false
                        }
                    }
                } else {
                    errorMessage = "Please fill in all fields"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(8.dp, RoundedCornerShape(25.dp)),
            shape = RoundedCornerShape(25.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allFieldsFilled) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (allFieldsFilled) "Continue" else "Fill all fields to continue",
                    color = if (allFieldsFilled) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
