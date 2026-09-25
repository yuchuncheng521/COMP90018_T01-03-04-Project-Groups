package com.knot.app.ui.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.KnotApplication
import com.knot.app.data.ActivitiesRepository
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityItem> = emptyList(),
    val p2pAlert: ActivityItem? = null,
    val p2pAlertDismissed: Boolean = false,
)

class ActivitiesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val nearbyManager =
        (application as KnotApplication).nearbyManager

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
            val activities = repository.getActivities()
            val alert = repository.getP2pAlert()

            uiState = uiState.copy(
                isLoading = false,
                activities = activities,
                p2pAlert = alert
            )
        }
    }

    fun dismissP2pAlert() {
        uiState = uiState.copy(
            p2pAlertDismissed = true
        )
    }

    fun markCompleted(activityId: String) {
        uiState = uiState.copy(
            activities = uiState.activities.map {
                if (it.id == activityId) {
                    it.copy(status = ActivityStatus.COMPLETED)
                } else {
                    it
                }
            }
        )
        viewModelScope.launch {
            repository.updateActivityStatus(activityId, ActivityStatus.COMPLETED)
        }
    }

    fun submitActivityResponse(
        activityId: String,
        text: String,
        photoPath: String?,
        videoPath: String?,
        audioPath: String?,
        location: String?,
        onSuccess: () -> Unit
    ) {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val result = repository.saveActivityResponse(
                activityId, text, photoPath, videoPath, audioPath, location
            )
            uiState = uiState.copy(isLoading = false)
            result.onSuccess {
                // Locally update status for immediate feedback
                uiState = uiState.copy(
                    activities = uiState.activities.map {
                        if (it.id == activityId) it.copy(status = ActivityStatus.COMPLETED) else it
                    }
                )
                onSuccess()
            }
        }
    }

    fun startNearby(userName: String) {
        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
    }

    fun simulateP2pConnection() {
        uiState = uiState.copy(
            p2pAlert = ActivityItem(
                id = "p2p-simulated",
                groupId = "test-group",
                groupName = "Melbourne Uni Squad",
                title = "test_user is nearby. Capture a memory together?",
                description = "You're both here right now. Take a photo to save this moment.",
                type = ActivityType.P2P_ALERT,
                status = ActivityStatus.PENDING,
                dueLabel = "Simulated connection · P2P"
            ),
            p2pAlertDismissed = false
        )
    }

    private fun observeNearbyMembers() {
        viewModelScope.launch {
            nearbyManager.connectedMembers.collect { members ->

                val alert =
                    members.firstOrNull()?.let { member ->
                        ActivityItem(
                            id = "p2p-${member.endpointId}",
                            groupId = member.sharedGroupId.orEmpty(),
                            groupName = "Shared group",
                            title = "${member.endpointName} is nearby. Capture a memory together?",
                            description = "You're both here right now. Take a photo to save this moment.",
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
