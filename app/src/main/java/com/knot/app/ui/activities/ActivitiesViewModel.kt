package com.knot.app.ui.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.KnotApplication
import com.knot.app.crypto.GroupKeyManager
import com.knot.app.data.ActivitiesRepository
import com.knot.app.data.GroupsRepository
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import com.knot.app.model.Group
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityItem> = emptyList(),
    val p2pAlert: ActivityItem? = null,
    val p2pAlertDismissed: Boolean = false,
    val userGroups: List<Group> = emptyList()
)

class ActivitiesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val nearbyManager =
        (application as KnotApplication).nearbyManager

    private val repository =
        ActivitiesRepository(nearbyManager)

    private val groupsRepository =
        GroupsRepository()

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
            val userGroups = groupsRepository.getGroups()

            uiState = uiState.copy(
                isLoading = false,
                activities = activities,
                p2pAlert = alert,
                userGroups = userGroups
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
        targetGroupId: String? = null,
        onSuccess: () -> Unit
    ) {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val activity = uiState.activities.find { it.id == activityId }
            val myUid = FirebaseAuth.getInstance().currentUser?.uid
            val effectiveGroupId = targetGroupId ?: activity?.groupId.orEmpty()

            val textToSave = if (effectiveGroupId.isNotBlank() && myUid != null && text.isNotBlank()) {
                GroupKeyManager.encryptText(getApplication(), effectiveGroupId, myUid, text) ?: text
            } else {
                text
            }

            val result = repository.saveActivityResponse(
                activityId = activityId,
                text = textToSave,
                photoPath = photoPath,
                videoPath = videoPath,
                audioPath = audioPath,
                location = location,
                targetGroupId = effectiveGroupId
            )
            uiState = uiState.copy(
                isLoading = false,
                activities = uiState.activities.map {
                    if (it.id == activityId) it.copy(status = ActivityStatus.COMPLETED) else it
                },
                p2pAlertDismissed = if (activityId.startsWith("p2p")) true else uiState.p2pAlertDismissed
            )
            result.onSuccess {
                onSuccess()
            }
            result.onFailure {
                // Still notify success for local/simulated activities
                onSuccess()
            }
        }
    }

    fun startNearby(userName: String) {
        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
    }

    fun simulateP2pConnection() {
        viewModelScope.launch {
            val groups = if (uiState.userGroups.isNotEmpty()) {
                uiState.userGroups
            } else {
                groupsRepository.getGroups()
            }

            // Simulate nearby member in specific shared groups (e.g. Melbourne Uni Squad & Sarah & Qin Yu)
            val sharedGroups = groups.filter {
                it.name.contains("Melbourne", ignoreCase = true) ||
                it.name.contains("Sarah", ignoreCase = true) ||
                it.name.contains("Squad", ignoreCase = true)
            }.ifEmpty { groups.take(2) }

            val sharedGroupIds = sharedGroups.map { it.id }
            val primaryGroup = sharedGroups.firstOrNull()

            val activity = ActivityItem(
                id = "p2p-local-simulated",
                groupId = primaryGroup?.id.orEmpty(),
                groupName = primaryGroup?.name ?: "Melbourne Uni Squad",
                title = "What are you doing together right now?",
                description = "test_user is nearby. Capture this moment with text, photo, audio, or video.",
                type = ActivityType.P2P_ALERT,
                status = ActivityStatus.PENDING,
                dueLabel = "Created just now · Nearby",
                sharedGroupIds = sharedGroupIds
            )

            uiState = uiState.copy(
                activities = listOf(activity) + uiState.activities.filterNot { it.id == activity.id },
                p2pAlert = activity,
                p2pAlertDismissed = false,
                userGroups = groups
            )
        }
    }

    private fun observeNearbyMembers() {
        viewModelScope.launch {
            nearbyManager.connectedMembers.collect { members ->

                val activity =
                    members.firstOrNull()?.let { member ->
                        val sharedGroupIds = if (member.sharedGroupIds.isNotEmpty()) {
                            member.sharedGroupIds
                        } else {
                            listOfNotNull(member.sharedGroupId)
                        }

                        ActivityItem(
                            id = "p2p-local-${member.userId ?: member.endpointId}",
                            groupId = sharedGroupIds.firstOrNull().orEmpty(),
                            groupName = "Shared group",
                            title = "What are you doing together right now?",
                            description = "${member.endpointName} is nearby. Capture this moment with text, photo, audio, or video.",
                            type = ActivityType.P2P_ALERT,
                            status = ActivityStatus.PENDING,
                            dueLabel = "Created just now · Nearby",
                            sharedGroupIds = sharedGroupIds
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
