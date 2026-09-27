package com.knot.app.ui.groups

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.knot.app.crypto.GroupKeyManager
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
    val justCreatedGroup: Group? = null,
    val memberAvatarInfo: Map<String, com.knot.app.data.MemberAvatarInfo> = emptyMap(),
    val actionError: String? = null,        // errors from leave/delete/remove member
    val memberNames: Map<String, String> = emptyMap() // id -> display name, for the "remove a member" picker
)

class GroupsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GroupsRepository = GroupsRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AndroidViewModel(application) {

    var uiState by mutableStateOf(GroupsUiState())
        private set

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    init {
        loadGroups()
    }

    fun loadGroups() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val groups = repository.getGroups()
            uiState = uiState.copy(isLoading = false, groups = groups)

            val allMemberIds = groups.flatMap { it.memberIds }.distinct()
            if (allMemberIds.isNotEmpty()) {
                launch {
                    val info = repository.getMemberAvatarInfo(allMemberIds)
                    uiState = uiState.copy(memberAvatarInfo = info)
                }
            }

            val myUid = auth.currentUser?.uid ?: return@launch
            groups.filter { it.ownerId == myUid }.forEach { group ->
                launch {
                    runCatching {
                        GroupKeyManager.syncMissingMemberKeys(
                            getApplication(), group.id, group.memberIds, myUid
                        )
                    }
                }
            }
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
            result.onSuccess { group ->
                runCatching {
                    GroupKeyManager.ensureGroupKeyAsCreator(getApplication(), group.id, group.ownerId)
                }
            }
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

    /** Current user leaves the group -- just removeMember() with their own id. */
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val result = repository.removeMember(groupId, currentUserId)
            result.onSuccess { loadGroups() }
                .onFailure { uiState = uiState.copy(actionError = it.message ?: "Couldn't leave the group.") }
        }
    }

    /** Owner deletes the whole group. */
    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            val result = repository.deleteGroup(groupId)
            result.onSuccess { loadGroups() }
                .onFailure { uiState = uiState.copy(actionError = it.message ?: "Couldn't delete the group.") }
        }
    }

    /** Owner removes a specific member (not themselves). */
    fun removeMember(groupId: String, memberId: String) {
        viewModelScope.launch {
            val result = repository.removeMember(groupId, memberId)
            result.onSuccess { loadGroups() }
                .onFailure { uiState = uiState.copy(actionError = it.message ?: "Couldn't remove that member.") }
        }
    }

    /** Loads display names for a group's members, for the "remove a member" picker. */
    fun loadMemberNames(memberIds: List<String>) {
        viewModelScope.launch {
            val names = repository.getMemberNames(memberIds)
            uiState = uiState.copy(memberNames = names)
        }
    }

    fun dismissActionError() {
        uiState = uiState.copy(actionError = null)
    }
}