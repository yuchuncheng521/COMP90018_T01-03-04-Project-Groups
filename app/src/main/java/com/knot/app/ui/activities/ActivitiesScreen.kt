package com.knot.app.ui.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.ui.components.P2PAlertBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    onActivityClick: (ActivityItem) -> Unit = {}
) {
    val uiState = viewModel.uiState

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your activities") }) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- P2P alert placeholder banner, always pinned to the top of the list ---
            val alert = uiState.p2pAlert
            if (alert != null && !uiState.p2pAlertDismissed) {
                item(key = "p2p-alert") {
                    AnimatedVisibility(visible = true) {
                        P2PAlertBanner(
                            alert = alert,
                            onRespond = { onActivityClick(alert) },
                            onDismiss = { viewModel.dismissP2pAlert() }
                        )
                    }
                }
            }

            if (uiState.activities.isEmpty()) {
                item {
                    Text(
                        text = "No activities right now. Check back for the next weekly prompt!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(uiState.activities, key = { it.id }) { activity ->
                    ActivityCard(
                        activity = activity,
                        onClick = { onActivityClick(activity) },
                        onToggleComplete = { viewModel.markCompleted(activity.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val isCompleted = activity.status == ActivityStatus.COMPLETED

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Completed" else "Mark as complete",
                    tint = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = activity.groupName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Unspecified
                )
                if (activity.dueLabel.isNotBlank()) {
                    Text(
                        text = activity.dueLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
