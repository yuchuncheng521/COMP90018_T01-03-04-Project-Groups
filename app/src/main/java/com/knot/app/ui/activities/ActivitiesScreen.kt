package com.knot.app.ui.activities

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.R
import com.knot.app.location.LocationManager
import com.knot.app.model.ActivityItem
import com.knot.app.model.ActivityStatus
import com.knot.app.permissions.PermissionManager
import com.knot.app.ui.audio.AudioRecorderScreen
import com.knot.app.ui.camera.CameraScreen
import com.knot.app.ui.components.P2PAlertBanner
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: ActivitiesViewModel,
    onActivityClick: (ActivityItem) -> Unit = {},
    onCreateClick: () -> Unit = {},
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen states
    var showCamera by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }

    // Selected media
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedVideoPath by remember { mutableStateOf<String?>(null) }
    var selectedAudioPath by remember { mutableStateOf<String?>(null) }

    // Location
    val locationManager = remember { LocationManager(context) }


    var currentLocationText by remember {
        mutableStateOf<String?>(null)
    }

    //nearby
    val nearbyPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                            permissions[Manifest.permission.BLUETOOTH_ADVERTISE] == true
                } else {
                    true
                }

            if (granted) {
                viewModel.startNearby("KnotUser")

            }
        }



    // Reusable function for getting the current location
    fun updateCurrentLocation() {
        coroutineScope.launch {
            val location = locationManager.getCurrentLocation()

            currentLocationText =
                if (location != null) {
                    locationManager.getLocationName(location)
                        ?: "Location detected"
                } else {
                    "Location unavailable"
                }
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        }
    }

    // Microphone permission
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAudioRecorder = true
        }
    }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            updateCurrentLocation()
        }
    }

    // Camera screen
    if (showCamera) {
        CameraScreen(
            onPhotoSelected = { photoPath ->
                selectedPhotoPath = photoPath
                showCamera = false
                updateCurrentLocation()


            },
            onVideoSelected = { videoPath ->
                selectedVideoPath = videoPath
                showCamera = false
                updateCurrentLocation()
            },
            onCancel = {
                showCamera = false
            }
        )

        return
    }

    // Audio recorder screen
    if (showAudioRecorder) {
        AudioRecorderScreen(
            onAudioSelected = { audioPath ->
                selectedAudioPath = audioPath
                showAudioRecorder = false
                updateCurrentLocation()
            },
            onCancel = {
                showAudioRecorder = false
            }
        )

        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Activities", fontSize = 38.sp)
                }
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { /* Already on Quests/Activities */ },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KnotDarkBrown,
                            contentColor = KnotCream
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("ACTIVITIES", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCreateClick,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = KnotDarkBrown
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, KnotDarkBrown)
                    ) {
                        Text("CREATE", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // -------------------------
            // P2P alert
            // -------------------------

            val alert = uiState.p2pAlert
            val showP2pAlert = (alert != null && !uiState.p2pAlertDismissed)

            if (showP2pAlert) {
                item(
                    key = "p2p-alert"
                ) {
                    AnimatedVisibility(
                        visible = true
                    ) {
                        P2PAlertBanner(
                            alert = alert!!,
                            onRespond = {
                                onActivityClick(alert)
                            },
                            onDismiss = {
                                viewModel.dismissP2pAlert()
                            }
                        )
                    }
                }
            }

            // -------------------------
            // Activities
            // -------------------------

            if (uiState.activities.isEmpty()) {
                item {
                    Text(
                        text = "No activities right now. Check back for the next weekly prompt!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            top = 24.dp
                        )
                    )
                }
            } else {
                items(
                    uiState.activities,
                    key = { it.id }
                ) { activity ->

                    ActivityCard(
                        activity = activity,
                        onClick = {
                            onActivityClick(activity)
                        },
                        onToggleComplete = {
                            viewModel.markCompleted(
                                activity.id
                            )
                        }
                    )
                }
            }

            //nearby
            item {
                Button(
                    onClick = {
                        if (PermissionManager.hasNearbyPermissions(context)) {
                            viewModel.startNearby("KnotUser")
                        } else {
                            nearbyPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_ADVERTISE
                                )
                            )
                        }
                    }
                ) {
                    Text("Start Nearby")
                }
            }
        }
    }
}


@Composable
private fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
) {
    val isCompleted =
        activity.status == ActivityStatus.COMPLETED

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onToggleComplete
            ) {
                Icon(
                    imageVector =
                        if (isCompleted) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.RadioButtonUnchecked
                        },
                    contentDescription =
                        if (isCompleted) {
                            "Completed"
                        } else {
                            "Mark as complete"
                        },
                    tint =
                        if (isCompleted) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {

                Text(
                    text = activity.groupName,
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = activity.title,
                    style =
                        MaterialTheme.typography.titleMedium,
                    textDecoration =
                        if (isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    color =
                        if (isCompleted) {
                            MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.5f)
                        } else {
                            Color.Unspecified
                        }
                )

                if (activity.dueLabel.isNotBlank()) {
                    Text(
                        text = activity.dueLabel,
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.right_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
