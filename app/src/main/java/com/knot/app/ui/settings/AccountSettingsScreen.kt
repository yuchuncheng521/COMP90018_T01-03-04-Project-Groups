package com.knot.app.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Palette
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
import com.knot.app.R
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotRed
import com.knot.app.ui.theme.KnotGreen

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
    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", fontSize = 38.sp) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
        ) {
            ProfileHeader(
                displayName = uiState.account.displayName,
                email = uiState.account.email,
                onClick = onProfileClick,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            SectionLabel("CUSTOMISATION & NOTIFICATIONS")
            SettingsSwitchRow(
                icon = Icons.Filled.Notifications,
                title = "Push notifications",
                subtitle = "Get notified about new prompts and replies",
                checked = uiState.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled,
            )
            SettingsSwitchRow(
                icon = Icons.Filled.Notifications,
                title = "Weekly prompt reminders",
                subtitle = "A nudge if you haven't answered this week's prompt",
                checked = uiState.weeklyPromptRemindersEnabled,
                onCheckedChange = viewModel::setWeeklyPromptReminders
            )
            SettingsSwitchRow(
                icon = Icons.Filled.Bluetooth,
                title = "P2P proximity alerts",
                subtitle = "Alert me when I'm near a group member (BLE)",
                checked = uiState.p2pAlertsEnabled,
                onCheckedChange = viewModel::setP2pAlertsEnabled
            )
            SettingsClickRow(
                icon = Icons.Filled.Brush,
                title = "App Preference",
                subtitle = "Theme and Text size",
                onClick = onAppPreferencesClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionLabel("PRIVACY")
            SettingsInfoRow(
                icon = Icons.Filled.Lock,
                title = "End-to-end encryption",
                subtitle = "Only your circle can read this content",
                value = "Enabled",
                valueColor = if (true) KnotGreen else KnotRed
            )
            SettingsSwitchRow(
                icon = Icons.Filled.LocationOn,
                title = "Attach location to memories",
                subtitle = "Store where a memory happened along with the date",
                checked = uiState.shareLocationWithMemories,
                onCheckedChange = viewModel::setShareLocationWithMemories
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionLabel("SENSORS & PERMISSIONS")
            SettingsStatusRow(
                icon = Icons.Filled.Bluetooth,
                title = "Bluetooth",
                subtitle = "Used for P2P proximity alerts",
                isAllowed = uiState.isBluetoothAllowed
            )
            SettingsStatusRow(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                subtitle = "Used for attaching locations to memories",
                isAllowed = uiState.isLocationAllowed
            )
            SettingsStatusRow(
                icon = Icons.Filled.CameraAlt,
                title = "Camera",
                subtitle = "Used for capturing photos and videos",
                isAllowed = uiState.isCameraAllowed
            )
            SettingsStatusRow(
                icon = Icons.Filled.Mic,
                title = "Microphone",
                subtitle = "Used for recording audio notes",
                isAllowed = uiState.isMicrophoneAllowed
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionLabel("CONNECTIVITY & STORAGE")
            SettingsInfoRow(
                icon = Icons.Filled.Sync,
                title = "Sync Status",
                value = uiState.syncStatus,
                valueColor = if (uiState.syncStatus == "Up to date") KnotGreen else KnotRed
            )
            SettingsProgressRow(
                icon = Icons.Filled.CloudQueue,
                title = "Cloud Storage Usage",
                progress = uiState.storageUsage,
                subtitle = "${(uiState.storageUsage * 100).toInt()}% of 5GB used"
            )
            SettingsSwitchRow(
                icon = Icons.Filled.Wifi,
                title = "Sync over Wi-Fi",
                subtitle = "Only sync large files on Wi-Fi to save data",
                checked = uiState.syncOverWifi,
                onCheckedChange = viewModel::setSyncOverWifi
            )
            SettingsClickRow(
                icon = Icons.Filled.DeleteSweep,
                title = "Clear local cache",
                subtitle = "Free up space on your device",
                onClick = { viewModel.setShowClearCacheDialog(show = true) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.setShowLogoutDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
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

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowClearCacheDialog(false) },
            title = { Text("Clear Local Cache") },
            text = { Text("This will delete temporary files stored on your device. Your account data in the cloud will not be affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearLocalCache(context) }) {
                    Text("Clear", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowClearCacheDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowLogoutDialog(false) },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setShowLogoutDialog(false)
                        viewModel.signOut(onSignedOut)
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowLogoutDialog(false) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
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
