package ru.fpvladder.laps.trainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import ru.fpvladder.laps.trainer.settings.CatppuccinTheme

@Composable
fun LapsTrainerTheme(
    appTheme: CatppuccinTheme = CatppuccinTheme.MOCHA,
    content: @Composable () -> Unit
) {
    val palette = when (appTheme) {
        CatppuccinTheme.LATTE -> CatppuccinLatte
        CatppuccinTheme.FRAPPE -> CatppuccinFrappe
        CatppuccinTheme.MACCHIATO -> CatppuccinMacchiato
        CatppuccinTheme.MOCHA -> CatppuccinMocha
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
