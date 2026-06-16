package ru.fpvladder.laps.trainer.ui.components

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat

fun Modifier.displayCutoutPaddingFromWindow(): Modifier = composed {
    @Suppress("UNUSED_EXPRESSION")
    LocalConfiguration.current // force recomposition on rotation/config changes

    val density = LocalDensity.current
    val context = LocalContext.current
    val view = LocalView.current

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = windowManager.currentWindowMetrics
    val displayInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(metrics.windowInsets)
    val displayCutout = displayInsetsCompat.getInsetsIgnoringVisibility(
        WindowInsetsCompat.Type.displayCutout()
    )

    val location = remember { IntArray(2) }
    view.getLocationOnScreen(location)
    val dialogLeftPx = location[0]
    val dialogRightPx = metrics.bounds.width() - (dialogLeftPx + view.width)

    val displayLeft = with(density) { displayCutout.left.toDp() }
    val displayRight = with(density) { displayCutout.right.toDp() }
    val dialogLeft = with(density) { dialogLeftPx.toDp() }
    val dialogRight = with(density) { dialogRightPx.toDp() }

    val start = (displayLeft - dialogLeft).coerceAtLeast(0.dp)
    val end = (displayRight - dialogRight).coerceAtLeast(0.dp)

    padding(start = start, end = end)
}
