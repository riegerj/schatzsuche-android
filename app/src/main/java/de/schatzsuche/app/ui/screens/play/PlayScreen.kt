package de.schatzsuche.app.ui.screens.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schatzsuche.app.data.model.toParticipantContentBlocks
import de.schatzsuche.app.data.model.toPostScanTasks
import de.schatzsuche.app.ui.components.ContentBlocksDisplay
import de.schatzsuche.app.ui.components.PostScanTasksForm
import de.schatzsuche.app.ui.components.QrScannerView
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.components.SchatzConfirmDialog
import de.schatzsuche.app.ui.components.SchatzTextButton
import de.schatzsuche.app.ui.components.TreasureMap
import de.schatzsuche.app.ui.theme.SchatzButtonDefaults
import de.schatzsuche.app.ui.theme.SchatzsucheTheme
import de.schatzsuche.app.ui.viewmodel.PlayPhase
import de.schatzsuche.app.ui.viewmodel.PlayViewModel

@Composable
fun PlayScreen(
    viewModel: PlayViewModel,
    onFinished: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val session by viewModel.session.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.currentStep?.id, uiState.phase) {
        scrollState.animateScrollTo(0)
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == PlayPhase.COMPLETED) {
            session?.id?.let { onFinished(it) }
        }
    }

    BackHandler {
        when {
            showCancelDialog -> showCancelDialog = false
            uiState.phase == PlayPhase.SCAN -> viewModel.dismissScanner()
            else -> showCancelDialog = true
        }
    }

    SchatzsucheTheme(huntTheme = uiState.huntTheme) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    SchatzAppBar(
                        title = session?.participantName ?: "Schatzsuche",
                        actions = {
                            SchatzTextButton(onClick = { showCancelDialog = true }) {
                                Text("Schatzsuche abbrechen", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TreasureMap(
                        theme = uiState.huntTheme,
                        totalSteps = uiState.totalSteps,
                        completedSteps = uiState.completedCount,
                        animateLatest = true
                    )

                    AnimatedContent(
                        targetState = uiState.phase to uiState.currentStep,
                        transitionSpec = {
                            (fadeIn(tween(350)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(200)) + slideOutVertically { -it / 4 })
                        },
                        label = "playStep"
                    ) { (phase, step) ->
                    when (phase) {
                        PlayPhase.INSTRUCTION, PlayPhase.SCAN -> {
                            if (step != null) {
                                Text(
                                    "${step.orderIndex + 1}. ${step.title}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                ContentBlocksDisplay(
                                    blocks = step.instructionJson.toParticipantContentBlocks(step.orderIndex + 1),
                                    immersiveMedia = true
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.showScanner() },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                                    colors = SchatzButtonDefaults.filledButtonColors()
                                ) {
                                    Text("QR-Code scannen", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        PlayPhase.POST_TASKS -> {
                            Text(
                                "Aufgabe erfüllen",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            step?.let {
                                PostScanTasksForm(
                                    tasks = it.postScanTasksJson.toPostScanTasks(),
                                    responses = uiState.taskResponses,
                                    onResponse = { task, response ->
                                        viewModel.updateTaskResponse(task, response)
                                    },
                                    error = uiState.taskError
                                )
                            }
                            Button(onClick = { viewModel.submitPostTasks() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Aufgabe abschließen")
                            }
                        }
                        PlayPhase.TREASURE -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎉", style = MaterialTheme.typography.displayLarge)
                                Text(
                                    "Glückwunsch!",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Du hast den Schatz gefunden!",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                step?.treasureHint?.let { hint ->
                                    Spacer(Modifier.height(16.dp))
                                    Text("Hinweis:", fontWeight = FontWeight.Bold)
                                    Text(
                                        hint,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { viewModel.finishTreasure() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Abenteuer beenden")
                                }
                            }
                        }
                        PlayPhase.COMPLETED -> {
                            Text("Schatzsuche abgeschlossen…")
                        }
                    }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.transitionHint != null,
                enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.88f, animationSpec = tween(250)),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.92f, animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                uiState.transitionHint?.let { hint ->
                    StepTransitionOverlay(hint)
                }
            }

            if (uiState.phase == PlayPhase.SCAN) {
                QrScannerOverlay(
                    scanActive = uiState.scanActive,
                    scanMessage = uiState.scanMessage,
                    scanMessageSuccess = uiState.scanMessageSuccess,
                    hintNumber = uiState.currentStep?.orderIndex?.plus(1),
                    onDismiss = { viewModel.dismissScanner() },
                    onTriggerScan = { viewModel.triggerScan() },
                    onQrScanned = { viewModel.onQrScanned(it) }
                )
            }
        }
    }

    if (showCancelDialog) {
        SchatzConfirmDialog(
            onDismissRequest = { showCancelDialog = false },
            title = "Schatzsuche abbrechen?",
            message = "Wenn du jetzt abbrichst, geht dein bisheriger Fortschritt verloren.",
            primaryLabel = "Weiterspielen",
            onPrimary = { showCancelDialog = false },
            secondaryLabel = "Schatzsuche abbrechen",
            onSecondary = {
                showCancelDialog = false
                viewModel.cancelSession(onCancelled)
            },
            secondaryDestructive = true
        )
    }
}

@Composable
private fun StepTransitionOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun QrScannerOverlay(
    scanActive: Boolean,
    scanMessage: String?,
    scanMessageSuccess: Boolean?,
    hintNumber: Int?,
    onDismiss: () -> Unit,
    onTriggerScan: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        QrScannerView(
            onQrScanned = onQrScanned,
            scanActive = scanActive,
            fullScreen = true,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }

            Spacer(Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                hintNumber?.let { number ->
                    Text(
                        "Richte die Kamera auf Hinweis Nr. $number",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                scanMessage?.let { message ->
                    val isSuccess = scanMessageSuccess == true
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFFC62828)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            message,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                if (scanActive) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Scanne…", color = Color.White)
                } else if (scanMessageSuccess != true) {
                    Button(
                        onClick = onTriggerScan,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                        colors = SchatzButtonDefaults.filledButtonColors()
                    ) {
                        Text("Jetzt scannen", style = MaterialTheme.typography.titleMedium)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f))
                ) {
                    Text("Zurück zu den Anweisungen", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
