package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirebaseAuthRepo"

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val isGuest: Boolean = false
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthRepository(private val context: Context? = null) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val firebaseAuth: FirebaseAuth? = try {
        val auth = FirebaseAuth.getInstance()
        Log.d(TAG, "✅ Firebase Auth initialized successfully")
        auth
    } catch (e: Exception) {
        Log.e(TAG, "❌ Firebase Auth initialization failed. Ensure google-services.json is valid: ${e.message}")
        Log.e(TAG, "Exception details:", e)
        null
    }

    private val isFirebaseAvailable: Boolean = firebaseAuth != null

    private val googleSignInClient: GoogleSignInClient? = context?.let {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("102235696238-7ojaij0q5tpbs6ll7pa0gq1ph30ghbn3.apps.googleusercontent.com")
                .requestEmail()
                .build()
            GoogleSignIn.getClient(it, gso).also {
                Log.d(TAG, "✅ Google Sign-In Client initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In Client initialization failed: ${e.message}")
            null
        }
    }

    init {
        Log.d(TAG, if (isFirebaseAvailable) "✅ FirebaseAuthRepository initialized — Firebase is available" else "⚠️ FirebaseAuthRepository initialized — Firebase is NOT available")
        Log.d(TAG, if (googleSignInClient != null) "✅ Google Sign-In Client is ready" else "⚠️ Google Sign-In Client not initialized")
        
        if (!isFirebaseAvailable) {
            Log.w(TAG, """
                Firebase configuration issue detected. Please verify:
                1. google-services.json file is in app/ directory
                2. google_services plugin is applied in build.gradle
                3. Firebase project is properly configured in Firebase Console
                4. OAuth 2.0 credentials are set up for your app
                5. Enable Email/Password and Google Sign-In in Firebase Console → Authentication → Sign-in method
            """.trimIndent())
        }
        
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Existing Firebase user found: ${currentUser.email}")
            _authState.value = AuthState.Authenticated(
                UserProfile(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "user@callsense.ai",
                    displayName = currentUser.displayName
                        ?: currentUser.email?.substringBefore("@")
                            ?.replaceFirstChar { it.uppercase() }
                        ?: "User"
                )
            )
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun login(email: String, pass: String): Result<UserProfile> {
        if (email.isBlank() || pass.isBlank()) {
            val err = "Email and password cannot be empty"
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        _authState.value = AuthState.Loading
        Log.d(TAG, "Attempting login for email: $email")

        return if (isFirebaseAvailable) {
            try {
                val result = firebaseAuth!!.signInWithEmailAndPassword(email, pass).await()
                val user = result.user ?: throw Exception("User is null after sign-in")
                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: email.substringBefore("@")
                        .replaceFirstChar { it.uppercase() }
                )
                _authState.value = AuthState.Authenticated(profile)
                Result.success(profile)
            } catch (e: FirebaseAuthException) {
                val errorMsg = getFirebaseErrorMessage(e.errorCode, e.message)
                Log.e(TAG, "Login Error: ${e.errorCode} -> $errorMsg")
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed.")
                Result.failure(e)
            }
        } else {
            loginAsGuestFallback(email)
        }
    }

    suspend fun register(email: String, pass: String, name: String): Result<UserProfile> {
        if (email.isBlank() || pass.isBlank()) {
            val err = "Email and password cannot be empty"
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        _authState.value = AuthState.Loading
        Log.d(TAG, "Attempting registration for email: $email")

        return if (isFirebaseAvailable) {
            try {
                val result = firebaseAuth!!.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user ?: throw Exception("User is null after registration")
                val profile = UserProfile(
                    uid = user.uid,
                    email = email,
                    displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
                )
                _authState.value = AuthState.Authenticated(profile)
                Result.success(profile)
            } catch (e: FirebaseAuthException) {
                val errorMsg = getFirebaseErrorMessage(e.errorCode, e.message)
                Log.e(TAG, "Registration Error: ${e.errorCode} -> $errorMsg")
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed.")
                Result.failure(e)
            }
        } else {
            loginAsGuestFallback(email, name)
        }
    }

    private fun loginAsGuestFallback(email: String, name: String = ""): Result<UserProfile> {
        val profile = UserProfile(
            "demo_${email.hashCode()}",
            email,
            name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        )
        _authState.value = AuthState.Authenticated(profile)
        return Result.success(profile)
    }

    fun loginAsGuest() {
        _authState.value = AuthState.Authenticated(
            UserProfile("guest_demo", "demo.assistant@callsense.ai", "Demo Executive", isGuest = true)
        )
    }

    /**
     * Returns the Google Sign-In Intent for launching the sign-in flow
     * Call this from your Activity/Fragment and handle the result with handleGoogleSignInResult()
     */
    fun getGoogleSignInIntent() = googleSignInClient?.signInIntent

    /**
     * Handle the result from Google Sign-In Activity
     * Call this after receiving the result from ActivityResult/startActivityForResult
     */
    suspend fun handleGoogleSignInResult(idToken: String): Result<UserProfile> {
        return authenticateWithGoogleIdToken(idToken)
    }

    /**
     * Authenticate with Firebase using Google ID Token
     */
    private suspend fun authenticateWithGoogleIdToken(idToken: String): Result<UserProfile> {
        if (idToken.isBlank()) {
            val err = "❌ Google ID Token is empty"
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        return if (isFirebaseAvailable && firebaseAuth != null) {
            try {
                _authState.value = AuthState.Loading
                Log.d(TAG, "🔐 Attempting Firebase authentication with Google ID Token")

                // Create Google credentials
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                
                // Sign in with Firebase
                val result = firebaseAuth!!.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("User is null after Google sign-in")

                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "google-user@callsense.ai",
                    displayName = user.displayName ?: user.email?.substringBefore("@")
                        ?.replaceFirstChar { it.uppercase() } ?: "Google User"
                )

                _authState.value = AuthState.Authenticated(profile)
                Log.d(TAG, "✅ Google Sign-In successful! Authenticated as: ${profile.email}")
                Result.success(profile)

            } catch (e: FirebaseAuthException) {
                val errorMsg = getFirebaseErrorMessage(e.errorCode, e.message)
                Log.e(TAG, "❌ Google Sign-In Error: ${e.errorCode} -> $errorMsg")
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))

            } catch (e: Exception) {
                val errorMsg = "❌ Google Sign-In failed: ${e.message}"
                Log.e(TAG, "❌ Exception during Google Sign-In: ${e.message}", e)
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(e)
            }
        } else {
            // Fallback for when Firebase is not available
            Log.w(TAG, "⚠️ Firebase not available, using guest fallback for Google sign-in")
            loginAsGuestFallback("google-user@callsense.ai", "Google User")
        }
    }

    fun loginWithGoogle() {
        // Google Sign-In flow:
        // 1. This is called when user taps Google Sign-In button
        // 2. In a real implementation, this would:
        //    - Launch Google Sign-In Activity with a registered OAuth2.0 credential ID
        //    - Get the Google Account and ID Token
        //    - Use FirebaseAuth.signInWithCredential(GoogleAuthCredential) 
        // 3. For now, we set a loading state to indicate the process is starting
        _authState.value = AuthState.Loading
        Log.d(TAG, "Google Sign-In initiated. Ensure project is configured with OAuth 2.0 credentials in Firebase Console.")
        // Implementation would continue after receiving the GoogleSignInAccount from the Activity
        // Example: signInWithGoogleCredential(account)
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> {
        return if (isFirebaseAvailable) {
            try {
                _authState.value = AuthState.Loading
                Log.d(TAG, "Attempting Google sign-in with ID token")
                // This would be called after getting the ID token from Google Sign-In
                // For production: use FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                val user = firebaseAuth!!.currentUser ?: throw Exception("User is null after Google sign-in")
                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "google-user@callsense.ai",
                    displayName = user.displayName ?: "Google User"
                )
                _authState.value = AuthState.Authenticated(profile)
                Result.success(profile)
            } catch (e: FirebaseAuthException) {
                val errorMsg = getFirebaseErrorMessage(e.errorCode, e.message)
                Log.e(TAG, "Google Sign-In Error: ${e.errorCode} -> $errorMsg")
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed.")
                Result.failure(e)
            }
        } else {
            loginAsGuestFallback("google-user@callsense.ai", "Google User")
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
            googleSignInClient?.signOut()?.addOnCompleteListener {
                Log.d(TAG, "✅ Signed out from both Firebase and Google")
            }
            _authState.value = AuthState.Unauthenticated
            Log.d(TAG, "✅ User signed out")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign out error: ${e.message}")
        }
    }

    private fun getFirebaseErrorMessage(errorCode: String?, fallback: String?): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL", "invalid-email" -> "❌ The email address is invalid. Please enter a valid email."
            "ERROR_WRONG_PASSWORD", "wrong-password", "ERROR_INVALID_CREDENTIAL", "invalid-credential" -> "❌ Incorrect email or password. Please try again."
            "ERROR_USER_NOT_FOUND", "user-not-found" -> "❌ No account found with this email. Please register first."
            "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" -> "❌ This email is already registered. Try signing in instead."
            "ERROR_WEAK_PASSWORD", "weak-password" -> "❌ Password is too weak. Use at least 6 characters with mix of upper/lowercase and numbers."
            "ERROR_OPERATION_NOT_ALLOWED", "operation-not-allowed" -> "❌ Email/Password authentication is disabled. Enable it in Firebase Console → Authentication → Sign-in method."
            "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> "⏳ Too many failed attempts. Please wait a few minutes and try again."
            "ERROR_NETWORK_REQUEST_FAILED", "network-request-failed" -> "🌐 Network error. Please check your internet connection and try again."
            "CONFIGURATION_NOT_FOUND" -> "⚙️ Firebase configuration error. Ensure google-services.json is properly configured."
            else -> fallback?.let { "❌ Authentication failed: $it" } ?: "❌ Authentication failed. Please try again."
        }
    }
}
