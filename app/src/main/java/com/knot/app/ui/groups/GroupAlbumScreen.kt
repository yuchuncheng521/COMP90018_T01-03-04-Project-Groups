package com.knot.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotSand
import com.knot.app.ui.theme.KnotTheme
import com.knot.app.R


@Composable
fun GroupDetailScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onCreateNewClick: () -> Unit = {}
) {
    // TODO: replace with a real ViewModel fetching this group's albums by groupId
    val albums = listOf("January 2026", "February 2026", "March 2026", "April 2026", "May 2026")

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
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = "Back to groups"
                    )
                }
                Text(
                    text = "Books",
                    fontFamily = JudsonFontFamily,
                    fontSize = 28.sp,
                    color = KnotDarkBrown,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Button(
                onClick = onCreateNewClick,
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
                Text("Create new", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(albums) { album ->
                AlbumPill(label = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
private fun AlbumPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, KnotDarkBrown, RoundedCornerShape(12.dp))
            .background(KnotSand, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KnotDarkBrown,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupDetailScreenPreview() {
    KnotTheme {
        GroupDetailScreen(
            groupId = "1",
            onBackClick = {}
        )
    }
}

