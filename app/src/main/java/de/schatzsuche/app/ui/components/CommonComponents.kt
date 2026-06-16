package de.schatzsuche.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import de.schatzsuche.app.data.model.ContentBlockType
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.data.model.PostScanTask
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.RichContentBlock
import de.schatzsuche.app.data.model.TaskResponse
import de.schatzsuche.app.ui.theme.SchatzButtonDefaults
import de.schatzsuche.app.ui.theme.toPalette
import de.schatzsuche.app.util.ImageUtil
import de.schatzsuche.app.util.MediaStorage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

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
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SchatzTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = SchatzButtonDefaults.textButtonColors(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun SchatzConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    secondaryDestructive: Boolean = false
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                        colors = SchatzButtonDefaults.filledButtonColors()
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = onSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                        colors = if (secondaryDestructive) {
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            SchatzButtonDefaults.outlinedButtonColors()
                        },
                        border = if (secondaryDestructive) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.outlinedButtonBorder(enabled = true)
                        }
                    ) {
                        Text(secondaryLabel, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
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
                val startPoint = Offset(centerX, stepHeight * 0.5f)

                fun locationForStep(stepIndex: Int): Offset {
                    val xOffset = if (stepIndex % 2 == 0) -60f else 60f
                    return Offset(centerX + xOffset, stepHeight * (stepIndex + 1))
                }

                val treasurePoint = locationForStep(totalSteps - 1)

                if (completedSteps > 0) {
                    val path = Path()
                    path.moveTo(startPoint.x, startPoint.y)
                    for (i in 0 until completedSteps.coerceAtMost(totalSteps)) {
                        val point = locationForStep(i)
                        path.lineTo(point.x, point.y)
                    }
                    drawPath(
                        path = path,
                        color = palette.mapPath,
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(12f, 8f),
                                if (animateLatest) dashPhase else 0f
                            )
                        )
                    )
                }

                drawCircle(
                    color = palette.accent,
                    radius = 14f,
                    center = startPoint
                )

                if (totalSteps > 1) {
                    for (index in 0 until totalSteps - 1) {
                        val point = locationForStep(index)
                        val isCompleted = index < completedSteps
                        val isCurrent = index == completedSteps
                        drawCircle(
                            color = when {
                                isCompleted -> palette.mapPath
                                isCurrent -> palette.accent
                                else -> palette.mapDot.copy(alpha = 0.5f)
                            },
                            radius = 10f,
                            center = point
                        )
                    }
                }

                drawCircle(color = Color(0xFFFFD700), radius = 16f, center = treasurePoint)
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
fun ContentBlocksDisplay(
    blocks: List<RichContentBlock>,
    modifier: Modifier = Modifier,
    showMedia: Boolean = true
) {
    ContentBlocksDisplay(
        blocks = blocks,
        modifier = modifier,
        immersiveMedia = false,
        showMedia = showMedia
    )
}

