package com.knot.app.ui.groups

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.knot.app.R
import com.knot.app.model.Memory
import com.knot.app.model.MemoryType
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
    val context = LocalContext.current

    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var editText by remember { mutableStateOf("") }
    var deletingMemory by remember { mutableStateOf<Memory?>(null) }

    LaunchedEffect(groupId, year, month) {
        viewModel.loadMonth(
            groupId = groupId,
            year = year,
            month = month
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
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
                items(currentActivity?.memories ?: emptyList(), key = { it.id }) { memory ->
                    val isMine = memory.authorId == viewModel.currentUserId
                    var menuExpanded by remember(memory.id) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = memory.authorName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = if (isMine) 40.dp else 0.dp)
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
                                    if (memory.isUploading) {
                                        MediaUploadingPlaceholder(label = "Uploading photo…", heightDp = 200)
                                    } else {
                                        AsyncImage(
                                            model = memory.contentUrl,
                                            contentDescription = "Photo by ${memory.authorName}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                MemoryType.AUDIO -> {
                                    if (memory.isUploading) {
                                        MediaUploadingPlaceholder(label = "Uploading audio…", heightDp = 64)
                                    } else {
                                        AudioResponsePlayer(
                                            audioUrl = memory.contentUrl
                                        )
                                    }
                                }

                                MemoryType.VIDEO -> {
                                    if (memory.isUploading) {
                                        MediaUploadingPlaceholder(label = "Uploading video…", heightDp = 220)
                                    } else {
                                        VideoResponsePlayer(
                                            videoUrl = memory.contentUrl
                                        )
                                    }
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

                        // Only the author sees the menu, pinned to the card's top-right corner
                        // like the Groups cards. Edit is text-only; delete removes the whole
                        // response (all of its text/media).
                        if (isMine) {
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Response options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (memory.type == MemoryType.TEXT) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                menuExpanded = false
                                                editText = memory.textContent
                                                editingMemory = memory
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete response") },
                                        onClick = {
                                            menuExpanded = false
                                            deletingMemory = memory
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingMemory?.let { memory ->
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("Edit your text") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    minLines = 1,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                // Blank is blocked: an empty text would make the text card disappear.
                // To remove a response entirely, use Delete.
                TextButton(
                    enabled = editText.isNotBlank(),
                    onClick = {
                        viewModel.editText(memory, editText)
                        editingMemory = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deletingMemory?.let { memory ->
        AlertDialog(
            onDismissRequest = { deletingMemory = null },
            title = { Text("Delete this response?") },
            text = {
                Text("This removes the whole response, including any text, photo, video or audio in it. This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteResponse(memory)
                        deletingMemory = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMemory = null }) {
                    Text("Cancel")
                }
            }
        )
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

/** Shown in place of the real photo/audio/video card while UploadResponseMediaWorker
 *  is still uploading that file in the background -- the Timeline listener swaps this
 *  out for the real content automatically once the upload finishes. */
@Composable
private fun MediaUploadingPlaceholder(label: String, heightDp: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VideoResponsePlayer(videoUrl: String) {
    // The file itself is already fully uploaded by this point (that's a separate
    // "uploading" state, shown elsewhere) -- this is just VideoView's own buffering
    // before it has enough of the stream to start playing, which takes a few seconds
    // over the network. Without this, that wait looked like a frozen/blank player.
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }
    var thumbnail by remember(videoUrl) { mutableStateOf<Bitmap?>(null) }

    // Pulls a frame straight from the already-uploaded video (no extra download
    // step, no new dependency) so there's a real picture to look at instead of a
    // black rectangle while VideoView buffers. Best-effort: if this fails for any
    // reason, the plain loading placeholder below still covers it.
    LaunchedEffect(videoUrl) {
        thumbnail = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoUrl, HashMap<String, String>())
                retriever.getFrameAtTime(0L)
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)

                    setMediaController(mediaController)
                    setOnPreparedListener { isPrepared = true }
                    setVideoURI(Uri.parse(videoUrl))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        if (!isPrepared) {
            val frame = thumbnail
            if (frame != null) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = "Video preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                MediaUploadingPlaceholder(label = "Loading video…", heightDp = 220)
            }
        }
    }
}