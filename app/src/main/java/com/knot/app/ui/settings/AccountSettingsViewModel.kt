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
import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.KnotApplication
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount
import com.knot.app.location.LocationPreferences
import com.knot.app.nearby.NearbyForegroundService
import com.knot.app.notifications.NotificationPreferences
import kotlinx.coroutines.launch

data class AccountSettingsUiState(
    val account: UserAccount = UserAccount(
        displayName = "Guest",
        email = ""
    ),
    val notificationsEnabled: Boolean = true,
    val weeklyPromptRemindersEnabled: Boolean = true,
    val p2pAlertsEnabled: Boolean = true,
    val shareLocationWithMemories: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val appTheme: String = "System", // Light, Dark, System
    val textSizeMultiplier: Float = 1.0f,
    val syncStatus: String = "Up to date",
    val storageUsage: Float = 0.45f,
    val syncOverWifi: Boolean = true,
    val showClearCacheDialog: Boolean = false,
    val isCameraAllowed: Boolean = false,
    val isMicrophoneAllowed: Boolean = false,
    val isLocationAllowed: Boolean = false,
    val isBluetoothAllowed: Boolean = false
    val p2pAlertsEnabled: Boolean = false,
    val shareLocationWithMemories: Boolean = true,
    val nearbyError: String? = null
)

class AccountSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val nearbyManager =
        (application as KnotApplication).nearbyManager

    var uiState by mutableStateOf(
        AccountSettingsUiState(
            account = repository.currentUser
                ?: UserAccount(
                    displayName = "Guest",
                    email = "Not signed in"
                ),
            notificationsEnabled =
                NotificationPreferences.isPushEnabled(application),
            shareLocationWithMemories =
                LocationPreferences.isAttachLocationEnabled(application)
        )
    )
        private set

    init {
        viewModelScope.launch {
            nearbyManager.errorMessage.collect { message ->
                uiState = uiState.copy(
                    nearbyError = message
                )
            }
        }
    }

    fun refreshAccount() {
        repository.currentUser?.let {
            uiState = uiState.copy(account = it)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        NotificationPreferences.setPushEnabled(
            getApplication(),
            enabled
        )

        uiState = uiState.copy(
            notificationsEnabled = enabled
        )
    }

    fun setWeeklyPromptReminders(enabled: Boolean) {
        uiState = uiState.copy(
            weeklyPromptRemindersEnabled = enabled
        )
    }

    fun setP2pAlertsEnabled(enabled: Boolean) {
        uiState = uiState.copy(
            p2pAlertsEnabled = enabled
        )

        if (!enabled) {
            stopNearby()
        }
    }

    fun setShareLocationWithMemories(enabled: Boolean) {
        LocationPreferences.setAttachLocationEnabled(
            getApplication(),
            enabled
        )

        uiState = uiState.copy(
            shareLocationWithMemories = enabled
        )
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

    fun setAppTheme(theme: String) {
        uiState = uiState.copy(appTheme = theme)
    }

    fun setTextSize(multiplier: Float) {
        uiState = uiState.copy(textSizeMultiplier = multiplier)
    }

    fun setSyncOverWifi(enabled: Boolean) {
        uiState = uiState.copy(syncOverWifi = enabled)
    }

    fun setShowClearCacheDialog(show: Boolean) {
        uiState = uiState.copy(showClearCacheDialog = show)
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
        stopNearby()
        repository.signOut()
        onSignedOut()
    }

    fun startNearby() {
        val userName = uiState.account.displayName.ifBlank { "KnotUser" }

        val intent =
            Intent(
                getApplication(),
                NearbyForegroundService::class.java
            ).apply {
                putExtra(
                    NearbyForegroundService.EXTRA_USER_NAME,
                    userName
                )
            }

        ContextCompat.startForegroundService(
            getApplication(),
            intent
        )
    }

    private fun stopNearby() {
        val intent =
            Intent(
                getApplication(),
                NearbyForegroundService::class.java
            )

        getApplication<Application>()
            .stopService(intent)

        nearbyManager.stopNearby()
    }
}
