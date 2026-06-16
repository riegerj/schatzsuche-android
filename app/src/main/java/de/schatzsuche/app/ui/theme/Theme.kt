package de.schatzsuche.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.schatzsuche.app.data.model.HuntTheme

@Composable
fun SchatzsucheTheme(
    huntTheme: HuntTheme = HuntTheme.CLASSIC,
    content: @Composable () -> Unit
) {
    val palette = huntTheme.toPalette()
    val onSurface = palette.onBackground
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color(0xFF1A1A1A),
        primaryContainer = palette.primary,
        onPrimaryContainer = palette.onPrimary,
        secondary = palette.secondary,
        onSecondary = palette.onPrimary,
        background = palette.background,
        surface = palette.surface,
        onBackground = onSurface,
        onSurface = onSurface,
        onSurfaceVariant = onSurface.copy(alpha = 0.92f),
        surfaceVariant = palette.surface.copy(alpha = 0.92f),
        tertiary = palette.accent,
        onTertiary = Color(0xFF1A1A1A),
        outline = onSurface.copy(alpha = 0.72f),
        outlineVariant = onSurface.copy(alpha = 0.45f),
        error = Color(0xFFEF5350),
        onError = Color.White,
        errorContainer = Color(0xFF5C1F1F),
        onErrorContainer = Color(0xFFFFCDD2)
    )

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 48.dp
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SchatzsucheTypography,
            content = {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    content()
                }
            }
        )
    }
}

object SchatzButtonDefaults {
    @Composable
    fun filledButtonPadding() = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

    @Composable
    fun textButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )

    @Composable
    fun outlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun filledButtonColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}
