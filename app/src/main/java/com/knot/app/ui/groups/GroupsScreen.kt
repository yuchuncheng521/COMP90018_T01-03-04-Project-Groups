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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.model.Group
import com.knot.app.ui.theme.JudsonFontFamily
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
        EmptyGroupsState(padding = PaddingValues())
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupsListPreview() {
    KnotTheme {
        GroupsList(
            padding = PaddingValues(),
            groups = listOf(
                Group(
                    id = "1",
                    name = "The Family",
                    memberCount = 5,
                    lastActivitySummary = "Sarah posted 2 photos",
                    unreadCount = 3
                ),
                Group(
                    id = "2",
                    name = "Uni Friends",
                    memberCount = 8,
                    lastActivitySummary = "",
                    unreadCount = 0
                )
            ),
            onGroupClick = {}
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (Group) -> Unit = {},
    onCreateGroupClick: () -> Unit = {}
) {
    val uiState = viewModel.uiState

    Scaffold(
        containerColor = KnotCream,
        topBar = {
            TopAppBar(title = { Text("Your groups")},
                    colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KnotCream,
                    scrolledContainerColor = KnotCream
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGroupClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New group") }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(padding)
            uiState.groups.isEmpty() -> EmptyGroupsState(padding)
            else -> GroupsList(padding, uiState.groups, onGroupClick)
        }
    }
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
private fun EmptyGroupsState(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Text(
            text = "Groups",
            fontFamily = JudsonFontFamily,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            color = KnotDarkBrown,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp)
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(32.dp, vertical =200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
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
                text = "Create a private circle with your loved ones to start sharing weekly memories.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GroupsList(
    padding: PaddingValues,
    groups: List<Group>,
    onGroupClick: (Group) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Text(
            text = "Groups",
            fontFamily = JudsonFontFamily,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            color = KnotDarkBrown,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)

        ) {
            items(groups, key = { it.id }) { group ->
                GroupCard(group = group, onClick = { onGroupClick(group) })
            }
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


