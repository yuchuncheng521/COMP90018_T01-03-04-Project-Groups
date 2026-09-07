package com.knot.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: UserAccount? = null
)

class AuthViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    val isLoggedIn: Boolean
        get() = repository.isLoggedIn

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter both email and password.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.signIn(email.trim(), password)
            uiState = result.fold(
                onSuccess = { AuthUiState(isLoading = false, loggedInUser = it) },
                onFailure = { AuthUiState(isLoading = false, errorMessage = it.message ?: "Login failed. Please try again.") }
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
                onSuccess = { AuthUiState(isLoading = false, loggedInUser = it) },
                onFailure = { AuthUiState(isLoading = false, errorMessage = it.message ?: "Sign up failed. Please try again.") }
            )
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun signOut() {
        repository.signOut()
        uiState = AuthUiState()
    }
}
