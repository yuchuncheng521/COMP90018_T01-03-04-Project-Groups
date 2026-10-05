package com.knot.app.ui.groups

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.TimelineRepository
import kotlinx.coroutines.launch
import com.knot.app.model.Memory
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
    val currentActivityIndex: Int = 0
) {
    val currentActivity: ActivityBucket?
        get() = activities.getOrNull(currentActivityIndex)
}

class MonthDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TimelineRepository(application)

    var uiState by mutableStateOf(MonthDetailUiState())
        private set

    fun loadMonth(
        groupId: String,
        year: Int,
        month: Int
    ) {
        uiState = MonthDetailUiState(isLoading = true)

        viewModelScope.launch {
            val memories = repository
                .getTimelineForGroup(groupId)
                .filter { memory ->
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

            uiState = MonthDetailUiState(
                isLoading = false,
                activities = activities,
                currentActivityIndex = (activities.size - 1).coerceAtLeast(0)
            )
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
}