package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A surface with a "double bottom" look: a primary upper sheet with rounded corners
 * and a secondary sheet peeking out from underneath by [peekHeight].
 *
 * The lower sheet is intentionally taller than the visible peek area; its top part
 * is hidden under the upper sheet, creating the "double bottom" overlap effect.
 *
 * @param upperContent Main content placed on the upper sheet.
 * @param lowerContent Actions placed on the lower (peek-through) sheet.
 */
@Composable
fun DoubleBottomSurface(
    modifier: Modifier = Modifier,
    peekHeight: Dp = 80.dp,
    upperContentPadding: Dp = 20.dp,
    lowerPaddingTop: Dp = 72.dp,
    lowerPaddingBottom: Dp = 16.dp,
    scrollable: Boolean = false,
    upperContent: @Composable () -> Unit,
    lowerContent: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val upperColor = if (isLight) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val lowerColor = if (isLight) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val scrollModifier = if (scrollable) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(scrollModifier)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = shape,
            color = lowerColor
        ) {
            Box(
                modifier = Modifier.padding(
                    top = lowerPaddingTop,
                    bottom = lowerPaddingBottom
                )
            ) {
                lowerContent()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(bottom = peekHeight),
            shape = shape,
            color = upperColor
        ) {
            Box(modifier = Modifier.padding(upperContentPadding)) {
                upperContent()
            }
        }
    }
}
