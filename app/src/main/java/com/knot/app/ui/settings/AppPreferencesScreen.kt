package com.knot.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.R
import com.knot.app.ui.theme.KnotDarkBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferencesScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
) {
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Preference", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.left_arrow),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Theme Setting
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleLarge,
                    color = KnotDarkBrown
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light", "Dark", "System").forEach { theme ->
                        FilterChip(
                            selected = uiState.appTheme == theme,
                            onClick = { viewModel.setAppTheme(theme) },
                            label = { Text(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Text Size Setting
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Text Size",
                    style = MaterialTheme.typography.titleLarge,
                    color = KnotDarkBrown
                )
                Slider(
                    value = uiState.textSizeMultiplier,
                    onValueChange = { viewModel.setTextSize(it) },
                    valueRange = 0.8f..1.5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = KnotDarkBrown,
                        activeTrackColor = KnotDarkBrown
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", style = MaterialTheme.typography.bodySmall)
                    Text("Large", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
