package com.knot.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount
import kotlinx.coroutines.launch

/**
 * State for the Login / Sign up / Forgot password screens.
 *
 * NOTE for whoever owns the screens (Qin Yu Chong): these are the fields wired up so far --
 * flag anything the UI still needs that isn't here yet, rather than reading this as final.
 *
 *  - isLoading:               show a spinner / disable submit buttons.
 *  - errorMessage:            already a friendly, ready-to-display string (see AuthErrorMapper);
 *                             never raw Firebase exception text.
 *  - loggedInUser:            non-null once sign-in/sign-up succeeds, or when an existing
 *                             session is (re)detected -- also updates on its own if the session
 *                             expires elsewhere, since it's fed by AuthRepository.authStateFlow().
 *  - isPasswordResetEmailSent: true after a reset email goes out, for the Forgot Password screen
 *                             to swap its form for a "check your inbox" message.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: UserAccount? = null,
    val isPasswordResetEmailSent: Boolean = false
)

class AuthViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    val isLoggedIn: Boolean
        get() = repository.isLoggedIn

    init {
        // Reacts on its own to sign-out/session-expiry from anywhere (not just this ViewModel's
        // own login()/signOut() calls) -- e.g. the token being revoked, or another AuthViewModel
        // instance signing out -- instead of only checking isLoggedIn once at launch.
        viewModelScope.launch {
            repository.authStateFlow().collect { user ->
                uiState = uiState.copy(loggedInUser = user)
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter both email and password.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.signIn(email.trim(), password)
            uiState = result.fold(
                onSuccess = { uiState.copy(isLoading = false, loggedInUser = it) },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Login failed. Please try again.") }
            )
        }
    }

    fun signUp(displayName: String, email: String, password: String, confirmPassword: String) {
        if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please fill in all fields.")
            return
        }
        if (password.length < 6) {
            uiState = uiState.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }
        if (password != confirmPassword) {
            uiState = uiState.copy(errorMessage = "Passwords don't match.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.signUp(displayName.trim(), email.trim(), password)
            uiState = result.fold(
                onSuccess = { uiState.copy(isLoading = false, loggedInUser = it) },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Sign up failed. Please try again.") }
            )
        }
    }

    /** Sends a password-reset email. On success, [AuthUiState.isPasswordResetEmailSent] flips to true. */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter your email first.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email.trim())
            uiState = result.fold(
                onSuccess = { uiState.copy(isLoading = false, isPasswordResetEmailSent = true) },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Couldn't send that email. Please try again.") }
            )
        }
    }

    /** Resets the Forgot Password screen back to its form (e.g. when the user navigates away and back). */
    fun clearPasswordResetState() {
        uiState = uiState.copy(isPasswordResetEmailSent = false)
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun signOut() {
        repository.signOut()
        uiState = AuthUiState()
    }
}
