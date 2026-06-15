package de.schatzsuche.app.ui.screens.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

    SchatzsucheTheme(huntTheme = uiState.huntTheme) {
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
                    PlayPhase.INSTRUCTION -> {
                        val step = uiState.currentStep
                        if (step != null) {
                            Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            ContentBlocksDisplay(step.instructionJson.toContentBlocks())
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Suche Hinweis Nr. ${step.orderIndex + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Button(onClick = { viewModel.showScanner() }, modifier = Modifier.fillMaxWidth()) {
                                Text("QR-Code scannen")
                            }
                        }
                    }
                    PlayPhase.SCAN -> {
                        Text("Richte die Kamera auf den QR-Code", fontWeight = FontWeight.Bold)
                        QrScannerView(onQrScanned = { viewModel.onQrScanned(it) })
                        uiState.scanError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = { viewModel.showInstruction() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Zurück zu den Anweisungen")
                        }
                    }
                    PlayPhase.POST_TASKS -> {
                        val step = uiState.currentStep
                        Text("Aufgabe erfüllen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        step?.let {
                            PostScanTasksForm(
                                tasks = it.postScanTasksJson.toPostScanTasks(),
                                responses = uiState.taskResponses,
                                onResponse = { task, response -> viewModel.updateTaskResponse(task, response) },
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
                                Text(hint, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
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
