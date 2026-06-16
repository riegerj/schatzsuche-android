package de.schatzsuche.app.ui.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.toContentBlocks
import de.schatzsuche.app.data.model.toTaskResponses
import de.schatzsuche.app.ui.components.ContentBlocksDisplay
import de.schatzsuche.app.ui.components.LoadingBox
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.components.TreasureMap
import de.schatzsuche.app.ui.theme.SchatzsucheTheme
import de.schatzsuche.app.ui.viewmodel.SummaryViewModel

@Composable
fun SummaryScreen(viewModel: SummaryViewModel, onHome: () -> Unit) {
    val details by viewModel.details.collectAsState()

    if (details == null) {
        LoadingBox()
        return
    }

    val d = details!!
    val totalDuration = (d.session.finishedAt ?: System.currentTimeMillis()) - d.session.startedAt

    SchatzsucheTheme(huntTheme = d.hunt.theme) {
        Scaffold(
            topBar = { SchatzAppBar("Zusammenfassung") }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🏆", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "Abenteuer abgeschlossen!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${d.hunt.theme.emoji} ${d.hunt.title}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text("Teilnehmer: ${d.session.participantName}")
                    Text("Gesamtdauer: ${viewModel.formatDuration(totalDuration)}")
                    Text("Beendet: ${viewModel.formatDateTime(d.session.finishedAt ?: System.currentTimeMillis())}")
                }

                TreasureMap(
                    theme = d.hunt.theme,
                    totalSteps = d.steps.size,
                    completedSteps = d.completions.size,
                    animateLatest = false
                )

                HorizontalDivider()

                Text("Verlauf", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                d.completions.forEach { completion ->
                    val step = d.steps.find { it.id == completion.stepId }
                    val stepDuration = completion.completedAt - completion.startedAt
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${completion.stepIndex + 1}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    step?.title ?: "Schritt",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Dauer: ${viewModel.formatDuration(stepDuration)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            step?.let {
                                Spacer(Modifier.height(8.dp))
                                ContentBlocksDisplay(
                                    blocks = it.instructionJson.toContentBlocks(),
                                    showMedia = false
                                )
                            }

                            val responses = completion.taskResponsesJson.toTaskResponses()
                            if (responses.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Tagebuch & Antworten:", fontWeight = FontWeight.SemiBold)
                                responses.forEach { response ->
                                    when (response.type) {
                                        PostScanTaskType.TEXT_INPUT -> {
                                            response.textAnswer?.let { answer -> Text("»$answer«") }
                                        }
                                        PostScanTaskType.SINGLE_CHOICE,
                                        PostScanTaskType.MULTIPLE_CHOICE -> {
                                            Text(response.selectedOptions.joinToString(", "))
                                        }
                                        PostScanTaskType.PHOTO,
                                        PostScanTaskType.VIDEO,
                                        PostScanTaskType.AUDIO -> Unit
                                    }
                                }
                            }
                        }
                    }
                }

                Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Zur Startseite")
                }
            }
        }
    }
}
