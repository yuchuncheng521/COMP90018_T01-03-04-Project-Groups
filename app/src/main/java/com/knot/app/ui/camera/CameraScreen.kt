package com.knot.app.ui.camera

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import android.Manifest
import androidx.compose.foundation.layout.Arrangement

@Composable
fun CameraScreen(
    onPhotoSelected: (String) -> Unit,
    onVideoSelected: (String) -> Unit
) {
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var capturedVideoPath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Used to take a photo
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.HD)
            )
            .build()
    }

    val videoCapture = remember {
        VideoCapture.withOutput(recorder)
    }

    var recording by remember {
        mutableStateOf<Recording?>(null)
    }
    if (capturedPhotoPath != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = File(capturedPhotoPath!!)
                ),
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = {
                        capturedPhotoPath = null
                    }
                ) {
                    Text("Retake")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        capturedPhotoPath?.let { path ->
                            onPhotoSelected(path)
                        }
                    }
                ) {
                    Text("Use Photo")
                }
            }
        }

        return
    }
    if (capturedVideoPath != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Video recorded successfully")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    capturedVideoPath = null
                }
            ) {
                Text("Retake Video")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    capturedVideoPath?.let { path ->
                        onVideoSelected(path)
                    }
                }
            ) {
                Text("Use Video")
            }
        }

            return
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->

                    val previewView = PreviewView(ctx)

                    val cameraProviderFuture =
                        ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({

                        try {

                            val cameraProvider =
                                cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(
                                        previewView.surfaceProvider
                                    )
                                }

                            val cameraSelector =
                                CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                videoCapture
                            )

                        } catch (e: Exception) {

                            Log.e(
                                "CameraScreen",
                                "Camera preview failed",
                                e
                            )
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture
                        ) { photoPath ->
                            capturedPhotoPath = photoPath
                        }
                    }
                ) {
                    Text("Take Photo")
                }

                Button(
                    onClick = {
                        if (recording == null) {

                            val videoFile = File(
                                context.filesDir,
                                "Knot_${System.currentTimeMillis()}.mp4"
                            )

                            val outputOptions =
                                FileOutputOptions.Builder(videoFile).build()

                            val pendingRecording =
                                videoCapture.output
                                    .prepareRecording(
                                        context,
                                        outputOptions
                                    )

                            recording =
                                if (
                                    PermissionChecker.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PermissionChecker.PERMISSION_GRANTED
                                ) {

                                    pendingRecording
                                        .withAudioEnabled()
                                        .start(
                                            ContextCompat.getMainExecutor(context)
                                        ) { event ->

                                            if (
                                                event is VideoRecordEvent.Finalize
                                            ) {

                                                if (!event.hasError()) {

                                                    Toast.makeText(
                                                        context,
                                                        "Video saved!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    Log.d(
                                                        "CameraScreen",
                                                        "Video saved: ${videoFile.absolutePath}"
                                                    )
                                                    capturedVideoPath = videoFile.absolutePath

                                                } else {

                                                    Log.e(
                                                        "CameraScreen",
                                                        "Video recording failed: ${event.error}"
                                                    )
                                                }

                                                recording = null
                                            }
                                        }

                                } else {

                                    pendingRecording
                                        .start(
                                            ContextCompat.getMainExecutor(context)
                                        ) { event ->

                                            if (
                                                event is VideoRecordEvent.Finalize
                                            ) {

                                                if (!event.hasError()) {

                                                    Toast.makeText(
                                                        context,
                                                        "Video saved!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    Log.d(
                                                        "CameraScreen",
                                                        "Video saved: ${videoFile.absolutePath}"
                                                    )
                                                    capturedVideoPath = videoFile.absolutePath
                                                }

                                                recording = null
                                            }
                                        }
                                }

                        } else {

                            recording?.stop()
                        }
                    }
                ) {
                    Text(
                        if (recording == null) {
                            "Start Recording"
                        } else {
                            "Stop Recording"
                        }
                    )
                }
            }
        }
    }


    private fun takePhoto(
        context: Context,
        imageCapture: ImageCapture,
        onPhotoSaved: (String) -> Unit
    ) {

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())

        val photoFile = File(
            context.filesDir,
            "Knot_$timeStamp.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {

                    Toast.makeText(
                        context,
                        "Photo saved!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(
                        "CameraScreen",
                        "Photo saved: ${photoFile.absolutePath}"
                    )
                    onPhotoSaved(photoFile.absolutePath)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Toast.makeText(
                        context,
                        "Failed to save photo",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.e(
                        "CameraScreen",
                        "Photo capture failed",
                        exception
                    )
                }
            }
        )
    }

