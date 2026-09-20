package com.knot.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.model.Group
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotTheme

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    KnotTheme {
        LoadingState(padding = PaddingValues())
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyGroupsStatePreview() {
    KnotTheme {
        EmptyGroupsState(padding = PaddingValues(), onJoinClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupsListPreview() {
    KnotTheme {
        GroupsList(
            padding = PaddingValues(),
            groups = listOf(
                Group(id = "1", name = "The Family", memberCount = 5, lastActivitySummary = "Sarah posted 2 photos", unreadCount = 3),
                Group(id = "2", name = "Uni Friends", memberCount = 8, lastActivitySummary = "", unreadCount = 0)
            ),
            onGroupClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (Group) -> Unit = {}
) {
    val uiState = viewModel.uiState

    Scaffold(
        containerColor = KnotCream,
        topBar = {
            TopAppBar(
                title = { Text("Groups", fontSize = 38.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KnotCream,
                    scrolledContainerColor = KnotCream
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Button(
                onClick = { viewModel.openJoinOrCreateSheet(JoinOrCreateMode.CREATE) },
                colors = ButtonDefaults.buttonColors(containerColor = KnotDarkBrown, contentColor = KnotCream),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier.width(360.dp).padding(horizontal = 24.dp).height(52.dp)
            ) {
                Text("New group", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(padding)
            uiState.groups.isEmpty() -> EmptyGroupsState(
                padding = padding,
                onJoinClick = { viewModel.openJoinOrCreateSheet(JoinOrCreateMode.JOIN) }
            )
            else -> GroupsList(padding, uiState.groups, onGroupClick)
        }
    }

    if (uiState.isJoinOrCreateSheetOpen) {
        JoinOrCreateDialog(
            mode = uiState.joinOrCreateMode,
            isSubmitting = uiState.isSubmittingJoinOrCreate,
            errorMessage = uiState.joinOrCreateError,
            onModeChange = { viewModel.openJoinOrCreateSheet(it) },
            onDismiss = { viewModel.dismissJoinOrCreateSheet() },
            onCreate = { name -> viewModel.createGroup(name) },
            onJoin = { code -> viewModel.joinGroup(code) }
        )
    }

    uiState.justCreatedGroup?.let { group ->
        InviteCodeDialog(group = group, onDismiss = { viewModel.dismissJustCreatedBanner() })
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyGroupsState(padding: PaddingValues, onJoinClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier.fillMaxSize().weight(1f).padding(32.dp, vertical = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Groups,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No groups yet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextButton(onClick = onJoinClick, modifier = Modifier.padding(top = 8.dp)) {
                Text("Join with a code")
            }
        }
    }
}

@Composable
private fun GroupsList(padding: PaddingValues, groups: List<Group>, onGroupClick: (Group) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        items(groups, key = { it.id }) { group ->
            GroupCard(group = group, onClick = { onGroupClick(group) })
        }
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${group.memberCount} Members",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(group.memberCount) {
                    MemberAvatarPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun MemberAvatarPlaceholder() {
    Box(
        modifier = Modifier.size(56.dp).background(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            CircleShape
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinOrCreateDialog(
    mode: JoinOrCreateMode,
    isSubmitting: Boolean,
    errorMessage: String?,
    onModeChange: (JoinOrCreateMode) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit
) {
    var text by remember(mode) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == JoinOrCreateMode.CREATE) "Create a group" else "Join a group") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == JoinOrCreateMode.CREATE,
                        onClick = { onModeChange(JoinOrCreateMode.CREATE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Create") }
                    SegmentedButton(
                        selected = mode == JoinOrCreateMode.JOIN,
                        onClick = { onModeChange(JoinOrCreateMode.JOIN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Join") }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (mode == JoinOrCreateMode.CREATE) "Group name" else "Invite code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = { if (mode == JoinOrCreateMode.CREATE) onCreate(text) else onJoin(text) }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(if (mode == JoinOrCreateMode.CREATE) "Create" else "Join")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InviteCodeDialog(group: Group, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\"${group.name}\" created!") },
        text = {
            Column {
                Text("Share this invite code with the people you want in this circle:")
                Text(
                    text = group.inviteCode,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

