package de.schatzsuche.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import de.schatzsuche.app.data.model.HuntTheme

@Composable
fun SchatzsucheTheme(
    huntTheme: HuntTheme = HuntTheme.CLASSIC,
    content: @Composable () -> Unit
) {
    val palette = huntTheme.toPalette()
    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        background = palette.background,
        surface = palette.surface,
        onPrimary = palette.onPrimary,
        onBackground = palette.onBackground,
        onSurface = palette.onBackground,
        tertiary = palette.accent
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
