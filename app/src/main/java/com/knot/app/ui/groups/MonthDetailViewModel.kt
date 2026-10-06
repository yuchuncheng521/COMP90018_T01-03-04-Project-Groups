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
import com.knot.app.ui.timeline.WeekBucket
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthDetailUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeekBucket> = emptyList(),
    val currentWeekIndex: Int = 0,
    val errorMessage: String? = null
) {
    val currentWeek: WeekBucket?
        get() = weeks.getOrNull(currentWeekIndex)
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
     * Starts listening to this group's responses. The screen now updates by itself when
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
                        timeInMillis = memory.createdAt
                    }

                    calendar.get(Calendar.YEAR) == year &&
                            calendar.get(Calendar.MONTH) + 1 == month
                }

                val weeks = buildWeeks(memories)
                val lastIndex = (weeks.size - 1).coerceAtLeast(0)

                // First load jumps to the latest week; after that, stay on the week
                // you're looking at when new data arrives.
                val index =
                    if (firstEmission) lastIndex
                    else uiState.currentWeekIndex.coerceIn(0, lastIndex)
                firstEmission = false

                uiState = MonthDetailUiState(
                    isLoading = false,
                    weeks = weeks,
                    currentWeekIndex = index,
                    errorMessage = uiState.errorMessage
                )
            }
        }
    }

    private fun buildWeeks(memories: List<Memory>): List<WeekBucket> =
        memories
            .sortedBy { it.createdAt }
            .groupBy { weekStartMillisFor(it.createdAt) }
            .toSortedMap()
            .map { (weekStart, weekMemories) ->

                val questionText =
                    weekMemories
                        .firstOrNull { it.activityTitle.isNotBlank() }
                        ?.activityTitle
                        ?: "No prompt answered this week"

                WeekBucket(
                    weekStartMillis = weekStart,
                    monthLabel = monthLabelFor(weekStart),
                    weekLabel = weekLabelFor(weekStart),
                    questionText = questionText,
                    memories = weekMemories
                )
            }

    fun goToPreviousWeek() {
        if (uiState.currentWeekIndex > 0) {
            uiState = uiState.copy(
                currentWeekIndex = uiState.currentWeekIndex - 1
            )
        }
    }

    fun goToNextWeek() {
        if (uiState.currentWeekIndex < uiState.weeks.size - 1) {
            uiState = uiState.copy(
                currentWeekIndex = uiState.currentWeekIndex + 1
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

private fun weekStartMillisFor(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return cal.timeInMillis
}

private fun monthLabelFor(weekStartMillis: Long): String =
    SimpleDateFormat(
        "MMMM",
        Locale.getDefault()
    ).format(weekStartMillis)

private fun weekLabelFor(weekStartMillis: Long): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = weekStartMillis
    }

    val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)

    val start = SimpleDateFormat(
        "MMM d",
        Locale.getDefault()
    ).format(weekStartMillis)

    val end = SimpleDateFormat(
        "MMM d",
        Locale.getDefault()
    ).format(
        weekStartMillis +
                6L * 24 * 60 * 60 * 1000
    )

    return "Week $weekOfMonth · $start–$end"
}