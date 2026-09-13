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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
        topBar = { TopAppBar(title = {
                Text("Groups",
                fontSize = 38.sp

                ) },
                    colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KnotCream,
                    scrolledContainerColor = KnotCream
                )
            )
                 },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Button(
                onClick = onCreateGroupClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KnotDarkBrown,
                    contentColor = KnotCream
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .width(360.dp)
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
            ) {
                Text("New group",style = MaterialTheme.typography.titleMedium)
            }
        }
    )


    { padding ->
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(32.dp, vertical =200.dp),
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
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
        modifier = Modifier
            .size(56.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                CircleShape
            )
    )

}


