package com.knot.app.ui.activities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.ActivitiesRepository
import com.knot.app.data.GroupsRepository
import com.knot.app.model.Group
import kotlinx.coroutines.launch

data class CreateActivityUiState(
    val isLoadingGroups: Boolean = true,
    val groups: List<Group> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val created: Boolean = false
)

class CreateActivityViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val activitiesRepository: ActivitiesRepository = ActivitiesRepository()
) : ViewModel() {

    var uiState by mutableStateOf(CreateActivityUiState())
        private set

    init {
        loadGroups()
    }

    fun loadGroups() {
        uiState = uiState.copy(isLoadingGroups = true)
        viewModelScope.launch {
            val groups = groupsRepository.getGroups()
            uiState = uiState.copy(isLoadingGroups = false, groups = groups)
        }
    }

    /** Creates the activity and assigns it to every member of [group] (see ActivitiesRepository.createActivity). */
    fun createActivity(group: Group?, title: String, description: String) {
        if (group == null) {
            uiState = uiState.copy(errorMessage = "Please select a group.")
            return
        }
        if (title.isBlank()) {
            uiState = uiState.copy(errorMessage = "Please enter a title.")
            return
        }

        uiState = uiState.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val result = activitiesRepository.createActivity(
                groupId = group.id,
                groupName = group.name,
                title = title.trim(),
                description = description.trim(),
                memberIds = group.memberIds
            )
            uiState = result.fold(
                onSuccess = { uiState.copy(isSubmitting = false, created = true) },
                onFailure = { uiState.copy(isSubmitting = false, errorMessage = it.message ?: "Couldn't create the activity.") }
            )
        }
    }

    fun dismissError() {
        uiState = uiState.copy(errorMessage = null)
    }
}
