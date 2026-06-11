package de.schatzsuche.app.ui.screens.setup

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.viewmodel.SetupViewModel

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

    Scaffold(
        topBar = { SchatzAppBar("Erstkonfiguration", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            if (isLoading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            Button(
                onClick = { viewModel.generateAndSave(regenerate = true) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("QR-Karten generieren & PDF erstellen")
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
    }
}
