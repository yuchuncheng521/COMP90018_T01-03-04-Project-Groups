package com.knot.app.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.KnotApplication
import com.knot.app.data.AuthRepository
import com.knot.app.location.LocationPreferences
import com.knot.app.model.UserAccount
import com.knot.app.nearby.NearbyForegroundService
import com.knot.app.notifications.NotificationPreferences
import com.knot.app.permissions.PermissionManager
import kotlinx.coroutines.launch

data class AccountSettingsUiState(
    val account: UserAccount = UserAccount(
        displayName = "Guest",
        email = ""
    ),
    val notificationsEnabled: Boolean = true,
    val weeklyPromptRemindersEnabled: Boolean = false,
    val p2pAlertsEnabled: Boolean = false,
    val shareLocationWithMemories: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val appTheme: String = "Default",
    val textSizeMultiplier: Float = 1.0f,
    val syncStatus: String = "Up to date",
    val storageUsage: Float = 0.45f,
    val syncOverWifi: Boolean = true,
    val showClearCacheDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isCameraAllowed: Boolean = false,
    val isMicrophoneAllowed: Boolean = false,
    val isLocationAllowed: Boolean = false,
    val isBluetoothAllowed: Boolean = false,
    val isEncryptionActive: Boolean = false,
    val encryptionStatus: String = "Checking...",
    val nearbyError: String? = null,
    val avatarColorHex: String = "#C97C5D"
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
            weeklyPromptRemindersEnabled =
                NotificationPreferences.areWeeklyPromptRemindersEnabled(application),
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

    fun loadPreferences(context: Context) {
        val prefs = context.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
        val savedMultiplier = prefs.getFloat("text_size_multiplier", 1.0f)
        val savedTheme = prefs.getString("app_theme", "Default") ?: "Default"
        val savedP2pEnabled = prefs.getBoolean("p2p_alerts_enabled", false)

        uiState = uiState.copy(
            textSizeMultiplier = savedMultiplier,
            appTheme = savedTheme,
            p2pAlertsEnabled = savedP2pEnabled
        )

        // Swiping the app away can stop the process/service, but it should not
        // change the user's P2P setting. When the app is opened again, restore
        // the foreground Nearby service if the saved setting is still enabled
        // and the required device state is available.
        if (
            savedP2pEnabled &&
            repository.isLoggedIn &&
            PermissionManager.isBluetoothEnabled(context) &&
            PermissionManager.hasNearbyPermissions(context)
        ) {
            startNearby()
        }
    }

    fun refreshAvatarColor() {
        viewModelScope.launch {
            val color = repository.getAvatarColor()
            if (color != null) {
                uiState = uiState.copy(avatarColorHex = color)
            }
        }
    }

    fun updateAvatarColor(colorHex: String) {
        uiState = uiState.copy(avatarColorHex = colorHex)
        viewModelScope.launch {
            repository.updateAvatarColor(colorHex)
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
        NotificationPreferences.setWeeklyPromptRemindersEnabled(
            getApplication(),
            enabled
        )

        uiState = uiState.copy(
            weeklyPromptRemindersEnabled = enabled
        )
    }

    fun setP2pAlertsEnabled(enabled: Boolean) {
        getApplication<Application>()
            .getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("p2p_alerts_enabled", enabled)
            .apply()

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

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updateDisplayName(name)
            uiState = result.fold(
                onSuccess = {
                    val updatedAccount = repository.currentUser ?: uiState.account.copy(displayName = name.trim())
                    uiState.copy(
                        account = updatedAccount,
                        isLoading = false,
                        successMessage = "Name updated successfully!"
                    )
                },
                onFailure = {
                    uiState.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Failed to update name."
                    )
                }
            )
        }
    }

    fun updatePassword(password: String) {
        if (password.length < 6) {
            uiState = uiState.copy(
                errorMessage = "Password must be at least 6 characters."
            )
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updatePassword(password)
            uiState = result.fold(
                onSuccess = {
                    uiState.copy(
                        isLoading = false,
                        successMessage = "Password updated successfully!"
                    )
                },
                onFailure = {
                    uiState.copy(
                        isLoading = false,
                        errorMessage = it.message
                            ?: "Failed to update password. You may need to re-login."
                    )
                }
            )
        }
    }

    fun setAppTheme(theme: String, context: Context? = null) {
        uiState = uiState.copy(appTheme = theme)
        val ctx = context ?: getApplication<Application>()
        ctx.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_theme", theme)
            .apply()
    }

    fun setTextSize(multiplier: Float, context: Context? = null) {
        uiState = uiState.copy(textSizeMultiplier = multiplier)
        val ctx = context ?: getApplication<Application>()
        ctx.getSharedPreferences("knot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putFloat("text_size_multiplier", multiplier)
            .apply()
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
            isBluetoothAllowed = PermissionManager.hasNearbyPermissions(context)
        )
    }

    fun clearLocalCache(context: Context) {
        uiState = uiState.copy(
            isLoading = true,
            showClearCacheDialog = false
        )

        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "Local cache cleared."
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to clear cache."
                )
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun signOut(onSignedOut: () -> Unit) {
        uiState = uiState.copy(showLogoutDialog = false)
        setP2pAlertsEnabled(false)
        repository.signOut()
        onSignedOut()
    }

    fun startNearby() {
        val userName = uiState.account.displayName.ifBlank { "KnotUser" }

        val intent = Intent(
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
        val intent = Intent(
            getApplication(),
            NearbyForegroundService::class.java
        )

        getApplication<Application>().stopService(intent)
        nearbyManager.stopNearby()
    }
}
