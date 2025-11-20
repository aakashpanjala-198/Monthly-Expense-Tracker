package com.monica.monthlyexpensetracker.ui.theme

import android.app.Activity
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
    primary = AuroraTeal,
    onPrimary = AuroraMidnight,
    secondary = AuroraCoral,
    onSecondary = AuroraMidnight,
    tertiary = AuroraSunrise,
    onTertiary = AuroraMidnight,
    background = AuroraMidnight,
    onBackground = AuroraMist,
    surface = AuroraSmoke,
    onSurface = AuroraMist,
    surfaceVariant = AuroraSlate,
    onSurfaceVariant = AuroraMist.copy(alpha = 0.85f)
)

private val LightColorScheme = lightColorScheme(
    primary = AuroraPlum,
    onPrimary = Color.White,
    secondary = AuroraTeal,
    onSecondary = Color.White,
    tertiary = AuroraCoral,
    onTertiary = Color.White,
    background = AuroraMist,
    onBackground = AuroraMidnight,
    surface = Color.White,
    onSurface = AuroraMidnight,
    surfaceVariant = Color(0xFFF1F3FF),
    onSurfaceVariant = Color(0xFF3F445F)
)

@Composable
fun MonthlyExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}