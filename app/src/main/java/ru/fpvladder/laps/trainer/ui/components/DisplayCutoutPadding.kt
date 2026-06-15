package ru.fpvladder.laps.trainer.ui.components

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat

fun Modifier.displayCutoutPaddingFromWindow(): Modifier = composed {
    val density = LocalDensity.current
    val context = LocalContext.current
    val windowManager = remember(context) {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    val metrics = remember(windowManager) { windowManager.currentWindowMetrics }
    val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(metrics.windowInsets)
    val cutoutInsets = insetsCompat.getInsetsIgnoringVisibility(
        WindowInsetsCompat.Type.displayCutout()
    )
    val left = with(density) { cutoutInsets.left.toDp() }
    val right = with(density) { cutoutInsets.right.toDp() }
    padding(start = left, end = right)
}
