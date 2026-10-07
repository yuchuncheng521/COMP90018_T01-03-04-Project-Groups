package com.knot.app.ui.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.knot.app.KnotApplication
import com.knot.app.data.ActivitiesRepository
import com.knot.app.data.UploadResponseMediaWorker
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.model.ActivityType
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.crypto.GroupKeyManager
import java.util.concurrent.TimeUnit

data class P2pGroupOption(
    val groupId: String,
    val groupName: String
)

data class ActivitiesUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityItem> = emptyList(),
    val p2pAlert: ActivityItem? = null,
    val p2pAlertDismissed: Boolean = false,
    val p2pGroupOptions: List<P2pGroupOption> = emptyList(),
    val p2pPeerName: String = "",
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

            uiState = uiState.copy(
                isLoading = false,
                activities = activities
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

            // Text encryption is unchanged: encrypted here, saved as ciphertext.
            val textToSave = if (activity != null && myUid != null && text.isNotBlank()) {
                GroupKeyManager.encryptText(getApplication(), activity.groupId, myUid, text) ?: text
            } else {
                text
            }

            val groupId = activity?.groupId ?: ""
            val hasMedia = photoPath != null || videoPath != null || audioPath != null

            // Step 1: text first. No upload happens here, so it can't wait on a big file.
            val result = repository.saveActivityResponse(
                activityId = persistedActivityId,
                groupId = groupId,
                text = textToSave,
                hasMedia = hasMedia,
                location = location
            )

            val responseId = result.getOrNull()
            if (responseId != null) {
                // Step 2: media afterwards, in the background (UploadResponseMediaWorker).
//                android.util.Log.d("PriorityTest", "TEXT SAVED  ${System.currentTimeMillis()}")
                if (hasMedia) {
                    enqueueMediaUpload(
                        responseId, groupId, persistedActivityId,
                        photoPath, videoPath, audioPath
                    )
                }

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

    private fun enqueueMediaUpload(
        responseId: String,
        groupId: String,
        activityId: String,
        photoPath: String?,
        videoPath: String?,
        audioPath: String?
    ) {
        val request = OneTimeWorkRequestBuilder<UploadResponseMediaWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    "responseId" to responseId,
                    "groupId" to groupId,
                    "activityId" to activityId,
                    "photoPath" to photoPath,
                    "videoPath" to videoPath,
                    "audioPath" to audioPath
                )
            )
            .build()

        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    fun selectP2pGroup(groupId: String): ActivityItem? {
        val option = uiState.p2pGroupOptions.firstOrNull { it.groupId == groupId }
            ?: return null

        val peerName = uiState.p2pPeerName.ifBlank { "Group member" }
        val currentAlert = uiState.p2pAlert ?: return null

        val activity = currentAlert.copy(
            id = currentAlert.id
                .takeIf { it.startsWith("p2p-local-") }
                ?: "p2p-local-${System.currentTimeMillis()}",
            groupId = option.groupId,
            groupName = option.groupName,
            description = "${peerName} is nearby. Capture this moment with text, photo, audio, or video."
        )

        uiState = uiState.copy(
            activities = listOf(activity) +
                    uiState.activities.filterNot { it.id == activity.id },
            p2pAlert = activity
        )

        return activity
    }

    fun startNearby(userName: String) {
        nearbyManager.startAdvertising(userName)
        nearbyManager.startDiscovery()
    }

    private fun observeNearbyMembers() {
        viewModelScope.launch {
            nearbyManager.connectedMembers.collect { members ->
                val member = members.firstOrNull()

                if (member == null) {
                    uiState = uiState.copy(
                        p2pAlert = null,
                        p2pAlertDismissed = false,
                        p2pGroupOptions = emptyList(),
                        p2pPeerName = ""
                    )
                    return@collect
                }

                val sharedGroupIds =
                    member.sharedGroupIds
                        .ifEmpty { listOfNotNull(member.sharedGroupId) }

                val groupNames = repository.getGroupNames(sharedGroupIds)
                val options = sharedGroupIds.map { groupId ->
                    P2pGroupOption(
                        groupId = groupId,
                        groupName = groupNames[groupId] ?: "Shared group"
                    )
                }

                if (options.isEmpty()) {
                    uiState = uiState.copy(
                        p2pAlert = null,
                        p2pGroupOptions = emptyList(),
                        p2pPeerName = member.endpointName
                    )
                    return@collect
                }

                val selected = options.singleOrNull()
                val alert = ActivityItem(
                    id = "p2p-local-${member.userId ?: member.endpointId}",
                    groupId = selected?.groupId.orEmpty(),
                    groupName = selected?.groupName ?: "Choose a group",
                    title = "What are you doing together right now?",
                    description = "${member.endpointName} is nearby. Capture this moment with text, photo, audio, or video.",
                    type = ActivityType.P2P_ALERT,
                    status = ActivityStatus.PENDING,
                    dueLabel = "Created just now · Nearby"
                )

                uiState = uiState.copy(
                    activities =
                        if (selected != null) {
                            listOf(alert) +
                                    uiState.activities.filterNot { it.id == alert.id }
                        } else {
                            uiState.activities.filterNot { it.id == alert.id }
                        },
                    p2pAlert = alert,
                    p2pAlertDismissed = false,
                    p2pGroupOptions = options,
                    p2pPeerName = member.endpointName
                )
            }
        }
    }

}