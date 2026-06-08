package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.model.TimerPrecision

private const val MIN_TIMER_UPDATE_INTERVAL = 89L

@Composable
fun FlightContent(
    onNavigateBack: () -> Unit,
    timerPrecision: TimerPrecision,
    modifier: Modifier = Modifier
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(timerPrecision) {
        val startTime = System.currentTimeMillis()
        while (true) {
            elapsedMs = System.currentTimeMillis() - startTime
            delay(timerPrecision.tickMs.coerceAtLeast(MIN_TIMER_UPDATE_INTERVAL))
        }
    }

    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = elapsedMs % 1000

    val timeText = when (timerPrecision.fractionDigits) {
        0 -> String.format("%02d:%02d", minutes, seconds)
        1 -> String.format("%02d:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("%02d:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("%02d:%02d.%03d", minutes, seconds, ms)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = timeText,
            fontFamily = FontFamily.Monospace,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}
