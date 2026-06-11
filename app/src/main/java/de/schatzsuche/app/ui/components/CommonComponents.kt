package de.schatzsuche.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import de.schatzsuche.app.data.model.ContentBlockType
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.data.model.PostScanTask
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.RichContentBlock
import de.schatzsuche.app.data.model.TaskResponse
import de.schatzsuche.app.ui.theme.toPalette
import de.schatzsuche.app.util.MediaStorage
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchatzAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun TreasureMap(
    theme: HuntTheme,
    totalSteps: Int,
    completedSteps: Int,
    modifier: Modifier = Modifier,
    animateLatest: Boolean = true
) {
    val palette = theme.toPalette()
    val infiniteTransition = rememberInfiniteTransition(label = "map")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "dash"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "🗺️ Schatzkarte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((totalSteps.coerceAtLeast(1) * 80 + 40).dp)
            ) {
                if (totalSteps <= 0) return@Canvas
                val stepHeight = size.height / (totalSteps + 1)
                val centerX = size.width / 2f
                val points = (0 until totalSteps).map { i ->
                    val xOffset = if (i % 2 == 0) -60f else 60f
                    Offset(centerX + xOffset, stepHeight * (i + 1))
                }

                if (completedSteps > 0) {
                    val path = Path()
                    path.moveTo(centerX, stepHeight * 0.5f)
                    for (i in 0 until completedSteps.coerceAtMost(points.size)) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                    drawPath(
                        path = path,
                        color = palette.mapPath,
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), if (animateLatest) dashPhase else 0f)
                        )
                    )
                }

                points.forEachIndexed { index, point ->
                    val isCompleted = index < completedSteps
                    val isCurrent = index == completedSteps
                    drawCircle(
                        color = when {
                            isCompleted -> palette.mapPath
                            isCurrent -> palette.accent
                            else -> palette.mapDot.copy(alpha = 0.5f)
                        },
                        radius = if (isCurrent) 14f else 10f,
                        center = point
                    )
                }

                drawCircle(color = Color(0xFFFFD700), radius = 16f, center = Offset(centerX, size.height - 20f))
            }
            Text(
                "Fortschritt: $completedSteps / $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackground
            )
        }
    }
}

@Composable
fun ContentBlocksDisplay(blocks: List<RichContentBlock>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block.type) {
                ContentBlockType.TEXT -> {
                    if (!block.text.isNullOrBlank()) {
                        Text(block.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                ContentBlockType.IMAGE -> {
                    block.mediaPath?.let { path ->
                        Image(
                            painter = rememberAsyncImagePainter(File(path)),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                ContentBlockType.AUDIO -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Audio-Anweisung verfügbar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ContentBlockType.VIDEO -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Video-Anweisung verfügbar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PostScanTasksForm(
    tasks: List<PostScanTask>,
    responses: Map<String, TaskResponse>,
    onResponse: (PostScanTask, TaskResponse) -> Unit,
    error: String? = null
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        tasks.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(task.question, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    when (task.type) {
                        PostScanTaskType.TEXT_INPUT -> {
                            val current = responses[task.id]?.textAnswer.orEmpty()
                            OutlinedTextField(
                                value = current,
                                onValueChange = {
                                    onResponse(task, TaskResponse(task.id, task.type, textAnswer = it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Antwort") }
                            )
                        }
                        PostScanTaskType.SINGLE_CHOICE -> {
                            task.options.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onResponse(task, TaskResponse(task.id, task.type, selectedOptions = listOf(option)))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = responses[task.id]?.selectedOptions?.contains(option) == true,
                                        onClick = {
                                            onResponse(task, TaskResponse(task.id, task.type, selectedOptions = listOf(option)))
                                        }
                                    )
                                    Text(option)
                                }
                            }
                        }
                        PostScanTaskType.MULTIPLE_CHOICE -> {
                            val selected = responses[task.id]?.selectedOptions?.toSet() ?: emptySet()
                            task.options.forEach { option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selected.contains(option),
                                        onCheckedChange = { checked ->
                                            val updated = if (checked) selected + option else selected - option
                                            onResponse(task, TaskResponse(task.id, task.type, selectedOptions = updated.toList()))
                                        }
                                    )
                                    Text(option)
                                }
                            }
                        }
                        PostScanTaskType.PHOTO,
                        PostScanTaskType.VIDEO,
                        PostScanTaskType.AUDIO -> {
                            val launcher = rememberLauncherForActivityResult(
                                ActivityResultContracts.GetContent()
                            ) { uri ->
                                if (uri != null) {
                                    val ext = when (task.type) {
                                        PostScanTaskType.PHOTO -> "jpg"
                                        PostScanTaskType.VIDEO -> "mp4"
                                        PostScanTaskType.AUDIO -> "m4a"
                                        else -> "dat"
                                    }
                                    val path = MediaStorage.copyToAppStorage(context, uri, "responses", ext)
                                    onResponse(task, TaskResponse(task.id, task.type, mediaPath = path))
                                }
                            }
                            val mediaPath = responses[task.id]?.mediaPath
                            if (mediaPath != null && task.type == PostScanTaskType.PHOTO) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(mediaPath)),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(8.dp))
                            } else if (mediaPath != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Aufnahme gespeichert")
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            val mime = when (task.type) {
                                PostScanTaskType.PHOTO -> "image/*"
                                PostScanTaskType.VIDEO -> "video/*"
                                PostScanTaskType.AUDIO -> "audio/*"
                                else -> "*/*"
                            }
                            OutlinedButton(onClick = { launcher.launch(mime) }) {
                                Text(
                                    when (task.type) {
                                        PostScanTaskType.PHOTO -> "Foto aufnehmen/auswählen"
                                        PostScanTaskType.VIDEO -> "Video aufnehmen/auswählen"
                                        PostScanTaskType.AUDIO -> "Audio aufnehmen/auswählen"
                                        else -> "Datei wählen"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kameraberechtigung benötigt")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berechtigung erteilen")
                }
            }
        }
        return
    }

    val scanned = remember { mutableStateOf(false) }
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val scanner = BarcodeScanning.getClient()
                analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !scanned.value) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { value ->
                                    if (!scanned.value) {
                                        scanned.value = true
                                        onQrScanned(value)
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp))
    )
}

@Composable
fun ThemeChip(theme: HuntTheme, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${theme.emoji} ${theme.displayName}") }
    )
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
