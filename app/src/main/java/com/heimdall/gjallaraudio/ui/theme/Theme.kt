package com.heimdall.gjallaraudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GjallarDarkColorScheme = darkColorScheme(
    primary = GjallarCyan,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = GjallarCyanLight,
    secondary = GjallarAmber,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = GjallarAmberLight,
    tertiary = PurpleNeon,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate600
)

@Composable
fun GjallarAudioTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = GjallarDarkColorScheme // Always use the Sentinel Heimdall theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Slate900.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Slate900.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
