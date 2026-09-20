package com.knot.app.ui.activities

import android.Manifest
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.knot.app.R
import com.knot.app.location.LocationManager
import com.knot.app.permissions.PermissionManager
import com.knot.app.ui.audio.AudioRecorderScreen
import com.knot.app.ui.camera.CameraScreen
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotInk
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    viewModel: ActivitiesViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState

    // Retrieve activity data from ViewModel
    val activity = uiState.activities.find { it.id == activityId }
    val activityTitle = activity?.title ?: "Activity Name"
    val activityDescription = activity?.description ?: "No description provided."

    // Screen states
    var showCamera by remember { mutableStateOf(value = false) }
    var showAudioRecorder by remember { mutableStateOf(value = false) }

    // Selected media
    var textResponse by remember { mutableStateOf("") }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedVideoPath by remember { mutableStateOf<String?>(null) }
    var selectedAudioPath by remember { mutableStateOf<String?>(null) }

    // Location
    val locationManager = remember { LocationManager(context) }
    var currentLocationText by remember { mutableStateOf<String?>(null) }

    // Reusable function for getting the current location
    fun updateCurrentLocation() {
        coroutineScope.launch {
            val location = locationManager.getCurrentLocation()
            currentLocationText = if (location != null) {
                "Lat: ${location.latitude}, Lng: ${location.longitude}"
            } else {
                "Location unavailable"
            }
        }
    }

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) showCamera = true
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) showAudioRecorder = true
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) updateCurrentLocation()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = activityTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                    },
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Activity details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = activityDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                OutlinedTextField(
                    value = textResponse,
                    onValueChange = { textResponse = it },
                    placeholder = { Text("Enter Text Response") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = KnotCream,
                        focusedContainerColor = KnotCream,
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = KnotDarkBrown
                    ),
                    minLines = 1,
                    maxLines = 5
                )

                // Open Camera Button
                ResponseButton(
                    text = if ((selectedPhotoPath != null || selectedVideoPath != null)) "Retake Media" else "Open Camera",
                    onClick = {
                        if (PermissionManager.hasCameraPermission(context)) {
                            showCamera = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )

                // Record Audio Button
                ResponseButton(
                    text = if (selectedAudioPath != null) "Re-record Audio" else "Record Audio",
                    onClick = {
                        if (PermissionManager.hasMicrophonePermission(context)) {
                            showAudioRecorder = true
                        } else {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                // Location Button
                ResponseButton(
                    text = if (currentLocationText != null) "Update Location" else "Get Current Location",
                    onClick = {
                        if (PermissionManager.hasLocationPermission(context)) {
                            updateCurrentLocation()
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

                // Previews
                if (selectedPhotoPath != null) {
                    Text("Selected photo", style = MaterialTheme.typography.labelLarge)
                    Image(
                        painter = rememberAsyncImagePainter(model = File(selectedPhotoPath!!)),
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                if (selectedVideoPath != null) {
                    Text("Selected video", style = MaterialTheme.typography.labelLarge)
                    VideoPreview(videoPath = selectedVideoPath!!)
                }

                if (selectedAudioPath != null) {
                    Text("Selected audio", style = MaterialTheme.typography.labelLarge)
                    AudioPreview(audioPath = selectedAudioPath!!)
                }

                if (currentLocationText != null) {
                    Text("Location attached: $currentLocationText", style = MaterialTheme.typography.bodyMedium, color = KnotDarkBrown)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, KnotInk)
                    ) {
                        Text("CANCEL", color = KnotInk)
                    }
                    Button(
                        onClick = {
                            viewModel.submitActivityResponse(
                                activityId = activityId,
                                text = textResponse,
                                photoPath = selectedPhotoPath,
                                videoPath = selectedVideoPath,
                                audioPath = selectedAudioPath,
                                location = currentLocationText,
                                onSuccess = onBackClick
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KnotDarkBrown)
                    ) {
                        Text("DONE", color = Color.White)
                    }
                }
            }
        }

        // Camera screen overlay
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
                onCancel = { showCamera = false },
            )
        }

        // Audio recorder overlay
        if (showAudioRecorder) {
            AudioRecorderScreen(
                onAudioSelected = { audioPath ->
                    selectedAudioPath = audioPath
                    showAudioRecorder = false
                    updateCurrentLocation()
                },
                onCancel = { showAudioRecorder = false },
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = KnotDarkBrown)
            }
        }
    }
}

@Composable
private fun ResponseButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, color = Color.DarkGray)
    }
}

@Composable
private fun VideoPreview(videoPath: String) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setVideoURI(Uri.fromFile(File(videoPath)))
                setOnPreparedListener { it.isLooping = false }
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
private fun AudioPreview(audioPath: String) {
    val mediaPlayer = remember(audioPath) {
        MediaPlayer().apply {
            setDataSource(audioPath)
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(value = false) }

    Button(
        onClick = {
            if (isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
            isPlaying = !isPlaying
        }
    ) {
        Text(if (isPlaying) "Pause Audio" else "Play Audio")
    }

    DisposableEffect(mediaPlayer) {
        onDispose { mediaPlayer.release() }
    }
}
