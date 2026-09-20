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
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.knot.app.nearby.NearbyManager
import com.knot.app.model.ActivityType
data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityItem> = emptyList(),
    val p2pAlert: ActivityItem? = null,
    val p2pAlertDismissed: Boolean = false
)

class ActivitiesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val nearbyManager =
        NearbyManager(application.applicationContext)

    private val repository =
        ActivitiesRepository(nearbyManager)

    var uiState by mutableStateOf(ActivitiesUiState())
        private set

    init {
        loadActivities()
        observeNearbyMembers()
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

    fun startNearby(userName: String) {
        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
    }

    private fun observeNearbyMembers() {
        viewModelScope.launch {
            nearbyManager.connectedMembers.collect { members ->

                val alert =
                    members.firstOrNull()?.let { member ->
                        ActivityItem(
                            id = "p2p-${member.endpointId}",
                            groupName = "Nearby member",
                            title = "You're near ${member.endpointName} right now!",
                            description = "Capture this moment together before it's gone.",
                            type = ActivityType.P2P_ALERT,
                            status = ActivityStatus.PENDING,
                            dueLabel = "Detected just now · Nearby"
                        )
                    }

                uiState = uiState.copy(
                    p2pAlert = alert,
                    p2pAlertDismissed = false
                )
            }
        }
    }
}
