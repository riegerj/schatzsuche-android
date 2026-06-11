package de.schatzsuche.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.schatzsuche.app.ui.navigation.SchatzsucheNavHost
import de.schatzsuche.app.ui.theme.SchatzsucheTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SchatzsucheApplication).repository
        setContent {
            SchatzsucheTheme {
                SchatzsucheNavHost(repository = repository)
            }
        }
    }
}
