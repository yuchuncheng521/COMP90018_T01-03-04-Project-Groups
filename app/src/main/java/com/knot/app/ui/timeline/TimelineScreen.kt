package com.knot.app.ui.timeline

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.knot.app.model.Memory
import com.knot.app.model.MemoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    groupId: String,
    groupName: String,
    viewModel: TimelineViewModel,
    onBackClick: () -> Unit = {}
) {
    LaunchedEffect(groupId) {
        viewModel.loadTimeline(groupId)
    }

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentWeek?.monthLabel ?: groupName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(padding)
            uiState.weeks.isEmpty() -> EmptyTimelineState(padding)
            else -> WeeklySpread(padding, uiState, viewModel)
        }
    }
}

@Composable
private fun WeeklySpread(padding: PaddingValues, uiState: TimelineUiState, viewModel: TimelineViewModel) {
    val week = uiState.currentWeek ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .pointerInput(uiState.currentWeekIndex) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) viewModel.goToNextWeek()
                    if (dragAmount > 20) viewModel.goToPreviousWeek()
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.goToPreviousWeek() }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
            }
            Text(week.weekLabel, style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { viewModel.goToNextWeek() }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next week")
            }
        }

        val weeksInMonth = uiState.weeks.filter { it.monthLabel == week.monthLabel }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            weeksInMonth.forEach { w ->
                val isCurrent = w.weekStartMillis == week.weekStartMillis
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (isCurrent) 7.dp else 5.dp)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u201c${week.questionText}\u201d",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(week.memories, key = { it.id }) { memory ->
                AnswerCard(memory)
            }
        }
    }
}

@Composable
private fun AnswerCard(memory: Memory) {
    when (memory.type) {
        MemoryType.TEXT -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .rotate(-2f)
                .background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(memory.textContent, style = MaterialTheme.typography.bodySmall)
        }

        MemoryType.PHOTO, MemoryType.VIDEO -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
        ) {
            Text(
                memory.authorName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            if (memory.type == MemoryType.VIDEO) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.align(Alignment.Center))
            }
        }

        MemoryType.AUDIO -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play audio")
            Text(
                memory.authorName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
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
private fun EmptyTimelineState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "No memories yet",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Answer this week's question or share a moment to start this group's timeline.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}