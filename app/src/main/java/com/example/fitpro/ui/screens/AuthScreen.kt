package com.example.fitpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

enum class AuthMode {
    LOGIN, SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    navController: NavController,
    userSession: UserSession,
    userDao: UserDao,
    onLoginSuccess: () -> Unit = {}
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    
    // Login state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showLoginPassword by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf("") }
    var isLoginLoading by remember { mutableStateOf(false) }
    
    // Signup state
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSignupPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var signupErrorMessage by remember { mutableStateOf("") }
    var isSignupLoading by remember { mutableStateOf(false) }
    
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
            text = if (authMode == AuthMode.LOGIN) "Welcome back! Let's get fit together" else "Join us and start your fitness journey",
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Auth Mode Toggle
        AuthModeToggle(
            currentMode = authMode,
            onModeChange = { 
                authMode = it
                // Clear errors when switching modes
                loginErrorMessage = ""
                signupErrorMessage = ""
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Animated content transition
        AnimatedContent(
            targetState = authMode,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    initialOffsetX = { if (targetState == AuthMode.SIGNUP) it else -it }
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    targetOffsetX = { if (targetState == AuthMode.SIGNUP) -it else it }
                ) + fadeOut(animationSpec = tween(300))
            },
            label = "auth_content"
        ) { mode ->
            when (mode) {
                AuthMode.LOGIN -> {
                    LoginContent(
                        email = loginEmail,
                        onEmailChange = { loginEmail = it },
                        password = loginPassword,
                        onPasswordChange = { loginPassword = it },
                        rememberMe = rememberMe,
                        onRememberMeChange = { rememberMe = it },
                        showPassword = showLoginPassword,
                        onTogglePasswordVisibility = { showLoginPassword = !showLoginPassword },
                        errorMessage = loginErrorMessage,
                        isLoading = isLoginLoading,
                        onLogin = {
                            coroutineScope.launch {
                                if (loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                                    isLoginLoading = true
                                    loginErrorMessage = ""
                                    
                                    try {
                                        val user = withContext(Dispatchers.IO) {
                                            userDao.authenticateUser(loginEmail.trim(), loginPassword)
                                        }
                                        
                                        if (user != null) {
                                            userSession.saveUserSession(loginEmail.trim(), rememberMe)
                                            onLoginSuccess()
                                        } else {
                                            val userExists = withContext(Dispatchers.IO) {
                                                userDao.userExists(loginEmail.trim())
                                            }
                                            
                                            if (userExists) {
                                                loginErrorMessage = "Incorrect password. Please try again."
                                            } else {
                                                loginErrorMessage = "Account not found. Please sign up first."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        loginErrorMessage = "Login failed. Please try again."
                                        e.printStackTrace()
                                    } finally {
                                        isLoginLoading = false
                                    }
                                } else {
                                    loginErrorMessage = "Please fill in all fields."
                                }
                            }
                        }
                    )
                }
                AuthMode.SIGNUP -> {
                    SignupContent(
                        name = signupName,
                        onNameChange = { signupName = it },
                        email = signupEmail,
                        onEmailChange = { signupEmail = it },
                        password = signupPassword,
                        onPasswordChange = { signupPassword = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        showPassword = showSignupPassword,
                        onTogglePasswordVisibility = { showSignupPassword = !showSignupPassword },
                        showConfirmPassword = showConfirmPassword,
                        onToggleConfirmPasswordVisibility = { showConfirmPassword = !showConfirmPassword },
                        errorMessage = signupErrorMessage,
                        isLoading = isSignupLoading,
                        onSignup = {
                            coroutineScope.launch {
                                if (signupName.isNotBlank() && signupEmail.isNotBlank() && 
                                    signupPassword.isNotBlank() && confirmPassword.isNotBlank()) {
                                    
                                    if (signupPassword != confirmPassword) {
                                        signupErrorMessage = "Passwords do not match."
                                        return@launch
                                    }
                                    
                                    if (signupPassword.length < 6) {
                                        signupErrorMessage = "Password must be at least 6 characters long."
                                        return@launch
                                    }
                                    
                                    isSignupLoading = true
                                    signupErrorMessage = ""
                                    
                                    try {
                                        val userExists = withContext(Dispatchers.IO) {
                                            userDao.userExists(signupEmail.trim())
                                        }
                                        
                                        if (userExists) {
                                            signupErrorMessage = "An account with this email already exists."
                                        } else {
                                            val userProfile = UserProfile(
                                                name = signupName.trim(),
                                                email = signupEmail.trim(),
                                                password = signupPassword,
                                                age = 0,
                                                weight = 0f,
                                                height = 0,
                                                gender = ""
                                            )
                                            
                                            withContext(Dispatchers.IO) {
                                                userDao.insertUser(userProfile)
                                            }
                                            
                                            userSession.saveUserSession(signupEmail.trim(), false)
                                            onLoginSuccess()
                                        }
                                    } catch (e: Exception) {
                                        signupErrorMessage = "Sign up failed. Please try again."
                                        e.printStackTrace()
                                    } finally {
                                        isSignupLoading = false
                                    }
                                } else {
                                    signupErrorMessage = "Please fill in all fields."
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthModeToggle(
    currentMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    val animatedOffset by animateFloatAsState(
        targetValue = if (currentMode == AuthMode.LOGIN) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "toggle_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                Color(0xFFF3F4F6),
                RoundedCornerShape(28.dp)
            )
            .padding(4.dp)
    ) {
        // Get the layout info using BoxWithConstraints
        BoxWithConstraints {
            val maxWidthPx = constraints.maxWidth
            val halfWidth = maxWidthPx / 2
            
            // Sliding background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { halfWidth.toDp() })
                    .offset(x = with(LocalDensity.current) { 
                        (animatedOffset * halfWidth).toDp() 
                    })
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF60A5FA)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Login Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.LOGIN) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (currentMode == AuthMode.LOGIN) Color.White else Color(0xFF6B7280)
                )
            }

            // Signup Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onModeChange(AuthMode.SIGNUP) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (currentMode == AuthMode.SIGNUP) Color.White else Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onLogin: () -> Unit
) {
    Column {
        // Email Input Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
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
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Password", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = "Password",
                    tint = Color(0xFF6B7280)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
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
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
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
                    onCheckedChange = onRememberMeChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3B82F6)
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
                    "Forgot password?",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Error message display
        AnimatedVisibility(
            visible = errorMessage.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
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

        // Login Button
        OutlinedButton(
            onClick = onLogin,
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA)
                    )
                )
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF3B82F6)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF3B82F6)
                )
            } else {
                Text(
                    text = "Login",
                    color = Color(0xFF3B82F6),
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
                    painter = painterResource(id = com.example.fitpro.R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
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
                    painter = painterResource(id = com.example.fitpro.R.drawable.apple),
                    contentDescription = "Apple",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
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

@Composable
fun SignupContent(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    showConfirmPassword: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onSignup: () -> Unit
) {
    Column {
        // Full Name Input Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Full Name", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = "Full Name",
                    tint = Color(0xFF6B7280)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Email Input Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
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
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Password", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = "Password",
                    tint = Color(0xFF6B7280)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
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
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Confirm Password Input Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = { Text("Confirm Password", color = Color(0xFF9CA3AF)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = "Confirm Password",
                    tint = Color(0xFF6B7280)
                )
            },
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                    Icon(
                        if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                        tint = Color(0xFF6B7280)
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF3B82F6),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            ),
            singleLine = true
        )

        // Error message display
        AnimatedVisibility(
            visible = errorMessage.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
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

        // Sign Up Button
        OutlinedButton(
            onClick = onSignup,
            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA)
                    )
                )
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF3B82F6)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF3B82F6)
                )
            } else {
                Text(
                    text = "Create Account",
                    color = Color(0xFF3B82F6),
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
                    painter = painterResource(id = com.example.fitpro.R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
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
                    painter = painterResource(id = com.example.fitpro.R.drawable.apple),
                    contentDescription = "Apple",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
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
