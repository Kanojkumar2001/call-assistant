package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = SlateSurfaceVariantDark,
    onPrimaryContainer = Color.White,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    tertiary = PurplePrimary,
    background = SlateBackgroundDark,
    onBackground = Color.White,
    surface = SlateSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = HighUrgencyRed
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurpleOnContainer,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = PurplePrimary,
    background = HighDensityBackground,
    onBackground = TextPrimaryDark,
    surface = HighDensitySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderOutline,
    error = HighUrgencyRed
)

@Composable
fun CallSenseTheme(
    darkTheme: Boolean = false, // High Density theme default
    dynamicColor: Boolean = false,
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
