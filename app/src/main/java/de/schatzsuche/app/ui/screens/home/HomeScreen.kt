package de.schatzsuche.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schatzsuche.app.data.repository.SchatzsucheRepository
import de.schatzsuche.app.ui.theme.SchatzButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    repository: SchatzsucheRepository,
    onAdmin: () -> Unit,
    onParticipant: () -> Unit,
    onSetup: () -> Unit
) {
    var needsSetup by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        needsSetup = withContext(Dispatchers.IO) {
            repository.getQrCodeCount() == 0
        }
    }

    if (needsSetup == true) {
        LaunchedEffect(Unit) { onSetup() }
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏴‍☠️", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "Schatzsuche",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Erlebe spannende Abenteuer mit QR-Codes!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )
            Button(
                onClick = onParticipant,
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                colors = SchatzButtonDefaults.filledButtonColors()
            ) {
                Text("Teilnehmer-Modus", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onAdmin,
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                contentPadding = SchatzButtonDefaults.filledButtonPadding(),
                colors = SchatzButtonDefaults.filledButtonColors()
            ) {
                Text("Admin-Modus", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
