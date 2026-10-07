package com.knot.app.ui.groups

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.data.ActivitiesRepository
import com.knot.app.data.TimelineRepository
import com.knot.app.model.Memory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

data class ActivityBucket(
    val activityId: String,
    val activityTitle: String,
    val dateMillis: Long,
    val memories: List<Memory>
)

data class MonthDetailUiState(
    val isLoading: Boolean = true,
    val activities: List<ActivityBucket> = emptyList(),
    val currentActivityIndex: Int = 0,
    val errorMessage: String? = null
) {
    val currentActivity: ActivityBucket?
        get() = activities.getOrNull(currentActivityIndex)
}

class MonthDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TimelineRepository(application)
    private val activitiesRepository = ActivitiesRepository()

    private var observeJob: Job? = null

    var uiState by mutableStateOf(MonthDetailUiState())
        private set

    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    /**
     * Starts listening to this group's responses. The screen updates by itself when
     * anything changes (your edits/deletes, a photo finishing its background upload,
     * other members' posts) -- no reload needed.
     */
    fun loadMonth(
        groupId: String,
        year: Int,
        month: Int
    ) {
        observeJob?.cancel()
        uiState = MonthDetailUiState(isLoading = true)

        observeJob = viewModelScope.launch {
            var firstEmission = true

            repository.observeTimelineForGroup(groupId).collect { allMemories ->
                val memories = allMemories.filter { memory ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = memory.activityCreatedAt
                    }

                    calendar.get(Calendar.YEAR) == year &&
                            calendar.get(Calendar.MONTH) + 1 == month
                }

                val activities = memories
                    .groupBy { it.activityId }
                    .map { (activityId, activityMemories) ->
                        ActivityBucket(
                            activityId = activityId,
                            activityTitle = activityMemories
                                .firstOrNull { it.activityTitle.isNotBlank() }
                                ?.activityTitle
                                ?: "Untitled activity",
                            dateMillis = activityMemories.first().activityCreatedAt,
                            memories = activityMemories.sortedBy { it.createdAt }
                        )
                    }
                    .sortedBy { it.dateMillis }

                val lastIndex = (activities.size - 1).coerceAtLeast(0)

                // First load jumps to the latest activity. After that, stay on the activity
                // you're looking at when new data arrives (found by id, so a new activity
                // appearing earlier in the list doesn't move you to a different one).
                val previousActivityId = uiState.currentActivity?.activityId
                val index =
                    if (firstEmission) {
                        lastIndex
                    } else {
                        activities.indexOfFirst { it.activityId == previousActivityId }
                            .takeIf { it >= 0 }
                            ?: uiState.currentActivityIndex.coerceIn(0, lastIndex)
                    }
                firstEmission = false

                uiState = MonthDetailUiState(
                    isLoading = false,
                    activities = activities,
                    currentActivityIndex = index,
                    errorMessage = uiState.errorMessage
                )
            }
        }
    }

    fun goToPreviousActivity() {
        val currentIndex = uiState.currentActivityIndex

        if (currentIndex > 0) {
            uiState = uiState.copy(
                currentActivityIndex = currentIndex - 1
            )
        }
    }

    fun goToNextActivity() {
        val currentIndex = uiState.currentActivityIndex

        if (currentIndex < uiState.activities.lastIndex) {
            uiState = uiState.copy(
                currentActivityIndex = currentIndex + 1
            )
        }
    }

    /** Edits only the text of the response this entry came from. The listener refreshes the screen. */
    fun editText(memory: Memory, newText: String) {
        viewModelScope.launch {
            activitiesRepository
                .editResponseText(getApplication(), memory.responseId, memory.groupId, newText)
                .onFailure { uiState = uiState.copy(errorMessage = it.message ?: "Couldn't save your edit.") }
        }
    }

    /** Deletes the WHOLE response this entry came from, including its text and any media. */
    fun deleteResponse(memory: Memory) {
        viewModelScope.launch {
            activitiesRepository
                .deleteResponse(memory.responseId)
                .onFailure { uiState = uiState.copy(errorMessage = it.message ?: "Couldn't delete the response.") }
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}

/**
 * TimelineRepository gives each entry the id "<responseId>_text" / "_photo" / "_video" / "_audio",
 * so the response document's id is everything before the last underscore. Firestore's
 * auto-generated ids don't contain underscores, so this is safe.
 */
private val Memory.responseId: String
    get() = id.substringBeforeLast('_')