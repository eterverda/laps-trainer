package ru.fpvladder.laps.trainer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom colors that are not part of the Material3 [ColorScheme]
 * but are still derived from the active Catppuccin palette.
 */
@Immutable
data class ExtendedColors(
    val timerSurface: Color = Color.Unspecified,
    val timerOnSurface: Color = Color.Unspecified,
    val selectableSurface: Color = Color.Unspecified,
    val selectableSelectedText: Color = Color.Unspecified
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
