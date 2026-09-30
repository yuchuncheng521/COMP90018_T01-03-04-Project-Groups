package com.knot.app.ui.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.GroupsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val months: List<String> = emptyList()
)

class GroupDetailViewModel(
    private val repository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    var uiState by mutableStateOf(GroupDetailUiState())
        private set

    fun loadGroup(groupId: String) {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val group = repository.getGroup(groupId)
            val months = if (group != null && group.createdAt > 0) {
                monthsSince(group.createdAt)
            } else {
                // No createdAt yet (e.g. groups created before this field existed,
                // or the group failed to load) -- empty is safer than a wrong list.
                emptyList()
            }
            uiState = uiState.copy(isLoading = false, months = months)
        }
    }

    /** Every month from the group's creation date up to and including the current month, oldest first. */
    private fun monthsSince(createdAtMillis: Long): List<String> {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val cursor = Calendar.getInstance().apply {
            timeInMillis = createdAtMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val end = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

        val months = mutableListOf<String>()
        while (!cursor.after(end)) {
            months.add(formatter.format(cursor.time))
            cursor.add(Calendar.MONTH, 1)
        }
        return months
    }
}

