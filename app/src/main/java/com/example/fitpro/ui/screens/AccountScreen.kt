package com.example.fitpro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    userDao: UserDao,
    currentUserEmail: String,
    userSession: UserSession
) {
    val userFlow: Flow<UserProfile?> = remember(currentUserEmail) {
        userDao.getUserProfile(currentUserEmail)
    }
    val userProfile by userFlow.collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showUpdateStatsDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        ProfileHeaderCard(
            userProfile = userProfile,
            onEditClick = { showEditProfileDialog = true }
        )
        
        // Stats Update Card
        StatsUpdateCard(
            userProfile = userProfile,
            onUpdateClick = { showUpdateStatsDialog = true }
        )
        
        // Settings Options
        SettingsOptionsCard(
            onLogoutClick = {
                userSession.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )
        
        // App Info
        AppInfoCard()
    }
    
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedProfile ->
                coroutineScope.launch {
                    userDao.insertOrUpdateUser(updatedProfile)
                    showEditProfileDialog = false
                }
            }
        )
    }
    
    // Update Stats Dialog  
    if (showUpdateStatsDialog) {
        UpdateStatsDialog(
            userProfile = userProfile,
            onDismiss = { showUpdateStatsDialog = false },
            onSave = { weight, height, age ->
                userProfile?.let { profile ->
                    coroutineScope.launch {
                        val updatedProfile = profile.copy(
                            weight = weight,
                            height = height,
                            age = age
                        )
                        userDao.insertOrUpdateUser(updatedProfile)
                        showUpdateStatsDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    userProfile: UserProfile?,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { /* Handle profile picture change */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userProfile?.name ?: "User Name",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = userProfile?.email ?: "user@email.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // BMI Display
            userProfile?.let { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BMI: ${String.format("%.1f", profile.calculateBMI())}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = profile.getBMICategory(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun StatsUpdateCard(
    userProfile: UserProfile?,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Physical Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            userProfile?.let { profile ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(
                        label = "Weight",
                        value = "${profile.weight} kg",
                        icon = Icons.Default.MonitorWeight
                    )
                    StatItem(
                        label = "Height", 
                        value = "${profile.height} cm",
                        icon = Icons.Default.Height
                    )
                    StatItem(
                        label = "Age",
                        value = "${profile.age} years",
                        icon = Icons.Default.Cake
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onUpdateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Update, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Stats")
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SettingsOptionsCard(
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem(
                icon = Icons.Default.Bedtime,
                title = "Sleep Schedule",
                subtitle = "Set your sleep schedule",
                onClick = { /* Handle sleep schedule */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Manage your notifications",
                onClick = { /* Handle notifications */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Choose your theme",
                onClick = { /* Handle theme */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Security,
                title = "Privacy",
                subtitle = "Privacy settings",
                onClick = { /* Handle privacy */ }
            )
            
            SettingsItem(
                icon = Icons.Default.ExitToApp,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "App Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Learn more about FitPro",
                onClick = { /* Handle about */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                subtitle = "Get help and support",
                onClick = { /* Handle help */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Star,
                title = "Rate App",
                subtitle = "Rate us on Play Store",
                onClick = { /* Handle rating */ }
            )
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var email by remember { mutableStateOf(userProfile?.email ?: "") }
    var currentPlan by remember { mutableStateOf(userProfile?.currentPlan ?: "Weight Loss Plan") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currentPlan,
                    onValueChange = { currentPlan = it },
                    label = { Text("Current Plan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    userProfile?.let { profile ->
                        onSave(
                            profile.copy(
                                name = name,
                                email = email,
                                currentPlan = currentPlan
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateStatsDialog(
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (Float, Int, Int) -> Unit
) {
    var weight by remember { mutableStateOf(userProfile?.weight?.toString() ?: "") }
    var height by remember { mutableStateOf(userProfile?.height?.toString() ?: "") }
    var age by remember { mutableStateOf(userProfile?.age?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Physical Stats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age (years)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weightFloat = weight.toFloatOrNull() ?: return@Button
                    val heightInt = height.toIntOrNull() ?: return@Button
                    val ageInt = age.toIntOrNull() ?: return@Button
                    onSave(weightFloat, heightInt, ageInt)
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
