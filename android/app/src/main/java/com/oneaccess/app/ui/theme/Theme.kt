package com.oneaccess.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark-only "premium" palette: deep background, soft surfaces, restrained accent.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),        // soft premium emerald accent
    onPrimary = Color(0xFF0B1220),
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF0B1220),
    background = Color(0xFF0B0F19),     // near-black
    onBackground = Color(0xFFE5E7EB),   // cool white
    surface = Color(0xFF0F172A),        // slate surface
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF111C33), // subtle card variant
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF24324D),
)

@Composable
fun OneAccessTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}

