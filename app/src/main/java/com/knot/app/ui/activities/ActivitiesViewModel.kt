package com.knot.app.ui.activities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.ActivitiesRepository
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityItem> = emptyList(),
    val p2pAlert: ActivityItem? = null,
    val p2pAlertDismissed: Boolean = false
)

class ActivitiesViewModel @JvmOverloads constructor(
    private val repository: ActivitiesRepository = ActivitiesRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ActivitiesUiState())
        private set

    init {
        loadActivities()
    }

    fun loadActivities() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            // TODO(sensors): getP2pAlert() is currently a placeholder. Replace with a live
            // observer on the BLE proximity-scan service once it exists, so this banner reacts
            // in real time instead of only refreshing on screen load.
            val activities = repository.getActivities()
            val alert = repository.getP2pAlert()
            uiState = uiState.copy(isLoading = false, activities = activities, p2pAlert = alert)
        }
    }

    fun dismissP2pAlert() {
        uiState = uiState.copy(p2pAlertDismissed = true)
    }

    fun markCompleted(activityId: String) {
        uiState = uiState.copy(
            activities = uiState.activities.map {
                if (it.id == activityId) it.copy(status = ActivityStatus.COMPLETED) else it
            }
        )
    }
}
