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
    val groups: List<Group> = emptyList(),
    /** Non-null while the create/join sheet is open. */
    val joinOrCreateMode: JoinOrCreateMode? = null,
    val isSubmittingJoinOrCreate: Boolean = false,
    val joinOrCreateError: String? = null,
    /** Set right after creating a circle, so the UI can show "share this code" once. */
    val justCreatedGroup: Group? = null
)

enum class JoinOrCreateMode { CREATE, JOIN }

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
        uiState = uiState.copy(joinOrCreateMode = mode, joinOrCreateError = null, justCreatedGroup = null)
    }

    fun dismissJoinOrCreateSheet() {
        uiState = uiState.copy(joinOrCreateMode = null, joinOrCreateError = null, isSubmittingJoinOrCreate = false)
    }

    fun createGroup(name: String) {
        uiState = uiState.copy(isSubmittingJoinOrCreate = true, joinOrCreateError = null)
        viewModelScope.launch {
            val result = repository.createGroup(name)
            uiState = result.fold(
                onSuccess = { group ->
                    uiState.copy(
                        isSubmittingJoinOrCreate = false,
                        joinOrCreateMode = null,
                        justCreatedGroup = group,
                        groups = uiState.groups + group
                    )
                },
                onFailure = { error ->
                    uiState.copy(isSubmittingJoinOrCreate = false, joinOrCreateError = error.message ?: "Couldn't create that circle. Please try again.")
                }
            )
        }
    }

    fun joinGroup(code: String) {
        uiState = uiState.copy(isSubmittingJoinOrCreate = true, joinOrCreateError = null)
        viewModelScope.launch {
            val result = repository.joinGroupByCode(code)
            uiState = result.fold(
                onSuccess = { group ->
                    val alreadyPresent = uiState.groups.any { it.id == group.id }
                    uiState.copy(
                        isSubmittingJoinOrCreate = false,
                        joinOrCreateMode = null,
                        groups = if (alreadyPresent) uiState.groups.map { if (it.id == group.id) group else it } else uiState.groups + group
                    )
                },
                onFailure = { error ->
                    uiState.copy(isSubmittingJoinOrCreate = false, joinOrCreateError = error.message ?: "Couldn't join that circle. Please try again.")
                }
            )
        }
    }

    fun dismissJustCreatedBanner() {
        uiState = uiState.copy(justCreatedGroup = null)
    }
}
