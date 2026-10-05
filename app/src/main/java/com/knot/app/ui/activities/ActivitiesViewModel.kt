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
            val localActivity = uiState.activities.find { it.id == activityId }
            val isLocalP2p = activityId.startsWith("p2p-local-")

            val activityResult =
                if (isLocalP2p) {
                    val activity = localActivity
                        ?: run {
                            uiState = uiState.copy(isLoading = false)
                            return@launch
                        }
                    repository.createP2pActivity(activity)
                } else {
                    Result.success(localActivity)
                }

            activityResult.onFailure {
                uiState = uiState.copy(isLoading = false)
                return@launch
            }

            val activity = activityResult.getOrNull()
            val persistedActivityId = activity?.id ?: activityId
            val myUid = FirebaseAuth.getInstance().currentUser?.uid

            val textToSave = if (activity != null && myUid != null && text.isNotBlank()) {
                GroupKeyManager.encryptText(getApplication(), activity.groupId, myUid, text) ?: text
            } else {
                text
            }

            val result = repository.saveActivityResponse(
                context = getApplication(),
                activityId = persistedActivityId,
                groupId = activity?.groupId ?: "",
                text = textToSave,
                photoPath = photoPath,
                videoPath = videoPath,
                audioPath = audioPath,
                location = location
            )

            if (result.isSuccess) {
                val refreshedActivities = repository.getActivities()
                uiState = uiState.copy(
                    isLoading = false,
                    activities = refreshedActivities,
                    p2pAlertDismissed = if (isLocalP2p) true else uiState.p2pAlertDismissed
                )
                onSuccess()
            } else {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun startNearby(userName: String) {
        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
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
