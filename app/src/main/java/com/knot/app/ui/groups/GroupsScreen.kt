package com.knot.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knot.app.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (Group) -> Unit = {}
) {
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your groups") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openJoinOrCreateSheet(JoinOrCreateMode.CREATE) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New circle") }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(padding)
            uiState.groups.isEmpty() -> EmptyGroupsState(padding, onJoinClick = { viewModel.openJoinOrCreateSheet(JoinOrCreateMode.JOIN) })
            else -> GroupsList(padding, uiState.groups, onGroupClick)
        }
    }

    uiState.joinOrCreateMode?.let { mode ->
        JoinOrCreateDialog(
            initialMode = mode,
            isSubmitting = uiState.isSubmittingJoinOrCreate,
            errorMessage = uiState.joinOrCreateError,
            onCreate = viewModel::createGroup,
            onJoin = viewModel::joinGroup,
            onDismiss = viewModel::dismissJoinOrCreateSheet
        )
    }

    uiState.justCreatedGroup?.let { group ->
        InviteCodeDialog(group = group, onDismiss = viewModel::dismissJustCreatedBanner)
    }
}

/**
 * Lets the user either create a new circle (name only -- an invite code is generated for them)
 * or join one with a code someone shared with them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinOrCreateDialog(
    initialMode: JoinOrCreateMode,
    isSubmitting: Boolean,
    errorMessage: String?,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == JoinOrCreateMode.CREATE) "Start a new circle" else "Join a circle") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == JoinOrCreateMode.CREATE,
                        onClick = { mode = JoinOrCreateMode.CREATE; input = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Create") }
                    SegmentedButton(
                        selected = mode == JoinOrCreateMode.JOIN,
                        onClick = { mode = JoinOrCreateMode.JOIN; input = "" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Join with code") }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(if (mode == JoinOrCreateMode.CREATE) "Circle name" else "Invite code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == JoinOrCreateMode.CREATE) KeyboardType.Text else KeyboardType.Ascii
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting && input.isNotBlank(),
                onClick = {
                    if (mode == JoinOrCreateMode.CREATE) onCreate(input) else onJoin(input)
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text(if (mode == JoinOrCreateMode.CREATE) "Create" else "Join")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

/** Shown once, right after creating a circle, so the owner can immediately share the code. */
@Composable
private fun InviteCodeDialog(group: Group, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\"${group.name}\" is ready") },
        text = {
            Column {
                Text("Share this code with the people you want in this circle:")
                Text(
                    text = group.inviteCode,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyGroupsState(padding: PaddingValues, onJoinClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
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
        Text(
            text = "Create a private circle with your loved ones to start sharing weekly memories, or join one with an invite code.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(onClick = onJoinClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Join with a code")
        }
    }
}

@Composable
private fun GroupsList(
    padding: PaddingValues,
    groups: List<Group>,
    onGroupClick: (Group) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupAvatar(initial = group.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.memberCount} members",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (group.lastActivitySummary.isNotBlank()) {
                    Text(
                        text = group.lastActivitySummary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (group.unreadCount > 0) {
                Badge { Text(group.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun GroupAvatar(initial: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
