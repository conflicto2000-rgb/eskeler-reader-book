package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF031A26),
    primaryContainer = Color(0xFF0A3A54),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = ElectricPurple,
    onSecondary = Color(0xFF280747),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = NeonIndigo,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = Color(0xFF334155),
    error = Color(0xFFEF4444)
)

private val LightColorScheme = darkColorScheme( // Default to dark aesthetic even if light requested for rich audio player look
    primary = ElectricCyan,
    onPrimary = Color.Black,
    secondary = ElectricPurple,
    background = DarkBackground,
    surface = DarkSurface
)

@Composable
fun AudioBookReaderTheme(
    darkTheme: Boolean = true, // Dark by default as requested
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
