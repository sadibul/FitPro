package com.example.fitpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Tell us about yourself",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.padding(top = 32.dp, bottom = 48.dp),
            textAlign = TextAlign.Center
        )

        // Gender Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F4F6)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Gender",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = gender == "Male",
                            onClick = { gender = "Male" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF6366F1)
                            )
                        )
                        Text(
                            "Male",
                            fontSize = 16.sp,
                            color = Color(0xFF374151)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = gender == "Female",
                            onClick = { gender = "Female" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF6366F1)
                            )
                        )
                        Text(
                            "Female",
                            fontSize = 16.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }
        }

        // Age Input
        OutlinedTextField(
            value = age,
            onValueChange = { if (it.length <= 3) age = it.filter { char -> char.isDigit() } },
            placeholder = { Text("Age", color = Color(0xFF9CA3AF)) },
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
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )

        // Weight Input
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
            placeholder = { Text("Weight (kg)", color = Color(0xFF9CA3AF)) },
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
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )

        // Height Input
        OutlinedTextField(
            value = height,
            onValueChange = { height = it.filter { char -> char.isDigit() } },
            placeholder = { Text("Height (cm)", color = Color(0xFF9CA3AF)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button with Gradient
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val userProfile = UserProfile(
                            email = userEmail,
                            name = userName,
                            password = userPassword,
                            gender = gender,
                            age = age.toIntOrNull() ?: 25,
                            weight = weight.toFloatOrNull() ?: 70f,
                            height = height.toIntOrNull() ?: 170
                        )
                        
                        // Insert user using IO dispatcher
                        withContext(Dispatchers.IO) {
                            userDao.insertUser(userProfile)
                        }
                        
                        // Save user session without remember me for new users
                        userSession.saveUserSession(userEmail, false)
                        
                        // Complete on main thread
                        onQuestionsComplete()
                        
                    } catch (e: Exception) {
                        e.printStackTrace() // Add logging for debugging
                        // Handle error - still complete to avoid getting stuck
                        onQuestionsComplete()
                    }
                }
            },
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
            ),
            enabled = gender.isNotEmpty() && age.isNotEmpty() && 
                     weight.isNotEmpty() && height.isNotEmpty()
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
