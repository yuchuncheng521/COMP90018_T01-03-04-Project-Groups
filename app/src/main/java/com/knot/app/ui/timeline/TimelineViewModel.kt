package com.knot.app.ui.timeline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.ActivitiesRepository
import com.knot.app.data.TimelineRepository
import com.knot.app.model.Memory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One week's worth of the shared timeline: its date range, prompt, and answers. */
data class WeekBucket(
    val weekStartMillis: Long,
    val monthLabel: String,      // "August" — shown as the screen title
    val weekLabel: String,       // "Week 4 · Aug 25–31"
    val questionText: String,    // resolved from the memories' activityId, if any
    val memories: List<Memory>
)

data class TimelineUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeekBucket> = emptyList(),
    val currentWeekIndex: Int = 0
) {
    val currentWeek: WeekBucket? get() = weeks.getOrNull(currentWeekIndex)
}

class TimelineViewModel @JvmOverloads constructor(
    private val repository: TimelineRepository = TimelineRepository(),
    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository()
) : ViewModel() {

    var uiState by mutableStateOf(TimelineUiState())
        private set

    fun loadTimeline(groupId: String) {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val memories = repository.getTimelineForGroup(groupId)
            // Map activityId -> question title, so each week can show its prompt.
            val questionsById = runCatching { activitiesRepository.getActivities() }
                .getOrDefault(emptyList())
                .filter { it.groupId == groupId }
                .associate { it.id to it.title }

            val weeks = memories
                .sortedBy { it.createdAt }
                .groupBy { weekStartMillisFor(it.createdAt) }
                .toSortedMap()
                .map { (weekStart, weekMemories) ->
                    val questionText = weekMemories
                        .firstNotNullOfOrNull { questionsById[it.activityId] }
                        ?: "No prompt answered yet this week"
                    WeekBucket(
                        weekStartMillis = weekStart,
                        monthLabel = monthLabelFor(weekStart),
                        weekLabel = weekLabelFor(weekStart),
                        questionText = questionText,
                        memories = weekMemories
                    )
                }

            uiState = TimelineUiState(
                isLoading = false,
                weeks = weeks,
                currentWeekIndex = (weeks.size - 1).coerceAtLeast(0) // open on the most recent week
            )
        }
    }

    fun goToPreviousWeek() {
        if (uiState.currentWeekIndex > 0) {
            uiState = uiState.copy(currentWeekIndex = uiState.currentWeekIndex - 1)
        }
    }

    fun goToNextWeek() {
        if (uiState.currentWeekIndex < uiState.weeks.size - 1) {
            uiState = uiState.copy(currentWeekIndex = uiState.currentWeekIndex + 1)
        }
    }

    /** Jump directly to a page — used by swipe gestures in the UI. */
    fun goToWeekIndex(index: Int) {
        if (index in uiState.weeks.indices) {
            uiState = uiState.copy(currentWeekIndex = index)
        }
    }
}

private fun weekStartMillisFor(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun monthLabelFor(weekStartMillis: Long): String =
    SimpleDateFormat("MMMM", Locale.getDefault()).format(weekStartMillis)

private fun weekLabelFor(weekStartMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
    val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
    val start = SimpleDateFormat("MMM d", Locale.getDefault()).format(weekStartMillis)
    val end = SimpleDateFormat("d", Locale.getDefault()).format(weekStartMillis + 6L * 24 * 60 * 60 * 1000)
    return "Week $weekOfMonth · $start–$end"
}