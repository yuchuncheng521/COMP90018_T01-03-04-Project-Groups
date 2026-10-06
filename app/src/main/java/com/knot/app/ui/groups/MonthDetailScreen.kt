package com.knot.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.knot.app.R
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotTheme
import com.knot.app.model.MemoryType
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Preview(showBackground = true)
@Composable
private fun MonthDetailScreenPreview() {
    KnotTheme {
        MonthDetailScreen(
            groupId = "preview-group",
            year = 2026,
            month = 8,
            onBackClick = {}
        )
    }
}

@Composable
fun MonthDetailScreen(
    groupId: String,
    year: Int,
    month: Int,
    onBackClick: () -> Unit
) {
    val viewModel: MonthDetailViewModel = viewModel()
    val uiState = viewModel.uiState

    LaunchedEffect(groupId, year, month) {
        viewModel.loadMonth(
            groupId = groupId,
            year = year,
            month = month
        )
    }

    val currentActivity = uiState.currentActivity

    Scaffold(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = "Back to albums",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${java.text.DateFormatSymbols().months[month - 1]} $year",
                    fontFamily = JudsonFontFamily,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.goToPreviousActivity() },
                    enabled = uiState.currentActivityIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous activity",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = currentActivity?.let {
                        SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                            .format(Date(it.dateMillis))
                    } ?: "No activities yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.goToNextActivity() },
                    enabled = uiState.currentActivityIndex < uiState.activities.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next activity",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "\u201c${currentActivity?.activityTitle ?: "No activity yet"}\u201d",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(currentActivity?.memories ?: emptyList()) { memory ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = memory.authorName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(Modifier.height(8.dp))

                            when (memory.type) {
                                MemoryType.TEXT -> {
                                    Text(
                                        text = memory.textContent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                MemoryType.PHOTO -> {
                                    AsyncImage(
                                        model = memory.contentUrl,
                                        contentDescription = "Photo by ${memory.authorName}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                MemoryType.AUDIO -> {
                                    AudioResponsePlayer(
                                        audioUrl = memory.contentUrl
                                    )
                                }

                                MemoryType.VIDEO -> {
                                    VideoResponsePlayer(
                                        videoUrl = memory.contentUrl
                                    )
                                }
                            }

                            if (memory.locationText.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "📍 ${memory.locationText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioResponsePlayer(audioUrl: String) {
    val mediaPlayer = remember(audioUrl) {
        MediaPlayer().apply {
            setDataSource(audioUrl)
            prepareAsync()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setOnPreparedListener {
            isPrepared = true
        }

        mediaPlayer.setOnCompletionListener {
            isPlaying = false
        }

        onDispose {
            mediaPlayer.release()
        }
    }

    Button(
        onClick = {
            if (!isPrepared) return@Button

            if (isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else {
                mediaPlayer.start()
                isPlaying = true
            }
        }
    ) {
        Text(
            if (isPlaying) {
                "Pause Audio"
            } else {
                "Play Audio"
            }
        )
    }
}

@Composable
private fun VideoResponsePlayer(videoUrl: String) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)

                setMediaController(mediaController)
                setVideoURI(Uri.parse(videoUrl))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}


