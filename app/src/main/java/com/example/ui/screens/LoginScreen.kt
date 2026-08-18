package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.R
import com.example.data.repository.AuthState
import com.example.ui.MainViewModel
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SlateBackgroundDark
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsState()

    var isRegisterTab by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Loading || isSubmitting

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(Exception::class.java)
            
            account?.idToken?.let { idToken ->
                Log.d(TAG, "✅ Google Sign-In successful, got ID Token")
                isSubmitting = true
                scope.launch {
                    val authResult = viewModel.authRepository.handleGoogleSignInResult(idToken)
                    authResult.onFailure { e ->
                        Log.e(TAG, "❌ Google authentication failed: ${e.message}", e)
                        isSubmitting = false
                    }
                }
            } ?: run {
                errorMessage = "❌ Failed to get Google ID Token. Please try again."
                Log.e(TAG, "❌ ID Token is null after Google Sign-In")
            }
        } catch (e: Exception) {
            errorMessage = "❌ Google Sign-In failed: ${e.message ?: "Unknown error"}"
            Log.e(TAG, "❌ Google Sign-In exception: ${e.message}", e)
        }
    }

    // ✅ Observe authState and react to Error state
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Error -> {
                isSubmitting = false
                val rawMsg = state.message
                Log.e(TAG, "Firebase Auth Error: $rawMsg")

                // Translate Firebase error codes into user-friendly messages
                errorMessage = when {
                    rawMsg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                    rawMsg.contains("wrong-password", ignoreCase = true) ||
                    rawMsg.contains("invalid-credential", ignoreCase = true) ->
                        "Incorrect email or password. Please try again."

                    rawMsg.contains("user-not-found", ignoreCase = true) ||
                    rawMsg.contains("EMAIL_NOT_FOUND", ignoreCase = true) ->
                        "No account found with this email. Please register first."

                    rawMsg.contains("email-already-in-use", ignoreCase = true) ->
                        "This email is already registered. Try signing in instead."

                    rawMsg.contains("weak-password", ignoreCase = true) ->
                        "Password is too weak. Use at least 6 characters."

                    rawMsg.contains("invalid-email", ignoreCase = true) ->
                        "Please enter a valid email address."

                    rawMsg.contains("network", ignoreCase = true) ||
                    rawMsg.contains("NETWORK_ERROR", ignoreCase = true) ->
                        "Network error. Please check your internet connection."

                    rawMsg.contains("too-many-requests", ignoreCase = true) ->
                        "Too many failed attempts. Please try again later."

                    rawMsg.contains("operation-not-allowed", ignoreCase = true) ->
                        "Email/Password sign-in is not enabled. Enable it in Firebase Console → Authentication → Sign-in method."

                    rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                        "Firebase not configured. Please check google-services.json."

                    else -> "Authentication failed: $rawMsg"
                }
            }
            is AuthState.Authenticated -> {
                isSubmitting = false
                errorMessage = null
            }
            is AuthState.Loading -> { /* spinner shows */ }
            is AuthState.Unauthenticated -> {
                isSubmitting = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Banner Image & Branded Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.ai_assistant_banner_1785320769471),
                        contentDescription = "Header Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CallSense AI",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Intelligent Missed Call & Voicemail Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login / Register Selector
            TabRow(
                selectedTabIndex = if (isRegisterTab) 1 else 0,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = !isRegisterTab,
                    onClick = {
                        isRegisterTab = false
                        errorMessage = null
                    },
                    text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = isRegisterTab,
                    onClick = {
                        isRegisterTab = true
                        errorMessage = null
                    },
                    text = { Text("Register", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    AnimatedVisibility(visible = isRegisterTab) {
                        Column {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Full Name", style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = isRegisterTab && displayName.isBlank() && isSubmitting,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password", style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    // ✅ Error Message Display
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ $msg",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ✅ Submit Button with proper validation
                    Button(
                        onClick = {
                            errorMessage = null

                            // Client-side validation before calling Firebase
                            when {
                                email.isBlank() -> {
                                    errorMessage = "Please enter your email address."
                                    return@Button
                                }
                                !email.contains("@") -> {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }
                                password.isBlank() -> {
                                    errorMessage = "Please enter your password."
                                    return@Button
                                }
                                password.length < 6 -> {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }
                                isRegisterTab && displayName.isBlank() -> {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                            }

                            isSubmitting = true
                            scope.launch {
                                Log.d(TAG, "Attempting ${if (isRegisterTab) "registration" else "login"} for: $email")
                                val result = if (isRegisterTab) {
                                    viewModel.authRepository.register(email.trim(), password, displayName.trim())
                                } else {
                                    viewModel.authRepository.login(email.trim(), password)
                                }
                                result.onFailure { e ->
                                    // authState LaunchedEffect already handles UI — but also log here
                                    Log.e(TAG, "Auth failed with exception", e)
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        if (isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isRegisterTab) "Creating Account..." else "Signing In...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        } else {
                            Text(
                                text = if (isRegisterTab) "🚀 Create Account" else "🔐 Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In Button
            Button(
                onClick = {
                    val googleSignInIntent = viewModel.authRepository.getGoogleSignInIntent()
                    if (googleSignInIntent != null) {
                        googleSignInLauncher.launch(googleSignInIntent)
                        Log.d(TAG, "🔐 Launching Google Sign-In flow")
                    } else {
                        errorMessage = "❌ Google Sign-In not available. Please check your Firebase configuration."
                        Log.e(TAG, "❌ Google Sign-In Intent is null")
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1F2937)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Mode Option
            OutlinedButton(
                onClick = {
                    viewModel.authRepository.loginAsGuest()
                    Toast.makeText(context, "Logged in as Demo Executive", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚡ Instant Demo Mode (Skip Auth)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CallSense AI utilizes Firebase Authentication & Gemini NLP for real-time speech processing and urgency detection.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
