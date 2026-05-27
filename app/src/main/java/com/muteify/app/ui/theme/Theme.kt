package com.muteify.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.muteify.app.data.model.AppTheme

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

private val DayColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DaySurfaceVariant,
    onPrimaryContainer = DayText,
    secondary = DaySecondary,
    tertiary = DayTertiary,
    background = DayBackground,
    onBackground = DayText,
    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayMutedText,
    error = DayError
)

private val NightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightSurfaceVariant,
    onPrimaryContainer = NightText,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedText,
    error = NightError
)

private val ReadingColorScheme = lightColorScheme(
    primary = ReadingPrimary,
    onPrimary = ReadingOnPrimary,
    primaryContainer = ReadingSurfaceVariant,
    onPrimaryContainer = ReadingText,
    secondary = ReadingSecondary,
    tertiary = ReadingTertiary,
    background = ReadingBackground,
    onBackground = ReadingText,
    surface = ReadingSurface,
    onSurface = ReadingText,
    surfaceVariant = ReadingSurfaceVariant,
    onSurfaceVariant = ReadingMutedText,
    error = ReadingError
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun MuteifyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    appTheme: AppTheme = if (darkTheme) AppTheme.OLED else AppTheme.DAY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.OLED -> OledColorScheme
        AppTheme.DAY -> DayColorScheme
        AppTheme.NIGHT -> NightColorScheme
        AppTheme.READING -> ReadingColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MuteifyTypography,
        content = content
    )
}
