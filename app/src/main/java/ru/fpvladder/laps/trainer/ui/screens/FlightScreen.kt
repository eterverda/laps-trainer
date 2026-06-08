package ru.fpvladder.laps.trainer.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.audio.BUZZER_DURATION_MS
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase

@Composable
fun FlightContent(
    phase: FlightPhase,
    startSignal: StartSignal,
    elapsedMs: Long,
    preStartTime: Long,
    isPreBlinking: Boolean,
    isMuted: Boolean,
    timerPrecision: TimerPrecision,
    onSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var countdownMs by remember { mutableLongStateOf(1500L) }
    LaunchedEffect(preStartTime, phase, startSignal) {
        if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) {
            while (true) {
                val passed = SystemClock.elapsedRealtime() - preStartTime
                countdownMs = (1500 + if (isMuted) 0 else BUZZER_DURATION_MS - passed).coerceAtLeast(0)
                if (countdownMs <= 0) break
                delay(16)
            }
        }
    }

    val isBlinking = phase == FlightPhase.PRE && isPreBlinking
    val alpha by animateFloatAsState(
        targetValue = if (isBlinking) 0f else 1f,
        animationSpec = tween(200),
        label = "timer_blink"
    )

    val timeText = if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) {
        formatCountdown(countdownMs, timerPrecision)
    } else {
        formatTime(if (phase == FlightPhase.PRE) 0L else elapsedMs, timerPrecision)
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
            modifier = Modifier
                .padding(top = 32.dp)
                .alpha(if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) 1f else alpha)
        )

        if (phase == FlightPhase.PRE && startSignal == StartSignal.MANUAL) {
            Text(
                text = "Нажмите GO чтобы начать вылет",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        if (phase == FlightPhase.POST) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "УДАЛИТЬ",
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "СОХРАНИТЬ",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(elapsedMs: Long, timerPrecision: TimerPrecision): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = elapsedMs % 1000

    return when (timerPrecision.fractionDigits) {
        0 -> String.format("%02d:%02d", minutes, seconds)
        1 -> String.format("%02d:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("%02d:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("%02d:%02d.%03d", minutes, seconds, ms)
    }
}

private fun formatCountdown(remainingMs: Long, timerPrecision: TimerPrecision): String {
    val countdown = when (timerPrecision.fractionDigits) {
        0 -> "-${remainingMs / 1000}"
        1 -> String.format("-%.1f", remainingMs / 1000.0)
        2 -> String.format("-%.2f", remainingMs / 1000.0)
        else -> String.format("-%.3f", remainingMs / 1000.0)
    }

    val targetWidth = when (timerPrecision.fractionDigits) {
        0 -> 5
        1 -> 7
        2 -> 8
        else -> 9
    }

    return countdown.padStart(targetWidth, ' ')
}
