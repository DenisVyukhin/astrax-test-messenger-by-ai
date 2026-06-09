package com.astrax.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

val AstraxAccent = Color(0xFF33ABF7)
val AstraxGreen = Color(0xFF31C76A)
val AstraxRed = Color(0xFFE85252)

@Composable
fun AstraxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val background by animateColorAsState(if (darkTheme) Color(0xFF111418) else Color(0xFFF4F7FA), label = "background")
    val surface by animateColorAsState(if (darkTheme) Color(0xFF1A1F25) else Color.White, label = "surface")
    val text by animateColorAsState(if (darkTheme) Color(0xFFE9EDF2) else Color(0xFF111418), label = "text")
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = AstraxAccent,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onBackground = text,
            onSurface = text,
            surfaceVariant = Color(0xFF262D35),
            onSurfaceVariant = Color(0xFFAAB4C0)
        )
    } else {
        lightColorScheme(
            primary = AstraxAccent,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onBackground = text,
            onSurface = text,
            surfaceVariant = Color(0xFFE8EEF4),
            onSurfaceVariant = Color(0xFF66717C)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
