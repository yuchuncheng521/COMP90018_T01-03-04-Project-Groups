package com.knot.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferencesScreen(
    viewModel: AccountSettingsViewModel,
    onBackClick: () -> Unit,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

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
                    text = "Colour Theme",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Default", "Monochrome", "Invert").forEach { theme ->
                        FilterChip(
                            selected = uiState.appTheme == theme,
                            onClick = { viewModel.setAppTheme(theme, context) },
                            label = { Text(theme) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Text Size Setting
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Text Size",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                val notchMultipliers = listOf(1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f)
                val currentNotch = notchMultipliers.indexOfFirst {
                    kotlin.math.abs(it - uiState.textSizeMultiplier) < 0.1f
                }.coerceAtLeast(0)

                Slider(
                    value = currentNotch.toFloat(),
                    onValueChange = { floatVal ->
                        val notchIndex = kotlin.math.round(floatVal).toInt().coerceIn(0, notchMultipliers.lastIndex)
                        viewModel.setTextSize(notchMultipliers[notchIndex], context)
                    },
                    valueRange = 0f..(notchMultipliers.size - 1).toFloat(),
                    steps = notchMultipliers.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Default", style = MaterialTheme.typography.bodySmall)
                    Text("Larger", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
