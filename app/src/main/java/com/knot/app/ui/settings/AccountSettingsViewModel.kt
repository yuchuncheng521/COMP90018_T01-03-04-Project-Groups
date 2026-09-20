package com.knot.app.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.knot.app.KnotApplication
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount
import androidx.lifecycle.viewModelScope
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
                )
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
            nearbyManager.stopNearby()
        }
    }

    fun setShareLocationWithMemories(enabled: Boolean) {
        uiState = uiState.copy(
            shareLocationWithMemories = enabled
        )
    }

    fun signOut(onSignedOut: () -> Unit) {
        nearbyManager.stopNearby()
        repository.signOut()
        onSignedOut()
    }

    fun startNearby() {
        val userName = uiState.account.displayName.ifBlank { "KnotUser" }

        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
    }
}