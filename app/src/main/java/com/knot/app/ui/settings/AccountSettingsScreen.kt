package com.knot.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.knot.app.permissions.PermissionManager
import android.widget.Toast
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onSignedOut: () -> Unit
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!PermissionManager.isBluetoothEnabled(context)) {
            viewModel.setP2pAlertsEnabled(false)
        }
    }

    DisposableEffect(Unit) {

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )

                    if (state == BluetoothAdapter.STATE_OFF) {
                        viewModel.setP2pAlertsEnabled(false)

                        Toast.makeText(
                            context,
                            "Bluetooth was turned off. P2P proximity alerts have been disabled.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        val filter =
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        context.registerReceiver(
            receiver,
            filter
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val nearbyPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                            permissions[Manifest.permission.BLUETOOTH_ADVERTISE] == true &&
                            permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                    permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                            permissions[Manifest.permission.BLUETOOTH_ADVERTISE] == true

                } else {
                    true
                }

            if (granted) {
                viewModel.setP2pAlertsEnabled(true)
                viewModel.startNearby()
            } else {
                viewModel.setP2pAlertsEnabled(false)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Account settings")
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // -------------------------
            // Profile
            // -------------------------

            item {
                ProfileHeader(
                    displayName = uiState.account.displayName,
                    email = uiState.account.email
                )

                HorizontalDivider()
            }

            // -------------------------
            // Notifications
            // -------------------------

            item {
                SectionLabel("Notifications")
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Notifications,
                    title = "Push notifications",
                    subtitle = "Get notified about new prompts and replies",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Notifications,
                    title = "Weekly prompt reminders",
                    subtitle = "A nudge if you haven't answered this week's prompt",
                    checked = uiState.weeklyPromptRemindersEnabled,
                    onCheckedChange = viewModel::setWeeklyPromptReminders
                )
            }

            // -------------------------
            // P2P Nearby
            // -------------------------

            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Bluetooth,
                    title = "P2P proximity alerts",
                    subtitle = "Alert me when I'm near a group member (BLE)",
                    checked = uiState.p2pAlertsEnabled,
                    onCheckedChange = { enabled ->

                        if (!enabled) {

                            viewModel.setP2pAlertsEnabled(false)

                        } else {

                            if (!PermissionManager.isBluetoothEnabled(context)) {

                                viewModel.setP2pAlertsEnabled(false)

                                Toast.makeText(
                                    context,
                                    "Please turn on Bluetooth to use P2P proximity alerts.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else if (
                                PermissionManager.hasNearbyPermissions(context)
                            ) {

                                viewModel.setP2pAlertsEnabled(true)
                                viewModel.startNearby()

                            } else {

                                val permissions =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_SCAN,
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_ADVERTISE,
                                            Manifest.permission.NEARBY_WIFI_DEVICES
                                        )
                                    } else {
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_SCAN,
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_ADVERTISE
                                        )
                                    }

                                nearbyPermissionLauncher.launch(permissions)
                            }
                        }
                    }
                )
            }

            // Nearby error message
            if (uiState.nearbyError != null) {
                item {
                    Text(
                        text = uiState.nearbyError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            // -------------------------
            // Privacy
            // -------------------------

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SectionLabel("Privacy")
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Attach location to memories",
                    subtitle = "Store where a memory happened along with the date",
                    checked = uiState.shareLocationWithMemories,
                    onCheckedChange = viewModel::setShareLocationWithMemories
                )
            }

            // -------------------------
            // Logout
            // -------------------------

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.signOut(onSignedOut)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            androidx.compose.material3.ButtonDefaults
                                .outlinedButtonColors(
                                    contentColor =
                                        MaterialTheme.colorScheme.error
                                )
                    ) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            "Log out",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileHeader(
    displayName: String,
    email: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text =
                    displayName
                        .firstOrNull()
                        ?.uppercaseChar()
                        ?.toString()
                        ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text =
                    email.ifBlank {
                        "No email on file"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}


@Composable
private fun SectionLabel(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 16.dp,
            bottom = 4.dp
        )
    )
}


@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
