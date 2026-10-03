package com.knot.app.ui.groups

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.TimelineRepository
import com.knot.app.ui.timeline.WeekBucket
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthDetailUiState(
    val isLoading: Boolean = true,
    val weeks: List<WeekBucket> = emptyList(),
    val currentWeekIndex: Int = 0
) {
    val currentWeek: WeekBucket?
        get() = weeks.getOrNull(currentWeekIndex)
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
                        timeInMillis = memory.createdAt
                    }

                    calendar.get(Calendar.YEAR) == year &&
                            calendar.get(Calendar.MONTH) + 1 == month
                }

            val weeks = memories
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

            uiState = MonthDetailUiState(
                isLoading = false,
                weeks = weeks,
                currentWeekIndex = (weeks.size - 1).coerceAtLeast(0)
            )
        }
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
}

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