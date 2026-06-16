package ru.fpvladder.laps.trainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import ru.fpvladder.laps.trainer.settings.AppThemeMode
import ru.fpvladder.laps.trainer.settings.DarkThemeVariant

@Composable
fun LapsTrainerTheme(
    appTheme: AppThemeMode = AppThemeMode.SYSTEM,
    darkThemeVariant: DarkThemeVariant = DarkThemeVariant.CATPUCCIN_MOCHA,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (isDark) {
        when (darkThemeVariant) {
            DarkThemeVariant.CATPUCCIN_FRAPPE -> CatppuccinFrappe
            DarkThemeVariant.CATPUCCIN_MACCHIATO -> CatppuccinMacchiato
            DarkThemeVariant.CATPUCCIN_MOCHA -> CatppuccinMocha
        }
    } else {
        CatppuccinLatte
    }
    val colorScheme = palette.toColorScheme()
    val extendedColors = ExtendedColors(
        timerSurface = palette.green,
        timerOnSurface = palette.crust,
        selectableSurface = palette.surface1,
        selectableSelectedText = palette.teal
    )

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
