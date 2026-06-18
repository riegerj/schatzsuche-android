package de.schatzsuche.app.ui.screens.admin

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import de.schatzsuche.app.data.model.ContentBlockType
import de.schatzsuche.app.data.model.HuntSessionStatus
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.RichContentBlock
import de.schatzsuche.app.data.model.TreasureHuntEntity
import de.schatzsuche.app.data.model.toContentBlocks
import de.schatzsuche.app.data.model.toPostScanTasks
import de.schatzsuche.app.data.model.toTaskResponses
import de.schatzsuche.app.data.repository.SchatzsucheRepository
import de.schatzsuche.app.ui.components.ContentBlocksDisplay
import de.schatzsuche.app.ui.components.InstructionAudioPlayer
import de.schatzsuche.app.ui.components.InstructionImageDisplay
import de.schatzsuche.app.ui.components.InstructionMediaActions
import de.schatzsuche.app.ui.components.InstructionVideoPlayer
import de.schatzsuche.app.ui.components.LoadingBox
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.components.SchatzConfirmDialog
import de.schatzsuche.app.ui.components.ThemeChip
import de.schatzsuche.app.ui.viewmodel.AdminViewModel
import de.schatzsuche.app.ui.screens.setup.InitialSetupScreen
import de.schatzsuche.app.ui.viewmodel.SetupViewModel
import de.schatzsuche.app.ui.viewmodel.StepEditViewModel
import de.schatzsuche.app.ui.viewmodel.StepEditViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminHomeScreen(
    viewModel: AdminViewModel,
    onEditHunt: (String) -> Unit,
    onSessions: () -> Unit,
    onQrPdf: () -> Unit,
    onBack: () -> Unit
) {
    val hunts by viewModel.hunts.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var huntPendingDelete by remember { mutableStateOf<TreasureHuntEntity?>(null) }

    Scaffold(
        topBar = {
            SchatzAppBar(
                title = "Admin-Modus",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onSessions) {
                        Icon(Icons.Default.History, contentDescription = "Verlauf")
                    }
                    IconButton(onClick = onQrPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "QR-PDF")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Neue Schatzsuche")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedButton(
                onClick = onQrPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("QR-Karten & Erstkonfiguration")
            }

            if (hunts.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Noch keine Schatzsuchen erstellt.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Erste Schatzsuche anlegen")
                    }
                }
            } else {
                LazyColumn(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(hunts) { hunt ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onEditHunt(hunt.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(hunt.theme.emoji, style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(hunt.title, fontWeight = FontWeight.Bold)
                                    Text("${hunt.theme.displayName}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { huntPendingDelete = hunt }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateHuntDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, theme ->
                viewModel.createHunt(title, theme) { id ->
                    showCreateDialog = false
                    onEditHunt(id)
                }
            }
        )
    }

    huntPendingDelete?.let { hunt ->
        SchatzConfirmDialog(
            onDismissRequest = { huntPendingDelete = null },
            title = "Schatzsuche löschen?",
            message = "Möchtest du die Schatzsuche „${hunt.title}“ wirklich löschen? Alle Schritte und zugehörigen Daten gehen dabei verloren. Diese Aktion kann nicht rückgängig gemacht werden.",
            primaryLabel = "Abbrechen",
            onPrimary = { huntPendingDelete = null },
            secondaryLabel = "Schatzsuche löschen",
            onSecondary = {
                viewModel.deleteHunt(hunt.id)
                huntPendingDelete = null
            },
            secondaryDestructive = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHuntDialog(onDismiss: () -> Unit, onCreate: (String, HuntTheme) -> Unit) {
    var title by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf(HuntTheme.PIRATES) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Schatzsuche") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Theme wählen:")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HuntTheme.entries.forEach { t ->
                        ThemeChip(t, selected = theme == t) { theme = t }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onCreate(title, theme) },
                enabled = title.isNotBlank()
            ) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HuntEditScreen(
    viewModel: AdminViewModel,
    huntId: String,
    onEditStep: (String, String) -> Unit,
    onQrHelp: (String) -> Unit,
    onBack: () -> Unit
) {
    val steps by viewModel.observeSteps(huntId).collectAsState(initial = emptyList())
    val qrCodes by viewModel.qrCodes.collectAsState()
    var huntTitle by remember { mutableStateOf("") }
    var huntTheme by remember { mutableStateOf(HuntTheme.CLASSIC) }
    var stepPendingDelete by remember { mutableStateOf<de.schatzsuche.app.data.model.HuntStepEntity?>(null) }

    LaunchedEffect(huntId) {
        val hunt = viewModel.getHunt(huntId)
        huntTitle = hunt?.title.orEmpty()
        huntTheme = hunt?.theme ?: HuntTheme.CLASSIC
        viewModel.syncStepQrAssignments(huntId)
    }

    Scaffold(
        topBar = {
            SchatzAppBar(
                title = huntTitle.ifBlank { "Schatzsuche bearbeiten" },
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onQrHelp(huntId) }) {
                        Icon(Icons.Default.Help, contentDescription = "QR-Hilfe")
                    }
                }
            )
        },
        floatingActionButton = {
            if (steps.size < qrCodes.size) {
                FloatingActionButton(onClick = { viewModel.addStep(huntId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Schritt hinzufügen")
                }
            }
        }
    ) { padding ->
        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            if (fromIndex in steps.indices && toIndex in steps.indices) {
                viewModel.reorderSteps(huntId, fromIndex, toIndex)
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = huntTitle,
                    onValueChange = {
                        huntTitle = it
                        viewModel.updateHunt(
                            de.schatzsuche.app.data.model.TreasureHuntEntity(
                                id = huntId, title = it, theme = huntTheme
                            )
                        )
                    },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Theme:", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HuntTheme.entries.forEach { t ->
                        ThemeChip(t, selected = huntTheme == t) {
                            huntTheme = t
                            viewModel.updateHunt(
                                de.schatzsuche.app.data.model.TreasureHuntEntity(
                                    id = huntId, title = huntTitle, theme = t
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Schritte", fontWeight = FontWeight.SemiBold)
                Text(
                    "Zum Sortieren am Griff ziehen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            items(steps, key = { it.id }) { step ->
                val defaultTitle = "Schritt ${step.orderIndex + 1}"
                val contentBlocks = step.instructionJson.toContentBlocks()
                val postTasks = step.postScanTasksJson.toPostScanTasks()
                val hasCustomText = contentBlocks
                    .any { it.type == de.schatzsuche.app.data.model.ContentBlockType.TEXT && !it.text.isNullOrBlank() && it.text != "Beschreibe hier das Rätsel…" }
                val hasMediaBlocks = contentBlocks.any {
                    (it.type == de.schatzsuche.app.data.model.ContentBlockType.IMAGE ||
                        it.type == de.schatzsuche.app.data.model.ContentBlockType.AUDIO ||
                        it.type == de.schatzsuche.app.data.model.ContentBlockType.VIDEO) &&
                        !it.mediaPath.isNullOrBlank()
                }
                val isEdited = step.title != defaultTitle ||
                    hasCustomText ||
                    hasMediaBlocks ||
                    postTasks.isNotEmpty() ||
                    !step.treasureHint.isNullOrBlank()

                ReorderableItem(reorderableState, key = step.id) { isDragging ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                isEdited -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                modifier = Modifier.draggableHandle(),
                                onClick = {}
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Schritt verschieben",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable { onEditStep(huntId, step.id) }
                                    .padding(vertical = 8.dp)
                            ) {
                            Text(
                                "${step.orderIndex + 1}. ${step.title}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "QR-Karte #${(step.orderIndex + 1).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            if (isEdited) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Bearbeitet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    "Noch nicht bearbeitet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (step.isFinalStep) {
                                Text("🏆 Schatz-Schritt", style = MaterialTheme.typography.bodySmall)
                            }
                            }
                            IconButton(onClick = { stepPendingDelete = step }) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            }
        }
    }

    stepPendingDelete?.let { step ->
        SchatzConfirmDialog(
            onDismissRequest = { stepPendingDelete = null },
            title = "Schritt löschen?",
            message = "Möchtest du den Schritt „${step.title}“ wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.",
            primaryLabel = "Abbrechen",
            onPrimary = { stepPendingDelete = null },
            secondaryLabel = "Schritt löschen",
            onSecondary = {
                viewModel.deleteStep(step.id)
                stepPendingDelete = null
            },
            secondaryDestructive = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepEditScreen(
    viewModel: AdminViewModel,
    huntId: String,
    stepId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as de.schatzsuche.app.SchatzsucheApplication).repository
    val stepVm: StepEditViewModel = viewModel(
        factory = StepEditViewModelFactory(repository, huntId, stepId)
    )
    val state by stepVm.state.collectAsState()
    val huntSteps by viewModel.observeSteps(huntId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mediaPendingDelete by remember { mutableStateOf<RichContentBlock?>(null) }

    val saveAndBack: () -> Unit = {
        scope.launch {
            stepVm.save()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            SchatzAppBar("Schritt bearbeiten", onBack = saveAndBack)
        }
    ) { padding ->
        val step = state.step
        if (step == null) {
            LoadingBox()
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = step.title,
                onValueChange = { stepVm.updateTitle(it) },
                label = { Text("Überschrift") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Anweisungen / Rätsel", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.instructionText,
                onValueChange = { stepVm.updateInstructionText(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Anweisungstext") }
            )

            state.mediaBlocks.forEach { block ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        when (block.type) {
                            de.schatzsuche.app.data.model.ContentBlockType.IMAGE -> {
                                block.mediaPath?.let { path ->
                                    InstructionImageDisplay(
                                        path = path,
                                        modifier = Modifier.height(120.dp)
                                    )
                                }
                            }
                            de.schatzsuche.app.data.model.ContentBlockType.AUDIO -> {
                                block.mediaPath?.let { path ->
                                    InstructionAudioPlayer(path = path, immersiveMedia = false)
                                }
                            }
                            de.schatzsuche.app.data.model.ContentBlockType.VIDEO -> {
                                block.mediaPath?.let { path ->
                                    InstructionVideoPlayer(
                                        path = path,
                                        immersiveMedia = false,
                                        onOpenFullScreen = {}
                                    )
                                }
                            }
                            else -> Unit
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { mediaPendingDelete = block }) {
                                Icon(Icons.Default.Delete, contentDescription = "Entfernen")
                            }
                        }
                    }
                }
            }
            Text("Medien hinzufügen", fontWeight = FontWeight.Bold)
            InstructionMediaActions(
                onPickUri = { uri, type -> stepVm.addMediaBlock(context, uri, type) },
                onCapturedFile = { file, type -> stepVm.addCapturedMediaFile(file, type) }
            )

            Text(
                "QR-Karte #${(step.orderIndex + 1).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Die Zuordnung zur QR-Karte erfolgt automatisch nach der Schritt-Reihenfolge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            val isLastStep = huntSteps.lastOrNull()?.id == step.id
            if (isLastStep) {
                Text("🏆 Schatz-Schritt", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = step.treasureHint.orEmpty(),
                    onValueChange = { stepVm.updateTreasureHint(it) },
                    label = { Text("Schatz-Hinweis (z.B. Zahlencode)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Aufgaben nach QR-Scan", fontWeight = FontWeight.Bold)
            state.postScanTasks.forEach { task ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = task.question,
                            onValueChange = { stepVm.updatePostScanTask(task.copy(question = it)) },
                            label = { Text("Frage") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (task.type == PostScanTaskType.SINGLE_CHOICE ||
                            task.type == PostScanTaskType.MULTIPLE_CHOICE
                        ) {
                            OutlinedTextField(
                                value = task.options.joinToString("\n"),
                                onValueChange = {
                                    stepVm.updatePostScanTask(task.copy(options = it.lines().filter { l -> l.isNotBlank() }))
                                },
                                label = { Text("Optionen (eine pro Zeile)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            OutlinedTextField(
                                value = task.correctAnswers.joinToString("\n"),
                                onValueChange = {
                                    stepVm.updatePostScanTask(task.copy(correctAnswers = it.lines().filter { l -> l.isNotBlank() }))
                                },
                                label = { Text("Richtige Antwort(en)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        TextButton(onClick = { stepVm.removePostScanTask(task.id) }) {
                            Text("Aufgabe entfernen")
                        }
                    }
                }
            }
            Text("Aufgabe hinzufügen:", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PostScanTaskType.entries.forEach { type ->
                    OutlinedButton(onClick = { stepVm.addPostScanTask(type) }) {
                        Text(type.name.take(12))
                    }
                }
            }

            Button(onClick = saveAndBack, modifier = Modifier.fillMaxWidth()) {
                Text("Speichern")
            }
        }
    }

    mediaPendingDelete?.let { block ->
        val mediaLabel = when (block.type) {
            ContentBlockType.IMAGE -> "Bild"
            ContentBlockType.AUDIO -> "Audioaufnahme"
            ContentBlockType.VIDEO -> "Video"
            else -> "Medium"
        }
        SchatzConfirmDialog(
            onDismissRequest = { mediaPendingDelete = null },
            title = "$mediaLabel löschen?",
            message = "Möchtest du diese $mediaLabel wirklich entfernen? Diese Aktion kann nicht rückgängig gemacht werden.",
            primaryLabel = "Abbrechen",
            onPrimary = { mediaPendingDelete = null },
            secondaryLabel = "$mediaLabel löschen",
            onSecondary = {
                stepVm.removeMediaBlock(block.id)
                mediaPendingDelete = null
            },
            secondaryDestructive = true
        )
    }
}

@Composable
fun QrHelpScreen(viewModel: AdminViewModel, huntId: String, onBack: () -> Unit) {
    val steps by viewModel.observeSteps(huntId).collectAsState(initial = emptyList())
    val qrCodes by viewModel.qrCodes.collectAsState()

    Scaffold(topBar = { SchatzAppBar("QR-Versteck-Hilfe", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(
                "Übersicht: Welcher QR-Code gehört zu welchem Schritt?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            steps.forEach { step ->
                val qr = qrCodes.find { it.codeId == step.qrCodeId }
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${step.orderIndex + 1}. ${step.title}", fontWeight = FontWeight.Bold)
                        Text(
                            "QR-Karte: #${qr?.number?.toString()?.padStart(2, '0') ?: "?"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (step.isFinalStep) Text("🏆 Hier versteckst du den Schatz!")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionsOverviewScreen(
    viewModel: AdminViewModel,
    onSessionClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val hunts by viewModel.hunts.collectAsState()
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY) }

    Scaffold(topBar = { SchatzAppBar("Durchgeführte Schatzsuchen", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions) { session ->
                val hunt = hunts.find { it.id == session.huntId }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSessionClick(session.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(hunt?.title ?: "Unbekannt", fontWeight = FontWeight.Bold)
                        Text("Teilnehmer: ${session.participantName}")
                        Text("Gestartet: ${fmt.format(Date(session.startedAt))}")
                        Text("Status: ${session.status.name}")
                        session.finishedAt?.let { end ->
                            val duration = end - session.startedAt
                            Text("Dauer: ${duration / 60000} Min ${(duration % 60000) / 1000} Sek")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(repository: SchatzsucheRepository, sessionId: String, onBack: () -> Unit) {
    var details by remember { mutableStateOf<de.schatzsuche.app.data.repository.SessionDetails?>(null) }

    LaunchedEffect(sessionId) {
        details = withContext(Dispatchers.IO) { repository.getSessionDetails(sessionId) }
    }

    Scaffold(topBar = { SchatzAppBar("Sitzungsdetails", onBack = onBack) }) { padding ->
        val d = details
        if (d == null) {
            LoadingBox()
        } else {
            Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(d.hunt.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Teilnehmer: ${d.session.participantName}")
                Text("Status: ${d.session.status}")
                Spacer(Modifier.height(16.dp))
                d.completions.forEach { completion ->
                    val step = d.steps.find { it.id == completion.stepId }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(step?.title ?: "Schritt", fontWeight = FontWeight.Bold)
                            val dur = completion.completedAt - completion.startedAt
                            Text("Dauer: ${dur / 1000} Sek")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrPdfScreen(viewModel: SetupViewModel, onBack: () -> Unit) {
    InitialSetupScreen(viewModel = viewModel, onDone = onBack, onBack = onBack)
}
