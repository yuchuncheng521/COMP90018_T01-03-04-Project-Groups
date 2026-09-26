package com.knot.app.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount
import com.knot.app.permissions.PermissionManager
import kotlinx.coroutines.launch

data class AccountSettingsUiState(
    val account: UserAccount = UserAccount(displayName = "Guest", email = ""),
    val notificationsEnabled: Boolean = true,
    val weeklyPromptRemindersEnabled: Boolean = true,
    val p2pAlertsEnabled: Boolean = true,
    val shareLocationWithMemories: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val appTheme: String = "Default", // Default, Monochrome, Invert
    val textSizeMultiplier: Float = 1.0f,
    val syncStatus: String = "Up to date",
    val storageUsage: Float = 0.45f,
    val syncOverWifi: Boolean = true,
    val showClearCacheDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isCameraAllowed: Boolean = false,
    val isMicrophoneAllowed: Boolean = false,
    val isLocationAllowed: Boolean = false,
    val isBluetoothAllowed: Boolean = false
)

class AccountSettingsViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(
        AccountSettingsUiState(
            account = repository.currentUser ?: UserAccount(displayName = "Guest", email = "Not signed in"),
            textSizeMultiplier = initialTextSizeMultiplier,
            appTheme = initialAppTheme
        )
    )
        private set

    fun refreshAccount() {
        repository.currentUser?.let { uiState = uiState.copy(account = it) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        uiState = uiState.copy(notificationsEnabled = enabled)
    }

    fun setWeeklyPromptReminders(enabled: Boolean) {
        uiState = uiState.copy(weeklyPromptRemindersEnabled = enabled)
    }

    fun setP2pAlertsEnabled(enabled: Boolean) {
        uiState = uiState.copy(p2pAlertsEnabled = enabled)
    }

    fun setShareLocationWithMemories(enabled: Boolean) {
        uiState = uiState.copy(shareLocationWithMemories = enabled)
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank()) return
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = repository.updateDisplayName(name)
            uiState = result.fold(
                onSuccess = {
                    refreshAccount()
                    uiState.copy(isLoading = false, successMessage = "Name updated successfully!")
                },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Failed to update name.") }
            )
        }
    }

    fun updateEmail(email: String) {
        if (email.isBlank()) return
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = repository.updateEmail(email)
            uiState = result.fold(
                onSuccess = {
                    uiState.copy(isLoading = false, successMessage = "Verification email sent to $email.")
                },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Failed to update email. You may need to re-login.") }
            )
        }
    }

    fun updatePassword(password: String) {
        if (password.length < 6) {
            uiState = uiState.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = repository.updatePassword(password)
            uiState = result.fold(
                onSuccess = {
                    uiState.copy(isLoading = false, successMessage = "Password updated successfully!")
                },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Failed to update password. You may need to re-login.") }
            )
        }
    }

    fun deleteAccount(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter both email and password to confirm.")
            return
        }
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = repository.deleteAccount(email, password)
            uiState = result.fold(
                onSuccess = {
                    onSuccess()
                    uiState.copy(isLoading = false, successMessage = "Account deleted.")
                },
                onFailure = { uiState.copy(isLoading = false, errorMessage = it.message ?: "Failed to delete account. Ensure credentials are correct.") }
            )
        }
    }

    fun setAppTheme(theme: String, context: Context? = null) {
        initialAppTheme = theme
        uiState = uiState.copy(appTheme = theme)
        context?.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("app_theme", theme)
            ?.apply()
    }

    fun setTextSize(multiplier: Float, context: Context? = null) {
        initialTextSizeMultiplier = multiplier
        uiState = uiState.copy(textSizeMultiplier = multiplier)
        context?.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putFloat("text_size_multiplier", multiplier)
            ?.apply()
    }

    fun loadPreferences(context: Context) {
        val prefs = context.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
        val savedMultiplier = prefs.getFloat("text_size_multiplier", 1.0f)
        val savedTheme = prefs.getString("app_theme", "Default") ?: "Default"
        initialTextSizeMultiplier = savedMultiplier
        initialAppTheme = savedTheme
        uiState = uiState.copy(
            textSizeMultiplier = savedMultiplier,
            appTheme = savedTheme
        )
    }

    companion object {
        private var initialTextSizeMultiplier: Float = 1.0f
        private var initialAppTheme: String = "Default"
    }

    fun setSyncOverWifi(enabled: Boolean) {
        uiState = uiState.copy(syncOverWifi = enabled)
    }

    fun setShowClearCacheDialog(show: Boolean) {
        uiState = uiState.copy(showClearCacheDialog = show)
    }

    fun setShowLogoutDialog(show: Boolean) {
        uiState = uiState.copy(showLogoutDialog = show)
    }

    fun checkPermissions(context: Context) {
        uiState = uiState.copy(
            isCameraAllowed = PermissionManager.hasCameraPermission(context),
            isMicrophoneAllowed = PermissionManager.hasMicrophonePermission(context),
            isLocationAllowed = PermissionManager.hasLocationPermission(context),
            isBluetoothAllowed = PermissionManager.hasBluetoothPermission(context)
        )
    }

    fun clearLocalCache(context: Context) {
        uiState = uiState.copy(isLoading = true, showClearCacheDialog = false)
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                uiState = uiState.copy(isLoading = false, successMessage = "Local cache cleared.")
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, errorMessage = "Failed to clear cache.")
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }

    fun signOut(onSignedOut: () -> Unit) {
        repository.signOut()
        onSignedOut()
    }
}
