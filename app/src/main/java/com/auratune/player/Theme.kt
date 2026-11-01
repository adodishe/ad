package com.auratune.player

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.materialLightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LavenderPrimary = Color(0xFFD0BCFF)
val LavenderSurface = Color(0xFF141318)

val DarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    surface = LavenderSurface,
    onSurface = Color(0xFFE6E1E5)
)

@Composable
fun AuraTuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
