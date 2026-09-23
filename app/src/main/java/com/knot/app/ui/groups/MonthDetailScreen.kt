package com.knot.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.knot.app.R
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
            monthLabel = "August",
            onBackClick = {}
        )
    }
}

@Composable
fun MonthDetailScreen(
    monthLabel: String, // e.g. "August" — passed straight through from whatever AlbumPill was tapped
    onBackClick: () -> Unit
) {
    // TODO: replace all three with a real ViewModel — weeks/prompt/responses for this album, from Firestore
    val weeks = listOf("Week 1 - Aug 25-31", "Week 2 - Sep 1-7", "Week 3 - Sep 8-14")
    var weekIndex by remember { mutableStateOf(0) }
    val prompt = "What made you smile"
    val memberResponses = listOf("Member", "Member", "Member")

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
                    text = monthLabel,
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
                    onClick = { weekIndex-- },
                    enabled = weekIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week",
                        tint = KnotDarkBrown
                    )
                }
                Text(
                    text = weeks[weekIndex],
                    style = MaterialTheme.typography.bodyLarge,
                    color = KnotDarkBrown,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { weekIndex++ },
                    enabled = weekIndex < weeks.lastIndex
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
                    text = "\u201c$prompt\u201d",
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
                items(memberResponses) { memberName ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .background(KnotSand.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = memberName,
                            style = MaterialTheme.typography.labelMedium,
                            color = KnotDarkBrown,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }
        }
    }
}