@Composable
fun ContentBlocksDisplay(
    blocks: List<RichContentBlock>,
    modifier: Modifier = Modifier,
    immersiveMedia: Boolean = false,
    showMedia: Boolean = true
) {
    val imagePaths = blocks.mapNotNull { block ->
        if (block.type == ContentBlockType.IMAGE) block.mediaPath else null
    }
    var fullScreenVideoPath by remember { mutableStateOf<String?>(null) }
    var fullScreenImageIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showMedia && immersiveMedia && imagePaths.isNotEmpty()) {
            InstructionImageGalleryCard(
                imagePath = imagePaths.first(),
                imageCount = imagePaths.size,
                onOpen = { fullScreenImageIndex = 0 }
            )
        }
        blocks.forEach { block ->
            when (block.type) {
                ContentBlockType.TEXT -> {
                    if (!block.text.isNullOrBlank()) {
                        Text(block.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                ContentBlockType.IMAGE -> {
                    if (!showMedia) return@forEach
                    if (!immersiveMedia) {
                        block.mediaPath?.let { path ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Hinweis ansehen", fontWeight = FontWeight.SemiBold)
                                    }
                                    InstructionImageDisplay(path)
                                }
                            }
                        }
                    }
                }
                ContentBlockType.AUDIO -> {
                    if (!showMedia) return@forEach
                    block.mediaPath?.let { path ->
                        InstructionAudioPlayer(path = path, immersiveMedia = immersiveMedia)
                    }
                }
                ContentBlockType.VIDEO -> {
                    if (!showMedia) return@forEach
                    block.mediaPath?.let { path ->
                        InstructionVideoPlayer(
                            path = path,
                            immersiveMedia = immersiveMedia,
                            onOpenFullScreen = { fullScreenVideoPath = path }
                        )
                    }
                }
            }
        }
    }

    fullScreenVideoPath?.let { path ->
        FullScreenVideoOverlay(path = path, onDismiss = { fullScreenVideoPath = null })
    }
    fullScreenImageIndex?.let { startIndex ->
        FullScreenImageGalleryOverlay(
            imagePaths = imagePaths,
            startIndex = startIndex,
            onDismiss = { fullScreenImageIndex = null }
        )
    }
}

@Composable
fun InstructionImageDisplay(
    path: String,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false
) {
    val file = remember(path) { File(path) }
    if (!file.exists()) return

    val bitmap = remember(path) { ImageUtil.decodeOrientedBitmap(path) }
    val imageModifier = if (fullscreen) {
        modifier
    } else {
        modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(12.dp))
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Fit
        )
        return
    }

    AndroidView(
        factory = { context ->
            android.widget.ImageView(context).apply {
                adjustViewBounds = true
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setImageBitmap(ImageUtil.decodeOrientedBitmap(path))
            }
        },
        modifier = imageModifier
    )
}

@Composable
private fun InstructionAudioPlayer(path: String, immersiveMedia: Boolean) {
    var isPlaying by remember(path) { mutableStateOf(false) }
    val mediaPlayer = remember(path) { MediaPlayer() }

    DisposableEffect(path) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Text("Hinweis anhören", modifier = Modifier.weight(1f))
            if (immersiveMedia) {
                AudioPlayingIndicator(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        try {
                            if (!mediaPlayer.isPlaying) {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(path)
                                mediaPlayer.prepare()
                                mediaPlayer.setOnCompletionListener { isPlaying = false }
                                mediaPlayer.start()
                            }
                            isPlaying = true
                        } catch (_: Exception) {
                            isPlaying = false
                        }
                    }
                }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Abspielen"
                )
            }
        }
    }
}

