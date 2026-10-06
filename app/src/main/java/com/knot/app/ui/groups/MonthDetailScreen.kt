package com.knot.app.ui.groups

import android.widget.Toast
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.knot.app.R
import com.knot.app.model.Memory
import com.knot.app.model.MemoryType
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotSand
import com.knot.app.ui.theme.KnotTheme


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

    val currentWeek = uiState.currentWeek

    Scaffold(
        containerColor = KnotCream,
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
                        tint = KnotDarkBrown
                    )
                }
                Text(
                    text = "${java.text.DateFormatSymbols().months[month - 1]} $year",
                    fontFamily = JudsonFontFamily,
                    fontSize = 28.sp,
                    color = KnotDarkBrown,
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
                    onClick = { viewModel.goToPreviousWeek() },
                    enabled = uiState.currentWeekIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week",
                        tint = KnotDarkBrown
                    )
                }
                Text(
                    text = currentWeek?.weekLabel ?: "No responses yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KnotDarkBrown,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { viewModel.goToNextWeek() },
                    enabled = uiState.currentWeekIndex < uiState.weeks.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week",
                        tint = KnotDarkBrown
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KnotSand, RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "\u201c${currentWeek?.questionText ?: "No prompt answered this week"}\u201d",
                    style = MaterialTheme.typography.titleMedium,
                    color = KnotDarkBrown,
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
                items(currentWeek?.memories ?: emptyList(), key = { it.id }) { memory ->
                    val isMine = memory.authorId == viewModel.currentUserId
                    var menuExpanded by remember(memory.id) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(
                                KnotSand.copy(alpha = 0.4f),
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
                                color = KnotDarkBrown,
                                modifier = Modifier.padding(end = if (isMine) 40.dp else 0.dp)
                            )

                            Spacer(Modifier.height(8.dp))

                            when (memory.type) {
                                MemoryType.TEXT -> {
                                    Text(
                                        text = memory.textContent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KnotDarkBrown
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
                                    Text(
                                        text = "Audio response",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KnotDarkBrown
                                    )
                                }

                                MemoryType.VIDEO -> {
                                    Text(
                                        text = "Video response",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KnotDarkBrown
                                    )
                                }
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