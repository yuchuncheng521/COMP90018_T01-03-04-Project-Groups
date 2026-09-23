package com.knot.app.ui.settings

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
