package de.schatzsuche.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    var showPhotoCapture by remember { mutableStateOf(false) }
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

    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            pendingVideoFile?.let { onCapturedFile(it, ContentBlockType.VIDEO) }
        }
        pendingVideoFile = null
    }

    fun launchPhotoCapture() {
        withPermissions(Manifest.permission.CAMERA) {
            showPhotoCapture = true
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

    if (showPhotoCapture) {
        PhotoCaptureDialog(
            onDismiss = { showPhotoCapture = false },
            onPhotoCaptured = { file ->
                onCapturedFile(file, ContentBlockType.IMAGE)
                showPhotoCapture = false
            }
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
private fun PhotoCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val outputFile = remember { MediaStorage.createMediaFile(context, "instructions", "jpg") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onPhotoCaptured(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                }
            }
        )
    }

    fun bindCameraUseCases(provider: ProcessCameraProvider, previewView: PreviewView, lensFacing: Int) {
        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                capture
            )
            imageCapture = capture
        } catch (_: Exception) {
            imageCapture = null
        }
    }

    Dialog(
        onDismissRequest = { if (!isCapturing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(enabled = !isCapturing, onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        bindCameraUseCases(provider, previewView, cameraFacing)
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                update = { previewView ->
                    cameraProvider?.let { provider ->
                        bindCameraUseCases(provider, previewView, cameraFacing)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                enabled = !isCapturing,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }

            val provider = cameraProvider
            val hasFrontCamera = provider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
            if (hasFrontCamera) {
                IconButton(
                    onClick = {
                        if (!isCapturing) {
                            cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    },
                    enabled = !isCapturing,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Kamera wechseln",
                        tint = Color.White
                    )
                }
            }

            if (isCapturing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Button(
                    onClick = { capturePhoto() },
                    enabled = imageCapture != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text("Foto aufnehmen")
                }
            }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordingIndicator()
                        Text(
                            "Aufnahme läuft",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Tippe auf „Stoppen“, wenn du fertig bist.")
                } else {
                    Text("Tippe auf „Aufnahme starten“, um eine Sprachnachricht aufzunehmen.")
                }
            }
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

@Composable
private fun RecordingIndicator() {
    val transition = rememberInfiniteTransition(label = "recording-indicator")
    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording-pulse"
    )

    Box(
        modifier = Modifier
            .size((12 * pulse).dp)
            .background(Color(0xFFD32F2F), shape = RoundedCornerShape(50))
    )
}
