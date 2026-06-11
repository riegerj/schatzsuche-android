package de.schatzsuche.app.ui.screens.participant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.schatzsuche.app.ui.components.SchatzAppBar
import de.schatzsuche.app.ui.viewmodel.ParticipantViewModel

@Composable
fun ParticipantHomeScreen(
    viewModel: ParticipantViewModel,
    onStartHunt: (String) -> Unit,
    onBack: () -> Unit
) {
    val hunts by viewModel.hunts.collectAsState()

    Scaffold(topBar = { SchatzAppBar("Teilnehmer-Modus", onBack = onBack) }) { padding ->
        if (hunts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Keine Schatzsuchen verfügbar.")
                Text("Bitte im Admin-Modus eine Schatzsuche erstellen.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(hunts) { hunt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onStartHunt(hunt.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("${hunt.theme.emoji} ${hunt.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(hunt.theme.displayName, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Tippen zum Starten →", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantStartScreen(
    viewModel: ParticipantViewModel,
    huntId: String,
    onStarted: (String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val hunts by viewModel.hunts.collectAsState()
    val hunt = hunts.find { it.id == huntId }

    Scaffold(topBar = { SchatzAppBar("Schatzsuche starten", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            hunt?.let {
                Text("${it.theme.emoji} ${it.title}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(32.dp))
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Spieler- oder Teamname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.startSession(huntId, name, onStarted) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abenteuer beginnen!")
            }
        }
    }
}
