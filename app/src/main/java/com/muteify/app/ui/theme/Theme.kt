package com.muteify.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val OledColorScheme = darkColorScheme(
    primary = OledPrimary,
    onPrimary = OledOnPrimary,
    primaryContainer = OledSurfaceVariant,
    onPrimaryContainer = OledText,
    secondary = OledSecondary,
    onSecondary = OledOnSecondary,
    secondaryContainer = OledSurfaceVariant,
    onSecondaryContainer = OledText,
    tertiary = OledTertiary,
    background = OledBlack,
    onBackground = OledText,
    surface = OledBlack,
    onSurface = OledText,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledMutedText,
    error = OledError
)

private val LightColorScheme = lightColorScheme(
    primary = UtilityLightPrimary,
    secondary = UtilityLightSecondary,
    tertiary = UtilityLightTertiary
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun MuteifyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        OledColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MuteifyTypography,
        content = content
    )
}
