package de.schatzsuche.app.ui.screens.play

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.schatzsuche.app.data.model.toContentBlocks
import de.schatzsuche.app.data.model.toPostScanTasks
import de.schatzsuche.app.ui.components.ContentBlocksDisplay
import de.schatzsuche.app.ui.components.PostScanTasksForm
import de.schatzsuche.app.ui.components.QrScannerView
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.components.TreasureMap
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
    val completions by viewModel.completions.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(completions.size) {
        if (completions.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
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
                            TextButton(onClick = { showCancelDialog = true }) {
                                Text("Abbrechen")
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

                    when (uiState.phase) {
                        PlayPhase.INSTRUCTION, PlayPhase.SCAN -> {
                            val step = uiState.currentStep
                            if (step != null) {
                                Text(
                                    step.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Suche Hinweis Nr. ${step.orderIndex + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                ContentBlocksDisplay(
                                    blocks = step.instructionJson.toContentBlocks(),
                                    immersiveMedia = true
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.showScanner() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("QR-Code scannen")
                                }
                            }
                        }
                        PlayPhase.POST_TASKS -> {
                            val step = uiState.currentStep
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
                            val step = uiState.currentStep
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
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Schatzsuche abbrechen?") },
            text = { Text("Der Fortschritt geht verloren.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelSession(onCancelled)
                }) { Text("Abbrechen") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Weiterspielen") }
            }
        )
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jetzt scannen")
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zurück zu den Anweisungen", color = Color.White)
                }
            }
        }
    }
}
