package de.schatzsuche.app.ui.screens.setup

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import de.schatzsuche.app.ui.components.QrScannerView
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.viewmodel.SetupViewModel
import de.schatzsuche.app.util.QrCodeUtil

private enum class QrSetupMode {
    GENERATE_NEW,
    SCAN_EXISTING
}

@Composable
fun InitialSetupScreen(
    viewModel: SetupViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val qrCount by viewModel.qrCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pdfFile by viewModel.pdfFile.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    var mode by remember { mutableStateOf(QrSetupMode.GENERATE_NEW) }

    var importStarted by remember { mutableStateOf(false) }
    var targetNumber by remember { mutableStateOf(1) }
    val scannedPayloadsByNumber = remember { mutableMapOf<Int, String>() }

    var scanActive by remember { mutableStateOf(false) }
    var localScanMessage by remember { mutableStateOf<String?>(null) }
    var localScanSuccess by remember { mutableStateOf<Boolean?>(null) }

    val canShowScannerOverlay = mode == QrSetupMode.SCAN_EXISTING && importStarted && !isLoading && pdfFile == null

    fun startImport() {
        scannedPayloadsByNumber.clear()
        targetNumber = 1
        importStarted = true
        scanActive = false
        localScanMessage = null
        localScanSuccess = null
        viewModel.clearMessage()
    }

    fun cancelImport() {
        importStarted = false
        scanActive = false
        localScanMessage = null
        localScanSuccess = null
    }

    Scaffold(
        topBar = { SchatzAppBar("Erstkonfiguration", onBack = onBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "QR-Karten vorbereiten",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Wähle, wie viele QR-Karten du benötigst. Diese werden auf A4-Seiten gedruckt " +
                        "(max. 6 Karten pro Seite). Jede Karte hat eine lesbare Nummer (#01, #02, …).",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Anzahl QR-Karten: $qrCount", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = qrCount.toFloat(),
                            onValueChange = { viewModel.setQrCount(it.toInt()) },
                            valueRange = 6f..36f,
                            steps = 29
                        )
                        Text("Standard: 12 Karten", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quelle der QR-Karten", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { mode = QrSetupMode.GENERATE_NEW },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Neu generieren")
                            }
                            OutlinedButton(
                                onClick = { mode = QrSetupMode.SCAN_EXISTING },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Gedruckte einscannen")
                            }
                        }
                        Text(
                            "Hinweis: Beim Einscannen wird die QR-Kartenliste in der App neu befüllt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (isLoading) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }

                if (mode == QrSetupMode.GENERATE_NEW) {
                    Button(
                        onClick = { viewModel.generateAndSave(regenerate = true) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QR-Karten generieren & PDF erstellen")
                    }
                } else {
                    Button(
                        onClick = { startImport() },
                        enabled = !isLoading && !importStarted && pdfFile == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QR-Karten einlesen (Einscannen)")
                    }
                    if (importStarted) {
                        Text(
                            "Bitte scanne QR-Karte #${targetNumber.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }

                pdfFile?.let { file ->
                    OutlinedButton(
                        onClick = {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "PDF teilen"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PDF teilen / drucken")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Fertig – zur Startseite")
                }
            }

            if (canShowScannerOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    QrScannerView(
                        onQrScanned = { rawPayload ->
                            // QrScannerView ist ohnehin „nur einmal pro Klick“ aktiv,
                            // trotzdem sichern wir UI-Zustand hier ab.
                            scanActive = false

                            val codeId = QrCodeUtil.parsePayload(rawPayload)
                            if (codeId == null) {
                                localScanSuccess = false
                                localScanMessage =
                                    "Falscher QR-Code. Bitte eine QR-Karte mit „Schatzsuche“ scannen."
                            } else {
                                val alreadyUsedElsewhere = scannedPayloadsByNumber
                                    .filterKeys { it != targetNumber }
                                    .values
                                    .mapNotNull { QrCodeUtil.parsePayload(it) }
                                    .contains(codeId)

                                if (alreadyUsedElsewhere) {
                                    localScanSuccess = false
                                    localScanMessage =
                                        "Diese QR-Karte ist bereits einer anderen Nummer zugeordnet. Bitte prüfen."
                                } else {
                                    scannedPayloadsByNumber[targetNumber] = rawPayload
                                    localScanSuccess = true
                                    localScanMessage =
                                        "Karte #${targetNumber.toString().padStart(2, '0')} übernommen."

                                    val next = targetNumber + 1
                                    if (next > qrCount) {
                                        val scans = scannedPayloadsByNumber
                                            .toList()
                                            .sortedBy { it.first }
                                            .map { it.first to it.second }
                                        importStarted = false
                                        viewModel.importFromExistingQrScans(scans)
                                    } else {
                                        targetNumber = next
                                    }
                                }
                            }
                        },
                        scanActive = scanActive,
                        fullScreen = true,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Scanne QR-Karte #${targetNumber.toString().padStart(2, '0')}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        localScanMessage?.let { msg ->
                            val c = if (localScanSuccess == true) Color(0xFF2E7D32) else Color(0xFFC62828)
                            Text(msg, color = c, textAlign = Alignment.CenterHorizontally)
                        }

                        Button(
                            onClick = { scanActive = true },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Jetzt scannen")
                        }

                        OutlinedButton(
                            onClick = { cancelImport() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import abbrechen")
                        }
                    }
                }
            }
        }
    }
}
