package com.knot.app.ui.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.GroupsRepository
import com.knot.app.model.Group
import kotlinx.coroutines.launch

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groups: List<Group> = emptyList()
)

class GroupsViewModel @JvmOverloads constructor(
    private val repository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    var uiState by mutableStateOf(GroupsUiState())
        private set

    init {
        loadGroups()
    }

    fun loadGroups() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val groups = repository.getGroups()
            uiState = GroupsUiState(isLoading = false, groups = groups)
        }
    }
}
