package ru.fpvladder.laps.trainer.ui.screens

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
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
    laps: List<LapEntry> = emptyList(),
    currentLapTime: Long = 0L,
    elapsedMs: Long = 0L,
    timeLimitSeconds: Int = 0,
    onSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onLapClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    modifier: Modifier = Modifier
) {
    val mainClickModifier = if (phase == FlightPhase.MAIN) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onLapClick() })
        }
    } else Modifier

    Column(
        modifier = modifier.then(mainClickModifier).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (phase == FlightPhase.MAIN || phase == FlightPhase.POST) {
            val scrollState = rememberScrollState()
            LaunchedEffect(laps.size, phase) {
                delay(50)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            var boxHeight by remember { mutableIntStateOf(0) }
            var contentHeight by remember { mutableIntStateOf(0) }
            var buttonsHeight by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val gapPx = with(density) { 16.dp.roundToPx() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { boxHeight = it.height }
                    .verticalScroll(scrollState)
                    .padding(all = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.onSizeChanged { contentHeight = it.height }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LapList(laps = laps, currentLapTime = currentLapTime, timerPrecision = timerPrecision)
                            if (phase == FlightPhase.POST) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Вылет завершен. Общее время ")
                                        withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                                            append(formatTime(elapsedMs, timerPrecision, timeLimitSeconds))
                                        }
                                    },
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    }
                    if (phase == FlightPhase.POST) {
                        val spacerHeight = with(density) {
                            val paddingPx = 16.dp.roundToPx()
                            val remaining = boxHeight - contentHeight - buttonsHeight - gapPx - paddingPx
                            if (remaining > 0 && contentHeight > 0 && buttonsHeight > 0) remaining.toDp() else 16.dp
                        }
                        Spacer(modifier = Modifier.height(spacerHeight))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { buttonsHeight = it.height },
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = onDiscard,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Удалить",
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = onSave,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Сохранить",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (phase == FlightPhase.PRE && startSignal == StartSignal.MANUAL) {
            Text(
                text = "Нажмите GO чтобы начать вылет",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
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

