package com.surajpole.parkingfinder.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    secondary = EmeraldGreen,
    onSecondary = SurfaceWhite,
    background = LightBackground,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    error = RedDanger,
    onError = SurfaceWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    secondary = EmeraldGreen,
    onSecondary = SurfaceWhite
)

@Composable
fun SurajpoleParkingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = NavyDark.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
