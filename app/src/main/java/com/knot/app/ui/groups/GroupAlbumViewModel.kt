package com.knot.app.ui.groups

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.TimelineRepository
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

data class AlbumItem(
    val year: Int,
    val month: Int,
    val label: String
)

data class GroupAlbumUiState(
    val isLoading: Boolean = false,
    val albums: List<AlbumItem> = emptyList()
)

class GroupAlbumViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TimelineRepository(application)

    var uiState by mutableStateOf(GroupAlbumUiState())
        private set

    fun loadAlbums(groupId: String) {
        uiState = uiState.copy(isLoading = true)

        viewModelScope.launch {
            val memories = repository.getTimelineForGroup(groupId)

            val albums = memories
                .map { memory ->
                    Calendar.getInstance().apply {
                        timeInMillis = memory.createdAt
                    }.let { calendar ->
                        Triple(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar
                        )
                    }
                }
                .distinctBy { (year, month, _) ->
                    year to month
                }
                .sortedWith(
                    compareByDescending<Triple<Int, Int, Calendar>> { it.first }
                        .thenByDescending { it.second }
                )
                .map { (year, month, _) ->
                    AlbumItem(
                        year = year,
                        month = month,
                        label = "${
                            DateFormatSymbols().months[month - 1]
                        } $year"
                    )
                }

            uiState = GroupAlbumUiState(
                isLoading = false,
                albums = albums
            )
        }
    }
}

