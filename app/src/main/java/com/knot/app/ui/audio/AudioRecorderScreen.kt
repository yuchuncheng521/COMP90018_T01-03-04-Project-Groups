package com.knot.app.ui.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun AudioRecorderScreen(
    onAudioSelected: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf<String?>(null) }
    var recordingFinished by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (recordingFinished && audioFilePath != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Audio recorded successfully")
                Button(
                    onClick = {
                        recordingFinished = false
                        audioFilePath = null
                    }
                ) {
                    Text("Retake Audio")
                }
                Button(
                    onClick = {
                        audioFilePath?.let { path ->
                            onAudioSelected(path)
                        }
                    }
                ) {
                    Text("Use Audio")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRecording) {
                        "Recording..."
                    } else {
                        "Audio Recorder"
                    }
                )

                Button(
                    onClick = {
                        if (!isRecording) {
                            val audioFile = File(
                                context.filesDir,
                                "Knot_${System.currentTimeMillis()}.m4a"
                            )
                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaRecorder()
                            }
                            recorder.apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(audioFile.absolutePath)
                                try {
                                    prepare()
                                    start()
                                    mediaRecorder = this
                                    isRecording = true
                                    audioFilePath = audioFile.absolutePath
                                } catch (e: Exception) {
                                    Log.e("AudioRecorder", "Failed to start recording", e)
                                    Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                Toast.makeText(context, "Audio saved!", Toast.LENGTH_SHORT).show()
                                recordingFinished = true
                            } catch (e: Exception) {
                                Log.e("AudioRecorder", "Failed to stop recording", e)
                            } finally {
                                mediaRecorder = null
                                isRecording = false
                            }
                        }
                    }
                ) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }
        }

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
        }
    }
}
