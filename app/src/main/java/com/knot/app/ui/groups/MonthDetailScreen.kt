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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotSand
import com.knot.app.ui.theme.KnotTheme
import com.knot.app.model.MemoryType


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
                items(currentWeek?.memories ?: emptyList()) { memory ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(
                                KnotSand.copy(alpha = 0.4f),
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
                                color = KnotDarkBrown
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
                    }
                }
            }
        }
    }
}


