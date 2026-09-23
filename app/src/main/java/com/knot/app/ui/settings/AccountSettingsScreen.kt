package com.knot.app.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.knot.app.R
import com.knot.app.permissions.PermissionManager
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotGreen
import com.knot.app.ui.theme.KnotRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onSignedOut: () -> Unit,
    onProfileClick: () -> Unit,
    onAppPreferencesClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // TODO: confirm with whoever wrote checkPermissions() whether this overlaps with
    // the defensive checks below — kept both since checkPermissions() likely feeds the
    // read-only SENSORS & PERMISSIONS status rows further down, a different job from
    // the toggle-disabling logic in the next effect. Unverified — I haven't seen
    // AccountSettingsViewModel.kt.
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(Unit) {
        if (!PermissionManager.isBluetoothEnabled(context)) {
            viewModel.setP2pAlertsEnabled(false)
        }

        val notificationsAllowed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        if (!notificationsAllowed && uiState.notificationsEnabled) {
            viewModel.setNotificationsEnabled(false)
        }

        if (!PermissionManager.hasLocationPermission(context) &&
            uiState.shareLocationWithMemories
        ) {
            viewModel.setShareLocationWithMemories(false)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
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
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
        if (!granted) {
            Toast.makeText(
                context,
                "Notification permission is required to receive push notifications.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setShareLocationWithMemories(granted)
        if (!granted) {
            Toast.makeText(
                context,
                "Location permission is required to attach location to memories.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
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
        topBar = { TopAppBar(title = { Text("Settings", fontSize = 38.sp) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // NOTE: no .verticalScroll() here on purpose — LazyColumn already scrolls
            // internally. Adding one on top of the other crashes at runtime.
        ) {
            item {
                ProfileHeader(
                    displayName = uiState.account.displayName,
                    email = uiState.account.email,
                    onClick = onProfileClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            }

            item { SectionLabel("CUSTOMISATION & NOTIFICATIONS") }
            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Notifications,
                    title = "Push notifications",
                    subtitle = "Get notified about new prompts and replies",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setNotificationsEnabled(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val alreadyGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (alreadyGranted) {
                                viewModel.setNotificationsEnabled(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setNotificationsEnabled(true)
                        }
                    }
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
            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Bluetooth,
                    title = "P2P proximity alerts",
                    subtitle = "Alert me when I'm near a group member (BLE)",
                    checked = uiState.p2pAlertsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setP2pAlertsEnabled(false)
                        } else if (!PermissionManager.isBluetoothEnabled(context)) {
                            viewModel.setP2pAlertsEnabled(false)
                            Toast.makeText(
                                context,
                                "Please turn on Bluetooth to use P2P proximity alerts.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (PermissionManager.hasNearbyPermissions(context)) {
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
                )
            }
            if (uiState.nearbyError != null) {
                item {
                    Text(
                        text = uiState.nearbyError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            item {
                SettingsClickRow(
                    icon = Icons.Filled.Brush,
                    title = "App Preference",
                    subtitle = "Theme and Text size",
                    onClick = onAppPreferencesClick
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
            item { SectionLabel("PRIVACY") }
            item {
                // TODO: valueColor is hardcoded to always KnotGreen (if (true) ...) —
                // this was already like this before the merge, not something either
                // branch introduced, but worth fixing so "Enabled" reflects real state.
                SettingsInfoRow(
                    icon = Icons.Filled.Lock,
                    title = "End-to-end encryption",
                    subtitle = "Only your circle can read this content",
                    value = "Enabled",
                    valueColor = KnotGreen
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Attach location to memories",
                    subtitle = "Store where a memory happened along with the date",
                    checked = uiState.shareLocationWithMemories,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setShareLocationWithMemories(false)
                        } else if (PermissionManager.hasLocationPermission(context)) {
                            viewModel.setShareLocationWithMemories(true)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
            item { SectionLabel("SENSORS & PERMISSIONS") }
            item {
                SettingsStatusRow(
                    icon = Icons.Filled.Bluetooth,
                    title = "Bluetooth",
                    subtitle = "Used for P2P proximity alerts",
                    isAllowed = uiState.isBluetoothAllowed
                )
            }
            item {
                SettingsStatusRow(
                    icon = Icons.Filled.LocationOn,
                    title = "Location",
                    subtitle = "Used for attaching locations to memories",
                    isAllowed = uiState.isLocationAllowed
                )
            }
            item {
                SettingsStatusRow(
                    icon = Icons.Filled.CameraAlt,
                    title = "Camera",
                    subtitle = "Used for capturing photos and videos",
                    isAllowed = uiState.isCameraAllowed
                )
            }
            item {
                SettingsStatusRow(
                    icon = Icons.Filled.Mic,
                    title = "Microphone",
                    subtitle = "Used for recording audio notes",
                    isAllowed = uiState.isMicrophoneAllowed
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) }
            item { SectionLabel("CONNECTIVITY & STORAGE") }
            item {
                SettingsInfoRow(
                    icon = Icons.Filled.Sync,
                    title = "Sync Status",
                    value = uiState.syncStatus,
                    valueColor = if (uiState.syncStatus == "Up to date") KnotGreen else KnotRed
                )
            }
            item {
                SettingsProgressRow(
                    icon = Icons.Filled.CloudQueue,
                    title = "Cloud Storage Usage",
                    progress = uiState.storageUsage,
                    subtitle = "${(uiState.storageUsage * 100).toInt()}% of 5GB used"
                )
            }
            item {
                SettingsSwitchRow(
                    icon = Icons.Filled.Wifi,
                    title = "Sync over Wi-Fi",
                    subtitle = "Only sync large files on Wi-Fi to save data",
                    checked = uiState.syncOverWifi,
                    onCheckedChange = viewModel::setSyncOverWifi
                )
            }
            item {
                SettingsClickRow(
                    icon = Icons.Filled.DeleteSweep,
                    title = "Clear local cache",
                    subtitle = "Free up space on your device",
                    onClick = { viewModel.setShowClearCacheDialog(show = true) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.signOut(onSignedOut) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KnotDarkBrown,
                            contentColor = KnotCream
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Log out", modifier = Modifier.padding(start = 8.dp))
                    }

                    Button(
                        onClick = onDeleteAccountClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KnotRed,
                            contentColor = KnotCream
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Delete Account")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearCacheDialog(false) },
            title = { Text("Clear Local Cache") },
            text = { Text("This will delete temporary files stored on your device. Your account data in the cloud will not be affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearLocalCache(context) }) {
                    Text("Clear", color = KnotDarkBrown)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearCacheDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun SettingsStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isAllowed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(
            text = if (isAllowed) "Allowed" else "Not allowed",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAllowed) KnotGreen else KnotRed
        )
    }
}

@Composable
private fun SettingsProgressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    progress: Float,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = KnotDarkBrown,
                trackColor = KnotDarkBrown.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Icon(
            painter = painterResource(id = R.drawable.right_arrow),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ProfileHeader(displayName: String, email: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            // Duplicate Text(displayName, ...) removed here — it appeared twice in a
            // row in the pre-merge file, unrelated to either branch's actual changes.
            Text(text = displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = email.ifBlank { "No email on file" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.right_arrow),
            contentDescription = "Edit Profile",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}