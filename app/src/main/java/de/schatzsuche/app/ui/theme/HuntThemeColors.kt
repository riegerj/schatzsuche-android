package de.schatzsuche.app.ui.theme

import androidx.compose.ui.graphics.Color
import de.schatzsuche.app.data.model.HuntTheme

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val accent: Color,
    val mapPath: Color,
    val mapDot: Color
)

fun HuntTheme.toPalette(): ThemePalette = when (this) {
    HuntTheme.PIRATES -> ThemePalette(
        primary = Color(0xFF5D3A1A),
        secondary = Color(0xFF8B6914),
        background = Color(0xFF1A2F4A),
        surface = Color(0xFF2C4A6E),
        onPrimary = Color(0xFFFFD700),
        onBackground = Color(0xFFF5E6C8),
        accent = Color(0xFFFFD700),
        mapPath = Color(0xFFFFD700),
        mapDot = Color(0xFFCD853F)
    )
    HuntTheme.SPACE -> ThemePalette(
        primary = Color(0xFF1B1B3A),
        secondary = Color(0xFF4A3F8C),
        background = Color(0xFF0A0A1F),
        surface = Color(0xFF1E1E3F),
        onPrimary = Color(0xFFE0E0FF),
        onBackground = Color(0xFFE8E8FF),
        accent = Color(0xFF7B68EE),
        mapPath = Color(0xFF00FFFF),
        mapDot = Color(0xFFFF69B4)
    )
    HuntTheme.KNIGHTS -> ThemePalette(
        primary = Color(0xFF3D2B1F),
        secondary = Color(0xFF6B4423),
        background = Color(0xFF1C1410),
        surface = Color(0xFF2E2218),
        onPrimary = Color(0xFFD4AF37),
        onBackground = Color(0xFFE8D5B7),
        accent = Color(0xFFC0C0C0),
        mapPath = Color(0xFFD4AF37),
        mapDot = Color(0xFF8B0000)
    )
    HuntTheme.EGYPT -> ThemePalette(
        primary = Color(0xFF8B6914),
        secondary = Color(0xFFB8860B),
        background = Color(0xFF2A1F0A),
        surface = Color(0xFF3D2E14),
        onPrimary = Color(0xFF1A1408),
        onBackground = Color(0xFFF5DEB3),
        accent = Color(0xFF00CED1),
        mapPath = Color(0xFF00CED1),
        mapDot = Color(0xFFDAA520)
    )
    HuntTheme.CLASSIC -> ThemePalette(
        primary = Color(0xFF2E5E4E),
        secondary = Color(0xFF4A8B6F),
        background = Color(0xFF1A2E26),
        surface = Color(0xFF2A4038),
        onPrimary = Color(0xFFF0FFF0),
        onBackground = Color(0xFFE8F5E9),
        accent = Color(0xFFFFB74D),
        mapPath = Color(0xFFFFB74D),
        mapDot = Color(0xFF81C784)
    )
}
