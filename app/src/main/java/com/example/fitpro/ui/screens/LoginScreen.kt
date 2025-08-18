package com.example.fitpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    userSession: UserSession,
    userDao: UserDao,
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "FitPro",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Welcome back! Let's get fit together",
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Email Input Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Email, 
                    contentDescription = "Email",
                    tint = Color(0xFF6B7280)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = "Password",
                    tint = Color(0xFF6B7280)
                )
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = Color(0xFF6B7280)
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Remember Me and Forgot Password Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF6366F1)
                    )
                )
                Text(
                    "Remember me",
                    color = Color(0xFF374151),
                    fontSize = 14.sp
                )
            }
            TextButton(onClick = { /* TODO: Implement forgot password */ }) {
                Text(
                    "Forgot Password?",
                    color = Color(0xFF6366F1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Error message display
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp
                )
            }
        }

        // Login Button with Gradient
        Button(
            onClick = { 
                coroutineScope.launch {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        errorMessage = ""
                        
                        try {
                            // Authenticate user against database using IO dispatcher
                            val user = withContext(Dispatchers.IO) {
                                userDao.authenticateUser(email.trim(), password)
                            }
                            
                            if (user != null) {
                                // Login successful
                                userSession.saveUserSession(email.trim(), rememberMe)
                                onLoginSuccess()
                            } else {
                                // Check if user exists but wrong password
                                val userExists = withContext(Dispatchers.IO) {
                                    userDao.userExists(email.trim())
                                }
                                
                                if (userExists) {
                                    errorMessage = "Incorrect password. Please try again."
                                } else {
                                    errorMessage = "Account not found. Please sign up first."
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Login failed. Please try again."
                            e.printStackTrace() // Add logging for debugging
                        } finally {
                            isLoading = false
                        }
                    } else {
                        errorMessage = "Please fill in all fields."
                    }
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Login",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE5E7EB)
            )
            Text(
                text = "  OR  ",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE5E7EB)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Don't have account text
        TextButton(
            onClick = { navController.navigate(Screen.SignUp.route) },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Don't have an account? Sign Up",
                color = Color(0xFF6366F1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Social login buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { /* TODO: Implement Google login */ },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Google",
                    color = Color(0xFF374151),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            OutlinedButton(
                onClick = { /* TODO: Implement Apple login */ },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = "Apple",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Apple",
                    color = Color(0xFF374151),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
