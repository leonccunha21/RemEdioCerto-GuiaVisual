package com.zmstore.projectr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MedicleanTeal,
    secondary = MedicleanTealLight,
    tertiary = MedicleanGold,
    background = Color(0xFF0F1716), // Deeper, more neutral dark background
    surface = Color(0xFF17201F),    // Slightly lighter surface for elevation
    onPrimary = Color.White,
    onSecondary = Color(0xFF003734),
    onTertiary = Color.Black,
    onBackground = Color(0xFFE1EAEA),
    onSurface = Color(0xFFE1EAEA),
    surfaceVariant = Color(0xFF232D2B),
    onSurfaceVariant = Color(0xFFB0BEBC),
    error = MedicleanError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MedicleanTeal,
    secondary = MedicleanTealDark,
    tertiary = MedicleanGold,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = MedicleanDarkGreen,
    onSurface = MedicleanDarkGreen,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = MedicleanTealDark,
    error = MedicleanError,
    onError = Color.White
)

@Composable
fun ProjectRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desabilitado para manter a identidade visual Mediclean
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
