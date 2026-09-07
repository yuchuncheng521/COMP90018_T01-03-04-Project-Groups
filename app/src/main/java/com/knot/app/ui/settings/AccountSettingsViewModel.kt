package com.knot.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.knot.app.data.AuthRepository
import com.knot.app.model.UserAccount

data class AccountSettingsUiState(
    val account: UserAccount = UserAccount(displayName = "Guest", email = ""),
    val notificationsEnabled: Boolean = true,
    val weeklyPromptRemindersEnabled: Boolean = true,
    val p2pAlertsEnabled: Boolean = true,
    val shareLocationWithMemories: Boolean = true
)

class AccountSettingsViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(
        AccountSettingsUiState(
            account = repository.currentUser ?: UserAccount(displayName = "Guest", email = "Not signed in")
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

    fun signOut(onSignedOut: () -> Unit) {
        repository.signOut()
        onSignedOut()
    }
}
