package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeasuredHorizontalPager(
    state: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit
) {
    val density = LocalDensity.current
    var maxHeightPx by remember { mutableIntStateOf(0) }
    val fallbackHeightPx = remember(density) { with(density) { 200.dp.roundToPx() } }

    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            // Measurement slots: render each page off-screen to find the tallest one.
            repeat(pageCount) { page ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    pageContent(page)
                }
            }
            // Actual pager.
            HorizontalPager(
                state = state,
                beyondViewportPageCount = pageCount,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    pageContent(page)
                }
            }
        }
    ) { measurables, constraints ->
        val pageMeasurables = measurables.take(pageCount)
        val pagePlaceables = pageMeasurables.map { measurable ->
            measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
        }
        val measuredMax = pagePlaceables.maxOfOrNull { it.height } ?: 0
        if (measuredMax != maxHeightPx) {
            maxHeightPx = measuredMax
        }

        val pagerMeasurable = measurables.last()
        val targetHeight = if (measuredMax > 0) measuredMax else fallbackHeightPx
        val pagerPlaceable = pagerMeasurable.measure(
            constraints.copy(minHeight = targetHeight, maxHeight = targetHeight)
        )

        layout(pagerPlaceable.width, pagerPlaceable.height) {
            pagerPlaceable.placeRelative(0, 0)
        }
    }
}
