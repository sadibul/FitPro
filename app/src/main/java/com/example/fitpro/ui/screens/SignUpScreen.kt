package com.example.fitpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    userDao: UserDao
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        )

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

        Button(
            onClick = { 
                coroutineScope.launch {
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                            return@launch
                        }
                        
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters long."
                            return@launch
                        }
                        
                        isLoading = true
                        errorMessage = ""
                        
                        try {
                            // Check if user already exists using IO dispatcher
                            val userExists = withContext(Dispatchers.IO) {
                                userDao.userExists(email.trim())
                            }
                            
                            if (userExists) {
                                errorMessage = "Email already exists. Please use a different email or try logging in."
                            } else {
                                // Email is available, proceed to questions
                                navController.navigate(Screen.Questions.createRoute(name.trim(), email.trim(), password))
                            }
                        } catch (e: Exception) {
                            errorMessage = "Sign up failed. Please try again."
                            e.printStackTrace() // Add logging to help debug
                        } finally {
                            isLoading = false
                        }
                    } else {
                        errorMessage = "Please fill in all fields."
                    }
                }
            },
            enabled = name.isNotEmpty() && email.isNotEmpty() && 
                     password.isNotEmpty() && confirmPassword.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(8.dp, RoundedCornerShape(25.dp)),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        TextButton(
            onClick = { navController.navigate(Screen.Login.route) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Already have an account? Login")
        }

        // Social signup buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { /* TODO: Implement Google signup */ },
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp))
            ) {
                Text("Google")
            }
            Button(
                onClick = { /* TODO: Implement Apple signup */ },
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp))
            ) {
                Text("Apple")
            }
        }
    }
}