@Composable
private fun AudioPlayingIndicator(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "audio-indicator")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio-phase"
    )
    val activeColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val progress = if (isPlaying) phase else 0.15f

    Canvas(modifier = modifier.size(width = 18.dp, height = 14.dp)) {
        val spacing = size.width / 4f
        val minHeight = size.height * 0.25f
        val maxHeight = size.height * 0.95f
        val h1 = minHeight + (maxHeight - minHeight) * (0.45f + 0.55f * progress)
        val h2 = minHeight + (maxHeight - minHeight) * (0.65f + 0.35f * (1f - progress))
        val h3 = minHeight + (maxHeight - minHeight) * (0.35f + 0.65f * progress)

        val heights = listOf(h1, h2, h3)
        heights.forEachIndexed { index, barHeight ->
            val x = spacing * (index + 1)
            drawLine(
                color = if (isPlaying) activeColor else inactiveColor,
                start = Offset(x, size.height),
                end = Offset(x, size.height - barHeight),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InstructionVideoPlayer(
    path: String,
    immersiveMedia: Boolean,
    onOpenFullScreen: () -> Unit
) {
    if (immersiveMedia) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFullScreen() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text("Hinweis ansehen", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text("Hinweis ansehen", fontWeight = FontWeight.SemiBold)
            }
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        controller.setMediaPlayer(this)
                        setMediaController(controller)
                        setVideoURI(Uri.fromFile(File(path)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun InstructionImageGalleryCard(
    imagePath: String,
    imageCount: Int,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text("Hinweis ansehen", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (imageCount > 1) {
                    Text(
                        "$imageCount Bilder · Wischen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            InstructionImageDisplay(path = imagePath)
        }
    }
}

@Composable
private fun FullScreenVideoOverlay(path: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        controller.setMediaPlayer(this)
                        setMediaController(controller)
                        setVideoURI(Uri.fromFile(File(path)))
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }
                        setOnCompletionListener { onDismiss() }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }
        }
    }
}

@Composable
private fun FullScreenImageGalleryOverlay(
    imagePaths: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    if (imagePaths.isEmpty()) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        val pagerState = rememberPagerState(
            initialPage = startIndex.coerceIn(0, imagePaths.lastIndex),
            pageCount = { imagePaths.size }
        )
        val multipleImages = imagePaths.size > 1
        var showSwipeHint by remember { mutableStateOf(multipleImages) }
        val hintTransition = rememberInfiniteTransition(label = "gallery-swipe-hint")
        val chevronPulse by hintTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chevron-pulse"
        )

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage > 0) {
                showSwipeHint = false
            }
        }
        LaunchedEffect(multipleImages) {
            if (multipleImages) {
                delay(5000)
                showSwipeHint = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InstructionImageDisplay(
                        path = imagePaths[page],
                        modifier = Modifier.fillMaxSize(),
                        fullscreen = true
                    )
                }
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            if (multipleImages) {
                if (pagerState.currentPage > 0) {
                    GallerySwipeChevron(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .offset(x = (-chevronPulse).dp)
                    )
                }
                if (pagerState.currentPage < imagePaths.lastIndex) {
                    GallerySwipeChevron(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .offset(x = chevronPulse.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showSwipeHint,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                ) {
                    Text(
                        text = "Wische für weitere Bilder",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                ImageGalleryPageIndicator(
                    pageCount = imagePaths.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }
        }
    }
}

@Composable
private fun GallerySwipeChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ImageGalleryPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 8.dp)
                    .alpha(if (selected) 1f else 0.45f)
                    .background(Color.White, CircleShape)
            )
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
                                    val blockType = when (task.type) {
                                        PostScanTaskType.PHOTO -> ContentBlockType.IMAGE
                                        PostScanTaskType.VIDEO -> ContentBlockType.VIDEO
                                        PostScanTaskType.AUDIO -> ContentBlockType.AUDIO
                                        else -> return@rememberLauncherForActivityResult
                                    }
                                    val path = MediaStorage.copyToAppStorage(context, uri, "responses", blockType)
                                        ?: return@rememberLauncherForActivityResult
                                    onResponse(task, TaskResponse(task.id, task.type, mediaPath = path))
                                }
                            }
                            val mediaPath = responses[task.id]?.mediaPath
                            if (mediaPath != null && task.type == PostScanTaskType.PHOTO) {
                                InstructionImageDisplay(
                                    path = mediaPath,
                                    modifier = Modifier.height(160.dp)
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
    modifier: Modifier = Modifier,
    scanActive: Boolean = false,
    fullScreen: Boolean = false
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
                Text("Kameraberechtigung benötigt", color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berechtigung erteilen")
                }
            }
        }
        return
    }

    val scanActiveRef = remember { AtomicBoolean(false) }
    LaunchedEffect(scanActive) {
        scanActiveRef.set(scanActive)
    }

    val cameraModifier = if (fullScreen) {
        modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp))
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
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
                    if (mediaImage != null && scanActiveRef.get()) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { value ->
                                    if (scanActiveRef.getAndSet(false)) {
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onQrScanned(value)
                                        }
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
        modifier = cameraModifier
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
