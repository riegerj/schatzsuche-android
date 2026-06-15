package de.schatzsuche.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.schatzsuche.app.data.model.ContentBlockType
import de.schatzsuche.app.util.MediaStorage
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstructionMediaActions(
    onPickUri: (android.net.Uri, ContentBlockType) -> Unit,
    onCapturedFile: (File, ContentBlockType) -> Unit
) {
    val context = LocalContext.current
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            pendingPermissionAction?.invoke()
        }
        pendingPermissionAction = null
    }

    fun withPermissions(vararg permissions: String, action: () -> Unit) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            action()
        } else {
            pendingPermissionAction = action
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickUri(uri, ContentBlockType.IMAGE)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickUri(uri, ContentBlockType.AUDIO)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickUri(uri, ContentBlockType.VIDEO)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingPhotoFile?.let { onCapturedFile(it, ContentBlockType.IMAGE) }
        }
        pendingPhotoFile = null
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            pendingVideoFile?.let { onCapturedFile(it, ContentBlockType.VIDEO) }
        }
        pendingVideoFile = null
    }

    fun launchPhotoCapture() {
        withPermissions(Manifest.permission.CAMERA) {
            val file = MediaStorage.createMediaFile(context, "instructions", "jpg")
            pendingPhotoFile = file
            takePictureLauncher.launch(MediaStorage.fileProviderUri(context, file))
        }
    }

    fun launchVideoCapture() {
        withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) {
            val file = MediaStorage.createMediaFile(context, "instructions", "mp4")
            pendingVideoFile = file
            captureVideoLauncher.launch(MediaStorage.fileProviderUri(context, file))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaActionRow(
            label = "Bild",
            onPick = { imagePicker.launch("image/*") },
            onCapture = { launchPhotoCapture() },
            captureLabel = "Foto aufnehmen"
        )
        MediaActionRow(
            label = "Audio",
            onPick = { audioPicker.launch("audio/*") },
            onCapture = {
                withPermissions(Manifest.permission.RECORD_AUDIO) {
                    showAudioDialog = true
                }
            },
            captureLabel = "Audio aufnehmen"
        )
        MediaActionRow(
            label = "Video",
            onPick = { videoPicker.launch("video/*") },
            onCapture = { launchVideoCapture() },
            captureLabel = "Video aufnehmen"
        )
    }

    if (showAudioDialog) {
        AudioRecordDialog(
            onDismiss = { showAudioDialog = false },
            onRecorded = { file ->
                onCapturedFile(file, ContentBlockType.AUDIO)
                showAudioDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaActionRow(
    label: String,
    onPick: () -> Unit,
    onCapture: () -> Unit,
    captureLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onPick) { Text("Datei wählen") }
            OutlinedButton(onClick = onCapture) { Text(captureLabel) }
        }
    }
}

@Composable
private fun AudioRecordDialog(
    onDismiss: () -> Unit,
    onRecorded: (File) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val outputFile = remember { MediaStorage.createMediaFile(context, "instructions", "m4a") }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching {
                if (isRecording) stop()
                release()
            }
            recorder = null
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        title = { Text("Audio aufnehmen") },
        text = {
            Text(
                if (isRecording) {
                    "Aufnahme läuft… Tippe auf „Stoppen“, wenn du fertig bist."
                } else {
                    "Tippe auf „Aufnahme starten“, um eine Sprachnachricht aufzunehmen."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isRecording) {
                        runCatching {
                            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaRecorder()
                            }
                            mediaRecorder.apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    setOutputFile(outputFile)
                                } else {
                                    @Suppress("DEPRECATION")
                                    setOutputFile(outputFile.absolutePath)
                                }
                                prepare()
                                start()
                            }
                            recorder = mediaRecorder
                            isRecording = true
                        }
                    } else {
                        runCatching {
                            recorder?.stop()
                            recorder?.release()
                        }
                        recorder = null
                        isRecording = false
                        onRecorded(outputFile)
                    }
                }
            ) {
                Text(if (isRecording) "Stoppen & speichern" else "Aufnahme starten")
            }
        },
        dismissButton = {
            if (!isRecording) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        }
    )
}
