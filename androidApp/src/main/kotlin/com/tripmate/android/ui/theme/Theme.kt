package com.tripmate.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A warm "travel journal" palette rather than a stock Material blue —
// terracotta as the accent (think sun-worn maps and boarding passes),
// deep teal for structure.
val Terracotta = Color(0xFFC96F4A)
val TerracottaDark = Color(0xFFE08A66)
val DeepTeal = Color(0xFF1F3D3E)
val Sand = Color(0xFFF6EFE7)
val SandDark = Color(0xFF14201F)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = DeepTeal,
    background = Sand,
    surface = Color.White,
    onBackground = DeepTeal,
    onSurface = DeepTeal,
)

private val DarkColors = darkColorScheme(
    primary = TerracottaDark,
    onPrimary = Color.Black,
    secondary = Color(0xFF9FC6C6),
    background = SandDark,
    surface = Color(0xFF1C2B2A),
    onBackground = Sand,
    onSurface = Sand,
)

@Composable
fun TripMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TripMateTypography,
        content = content,
    )
}
