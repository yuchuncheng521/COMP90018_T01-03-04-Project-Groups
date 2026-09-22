package com.knot.app.ui.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knot.app.data.GroupsRepository
import com.knot.app.model.Group
import kotlinx.coroutines.launch

enum class JoinOrCreateMode { CREATE, JOIN }

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groups: List<Group> = emptyList(),
    val isJoinOrCreateSheetOpen: Boolean = false,
    val joinOrCreateMode: JoinOrCreateMode = JoinOrCreateMode.CREATE,
    val isSubmittingJoinOrCreate: Boolean = false,
    val joinOrCreateError: String? = null,
    val justCreatedGroup: Group? = null
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
            uiState = uiState.copy(isLoading = false, groups = groups)
        }
    }

    fun openJoinOrCreateSheet(mode: JoinOrCreateMode) {
        uiState = uiState.copy(isJoinOrCreateSheetOpen = true, joinOrCreateMode = mode, joinOrCreateError = null)
    }

    fun dismissJoinOrCreateSheet() {
        uiState = uiState.copy(isJoinOrCreateSheetOpen = false, joinOrCreateError = null)
    }

    fun createGroup(name: String) {
        if (name.isBlank()) {
            uiState = uiState.copy(joinOrCreateError = "Please enter a group name.")
            return
        }
        uiState = uiState.copy(isSubmittingJoinOrCreate = true, joinOrCreateError = null)
        viewModelScope.launch {
            val result = repository.createGroup(name)
            uiState = result.fold(
                onSuccess = { group ->
                    uiState.copy(
                        isSubmittingJoinOrCreate = false,
                        isJoinOrCreateSheetOpen = false,
                        justCreatedGroup = group,
                        groups = uiState.groups + group
                    )
                },
                onFailure = { uiState.copy(isSubmittingJoinOrCreate = false, joinOrCreateError = it.message ?: "Couldn't create the group. Please try again.") }
            )
        }
    }

    fun joinGroup(code: String) {
        if (code.isBlank()) {
            uiState = uiState.copy(joinOrCreateError = "Please enter an invite code.")
            return
        }
        uiState = uiState.copy(isSubmittingJoinOrCreate = true, joinOrCreateError = null)
        viewModelScope.launch {
            val result = repository.joinGroupByCode(code)
            uiState = result.fold(
                onSuccess = { group ->
                    uiState.copy(isSubmittingJoinOrCreate = false, isJoinOrCreateSheetOpen = false, groups = uiState.groups + group)
                },
                onFailure = { uiState.copy(isSubmittingJoinOrCreate = false, joinOrCreateError = it.message ?: "Couldn't join that group. Please check the code and try again.") }
            )
        }
    }

    fun dismissJustCreatedBanner() {
        uiState = uiState.copy(justCreatedGroup = null)
    }
}
