package com.example.fitpro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fitpro.Screen
import com.example.fitpro.data.UserDao
import com.example.fitpro.data.UserProfile
import com.example.fitpro.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    userDao: UserDao,
    currentUserEmail: String,
    userSession: UserSession,
    onLogout: () -> Unit
) {
    val userFlow: Flow<UserProfile?> = remember(currentUserEmail) {
        userDao.getUserProfile(currentUserEmail)
    }
    val userProfile by userFlow.collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar
        TopNavigationBar(
            onBackClick = { navController.popBackStack() },
            onSettingsClick = { /* Handle settings */ }
        )
        
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Profile Section
            ProfileSection(
                userProfile = userProfile,
                userDao = userDao,
                onEditClick = { showEditProfileDialog = true }
            )
            
            // Stats Cards Row
            StatsCardsRow(userProfile = userProfile)
            
            // Premium Section
            PremiumSection()
            
            // Menu Items
            MenuItemsSection(
                onEditProfileClick = { showEditProfileDialog = true },
                onNotificationsClick = { /* Handle notifications */ },
                onSettingsClick = { /* Handle settings */ }
            )
            
            // Logout Button
            LogoutButton(
                onLogoutClick = {
                    userSession.logout()
                    onLogout()
                }
            )
        }
    }
    
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedProfile ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        userDao.updateUser(updatedProfile)
                        withContext(Dispatchers.Main) {
                            showEditProfileDialog = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showEditProfileDialog = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TopNavigationBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileSection(
    userProfile: UserProfile?,
    userDao: UserDao,
    onEditClick: () -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize with current profile image URI
    LaunchedEffect(userProfile?.profileImageUri) {
        selectedImageUri = userProfile?.profileImageUri?.let { Uri.parse(it) }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Update profile immediately with new image
            userProfile?.let { profile ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val updatedProfile = profile.copy(profileImageUri = it.toString())
                        userDao.updateUser(updatedProfile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Profile Picture with Border (similar to green border in image)
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userProfile?.name ?: "Mondi Morris",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = userProfile?.email ?: "mondimorris@gmail.com",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatsCardsRow(userProfile: UserProfile?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.MonitorWeight,
            title = "Weight",
            value = "${userProfile?.weight ?: 65.5} kg"
        )
        
        StatsCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Height,
            title = "Height",
            value = "${(userProfile?.height ?: 170) / 30.48f} Ft".let { 
                String.format("%.1f Ft", (userProfile?.height ?: 170) / 30.48f)
            }
        )
        
        StatsCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Cake,
            title = "Age",
            value = "${userProfile?.age ?: 25} Years"
        )
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PremiumSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Pro",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Go Premium & Unlock More!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upgrade to Premium and enjoy exclusive features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MenuItemsSection(
    onEditProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuItem(
            icon = Icons.Default.Person,
            title = "Edit Profile",
            onClick = onEditProfileClick
        )
        
        MenuItem(
            icon = Icons.Default.Notifications,
            title = "Notification",
            onClick = onNotificationsClick,
            showSwitch = true,
            switchState = true
        )
        
        MenuItem(
            icon = Icons.Default.Settings,
            title = "Setting",
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    showSwitch: Boolean = false,
    switchState: Boolean = false
) {
    var switchChecked by remember { mutableStateOf(switchState) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!showSwitch) onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (showSwitch) {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = { 
                        switchChecked = it
                        onClick() 
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(onLogoutClick: () -> Unit) {
    Button(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Log Out",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
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
    var weight by remember { mutableStateOf(userProfile?.weight?.toString() ?: "") }
    var height by remember { mutableStateOf(userProfile?.height?.toString() ?: "") }
    var age by remember { mutableStateOf(userProfile?.age?.toString() ?: "") }
    
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false // Don't allow email changes as it's the primary key
                )
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
                    userProfile?.let { profile ->
                        val weightFloat = weight.toFloatOrNull() ?: profile.weight
                        val heightInt = height.toIntOrNull() ?: profile.height
                        val ageInt = age.toIntOrNull() ?: profile.age
                        
                        onSave(
                            profile.copy(
                                name = name,
                                weight = weightFloat,
                                height = heightInt,
                                age = ageInt
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