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
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.crypto.GroupKeyManager

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
        // P2P activities are generated locally for now, so they do not have
        // a matching Firestore activity document to update.
        if (activityId.startsWith("p2p-local-")) {
            uiState = uiState.copy(
                activities = uiState.activities.map {
                    if (it.id == activityId) {
                        it.copy(status = ActivityStatus.COMPLETED)
                    } else {
                        it
                    }
                },
                p2pAlertDismissed = true
            )
            onSuccess()
            return
        }

        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val activity = uiState.activities.find { it.id == activityId }
            val myUid = FirebaseAuth.getInstance().currentUser?.uid

            val textToSave = if (activity != null && myUid != null && text.isNotBlank()) {
                GroupKeyManager.encryptText(getApplication(), activity.groupId, myUid, text) ?: text
            } else {
                text
            }

            val result = repository.saveActivityResponse(
                activityId, textToSave, photoPath, videoPath, audioPath, location
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
        val activity = ActivityItem(
            id = "p2p-local-simulated",
            groupId = "test-group",
            groupName = "Melbourne Uni Squad",
            title = "What are you doing together right now?",
            description = "test_user is nearby. Capture this moment with text, photo, audio, or video.",
            type = ActivityType.P2P_ALERT,
            status = ActivityStatus.PENDING,
            dueLabel = "Created just now · Nearby"
        )

        uiState = uiState.copy(
            activities = listOf(activity) +
                    uiState.activities.filterNot { it.id == activity.id },
            p2pAlert = activity,
            p2pAlertDismissed = false
        )
    }

    private fun observeNearbyMembers() {
        viewModelScope.launch {
            nearbyManager.connectedMembers.collect { members ->

                val activity =
                    members.firstOrNull()?.let { member ->
                        ActivityItem(
                            id = "p2p-local-${member.userId ?: member.endpointId}",
                            groupId = member.sharedGroupId.orEmpty(),
                            groupName = "Shared group",
                            title = "What are you doing together right now?",
                            description = "${member.endpointName} is nearby. Capture this moment with text, photo, audio, or video.",
                            type = ActivityType.P2P_ALERT,
                            status = ActivityStatus.PENDING,
                            dueLabel = "Created just now · Nearby"
                        )
                    }

                if (activity == null) {
                    uiState = uiState.copy(
                        p2pAlert = null,
                        p2pAlertDismissed = false
                    )
                } else {
                    uiState = uiState.copy(
                        activities = listOf(activity) +
                                uiState.activities.filterNot { it.id == activity.id },
                        p2pAlert = activity,
                        p2pAlertDismissed = false
                    )
                }
            }
        }
    }
}
