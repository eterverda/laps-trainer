package ru.fpvladder.laps.trainer.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.res.painterResource
import ru.fpvladder.laps.trainer.R
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.audio.BUZZER_DURATION_MS
import ru.fpvladder.laps.trainer.model.LapEntry
import ru.fpvladder.laps.trainer.model.LapIcon
import ru.fpvladder.laps.trainer.model.LapStatus
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
    laps: List<LapEntry> = emptyList(),
    currentLapTime: Long = 0L,
    onSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onLapClick: () -> Unit = {},
    onError: () -> Unit = {},
    onFix: () -> Unit = {},
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

    val mainClickModifier = if (phase == FlightPhase.MAIN) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onLapClick() })
        }
    } else Modifier

    Column(
        modifier = modifier.then(mainClickModifier).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeText,
            fontFamily = FontFamily.Monospace,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 16.dp)
                .alpha(if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) 1f else alpha)
        )

        if (phase == FlightPhase.MAIN || phase == FlightPhase.POST) {
            val scrollState = rememberScrollState()
            LaunchedEffect(laps.size) {
                delay(50)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(all = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                LapList(laps = laps, currentLapTime = currentLapTime, timerPrecision = timerPrecision)
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        when {
            phase == FlightPhase.PRE && startSignal == StartSignal.MANUAL -> {
                Text(
                    text = "Нажмите GO чтобы начать вылет",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            phase == FlightPhase.MAIN -> {
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onError,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ОШИБКА",
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = onFix,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_square),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ИСПРАВИЛ",
                            fontSize = 14.sp
                        )
                    }
                }
            }
            phase == FlightPhase.POST -> {
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
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
}

@Composable
private fun LapList(
    laps: List<LapEntry>,
    currentLapTime: Long,
    timerPrecision: TimerPrecision
) {
    data class Item(val label: String, val time: String, val icons: List<LapIcon>, val isFailed: Boolean)

    val items = remember(laps, currentLapTime, timerPrecision) {
        laps.mapIndexed { index, lap ->
            val isCurrent = index == laps.lastIndex && !lap.icons.contains(LapIcon.LAP)
            val time = if (isCurrent) currentLapTime else lap.timeMs
            val isFailed = lap.status == LapStatus.FAIL
            Item(
                label = if (isFailed) "" else lap.lapLabel,
                time = formatTimeDynamic(time, timerPrecision),
                icons = lap.icons,
                isFailed = isFailed
            )
        }
    }

    val maxLabelLen = items.filter { !it.isFailed }.maxOfOrNull { it.label.length } ?: 0
    val maxTimeLen = items.maxOfOrNull { it.time.length } ?: 0

    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.label.padStart(maxLabelLen)} ${item.time.padStart(maxTimeLen)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isFailed) TextDecoration.LineThrough else null
                )
                if (item.icons.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.icons.forEach { icon ->
                            when (icon) {
                                LapIcon.ERROR -> Icon(
                                    painter = painterResource(R.drawable.ic_cross),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                LapIcon.FIX -> Icon(
                                    painter = painterResource(R.drawable.ic_square),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                LapIcon.LAP -> Icon(
                                    painter = painterResource(R.drawable.ic_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeDynamic(elapsedMs: Long, timerPrecision: TimerPrecision): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = elapsedMs % 1000

    return when (timerPrecision.fractionDigits) {
        0 -> {
            if (minutes > 0) String.format("%d:%02d", minutes, seconds)
            else "$seconds"
        }
        1 -> {
            val frac = ms / 100
            if (minutes > 0) String.format("%d:%02d.%01d", minutes, seconds, frac)
            else String.format("%d.%01d", seconds, frac)
        }
        2 -> {
            val frac = ms / 10
            if (minutes > 0) String.format("%d:%02d.%02d", minutes, seconds, frac)
            else String.format("%d.%02d", seconds, frac)
        }
        else -> {
            if (minutes > 0) String.format("%d:%02d.%03d", minutes, seconds, ms)
            else String.format("%d.%03d", seconds, ms)
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
