package com.example.fitpro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fitpro.R
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.ImageUtils
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    userDao: UserDao,
    currentUserEmail: String,
    userSession: UserSession,
    userProfileFlow: Flow<UserProfile?>,
    onLogout: () -> Unit
) {
    val userProfile by userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    
    // Ensure profile image exists in internal storage
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            // If the profile has an image URI but the file doesn't exist, try to find it
            if (profile.profileImageUri != null) {
                val file = java.io.File(profile.profileImageUri!!)
                if (!file.exists()) {
                    // Try to find the image in our internal storage
                    val savedImageUri = ImageUtils.getUserProfileImageUri(context, currentUserEmail)
                    if (savedImageUri != null && savedImageUri != profile.profileImageUri) {
                        // Update the profile with the correct URI
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val updatedProfile = profile.copy(profileImageUri = savedImageUri)
                                userDao.updateUser(updatedProfile)
                                android.util.Log.d("AccountScreen", "Profile image URI corrected: $savedImageUri")
                            } catch (e: Exception) {
                                android.util.Log.e("AccountScreen", "Error correcting profile image URI", e)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Profile image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Update profile image in database immediately
            userProfile?.let { profile ->
                coroutineScope.launch {
                    try {
                        // Copy image to internal storage
                        val savedImageUri = ImageUtils.copyImageToInternalStorage(
                            context = context,
                            sourceUri = selectedUri,
                            userEmail = currentUserEmail
                        )
                        
                        if (savedImageUri != null) {
                            // Update profile with the saved image URI
                            val updatedProfile = profile.copy(profileImageUri = savedImageUri)
                            withContext(Dispatchers.IO) {
                                userDao.updateUser(updatedProfile)
                            }
                            android.util.Log.d("AccountScreen", "Profile image updated successfully: $savedImageUri")
                        } else {
                            android.util.Log.e("AccountScreen", "Failed to save image to internal storage")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AccountScreen", "Error updating profile image", e)
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Simple header with floating card effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    clip = false
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 20.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Content area with simple background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // Enhanced Profile Picture Section
            ProfileImageSection(
                profileImageUri = userProfile?.profileImageUri,
                onImageClick = { imagePickerLauncher.launch("image/*") }
            )
            
            // Compact User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userProfile?.name ?: "User Name",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1F2937)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = userProfile?.email ?: currentUserEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Stats Cards Row
            StatsCardsRow(userProfile = userProfile)
            
            // Premium Card
            PremiumCard()
            
            // Edit Profile Button
            EditProfileButton(onEditClick = { showEditProfileDialog = true })
            
            // Notification Toggle
            NotificationCard()
            
            // Settings Button
            SettingsButton()
            
            // Logout Button
            LogoutButton(onLogoutClick = {
                try {
                    // Clear session first
                    userSession.logout()
                    // Then trigger the logout callback
                    onLogout()
                    android.util.Log.d("AccountScreen", "User logged out successfully")
                } catch (e: Exception) {
                    android.util.Log.e("AccountScreen", "Error during logout", e)
                    // Still trigger logout even if there's an error
                    onLogout()
                }
            })
        }
    }
    
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, age, height, weight ->
                userProfile?.let { profile ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val updatedProfile = profile.copy(
                                name = name,
                                age = age,
                                height = height.toInt(),
                                weight = weight
                            )
                            userDao.updateUser(updatedProfile)
                            withContext(Dispatchers.Main) {
                                showEditProfileDialog = false
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AccountScreen", "Error updating profile", e)
                            withContext(Dispatchers.Main) {
                                showEditProfileDialog = false
                            }
                        }
                    }
                }
            }
        )
    }
}


@Composable
private fun ProfileImageSection(
    profileImageUri: String?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5DADE2),
                        Color(0xFF3498DB),
                        Color(0xFF2980B9)
                    )
                ),
                shape = CircleShape
            )
            .padding(4.dp)
            .clickable { onImageClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground), // Fallback on error
                    onError = { 
                        android.util.Log.e("ProfileImage", "Failed to load image: $profileImageUri")
                    }
                )
            } else {
                // Enhanced default profile with gradient background and icon
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD),
                                    Color(0xFFBBDEFB)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile",
                        tint = Color(0xFF3498DB),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable 
private fun StatsCardsRow(userProfile: UserProfile?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                value = "${userProfile?.weight?.toInt() ?: 0}",
                label = "Weight",
                gradientColors = listOf(Color(0xFF4A90E2), Color(0xFF357ABD))
            )
            StatCard(
                value = "${userProfile?.height?.toInt() ?: 0}",
                label = "Height",
                gradientColors = listOf(Color(0xFF5CB85C), Color(0xFF449D44))
            )
            StatCard(
                value = "${userProfile?.age ?: 0}",
                label = "Years",
                gradientColors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A52))
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    gradientColors: List<Color>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.split(" ")[0], // Only show the number, remove units
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PremiumCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Premium action */ },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced Pro badge with gradient
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pro",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Go Premium &",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "Unlock More!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Access premium features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
            
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF3498DB).copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF3498DB),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileButton(onEditClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.weight(1f)
            )
            
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF4A90E2).copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationCard() {
    var notificationEnabled by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.weight(1f)
            )
            
            Switch(
                checked = notificationEnabled,
                onCheckedChange = { notificationEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4A90E2),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
    }
}

@Composable
private fun SettingsButton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle settings click */ },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.weight(1f)
            )
            
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF9C27B0).copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(onLogoutClick: () -> Unit) {
    var isLoggingOut by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isLoggingOut) {
                    isLoggingOut = true
                    onLogoutClick()
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoggingOut) Color.Gray.copy(alpha = 0.3f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        border = BorderStroke(2.dp, Color(0xFFFF5757))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoggingOut) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF3498DB),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Logging out...",
                        color = Color(0xFF3498DB),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "Log Out",
                    color = Color(0xFFFF5757),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Float, Float) -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var age by remember { mutableStateOf(userProfile?.age?.toString() ?: "") }
    var height by remember { mutableStateOf(userProfile?.height?.toString() ?: "") }
    var weight by remember { mutableStateOf(userProfile?.weight?.toString() ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.05f)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color.Black.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                    label = { Text("Age", color = Color.Black.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) height = it },
                    label = { Text("Height (cm)", color = Color.Black.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) weight = it },
                    label = { Text("Weight (kg)", color = Color.Black.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.2f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                val ageInt = age.toIntOrNull() ?: 0
                                val heightFloat = height.toFloatOrNull() ?: 0f
                                val weightFloat = weight.toFloatOrNull() ?: 0f
                                onSave(name, ageInt, heightFloat, weightFloat)
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF4A90E2),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Save",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
