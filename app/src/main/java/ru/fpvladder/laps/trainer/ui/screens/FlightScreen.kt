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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.audio.BUZZER_DURATION_MS
import ru.fpvladder.laps.trainer.model.BestLap
import ru.fpvladder.laps.trainer.model.computeFlightRecords
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Lap.Status
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.model.formatMinutesString
import ru.fpvladder.laps.trainer.ui.components.BulletText
import ru.fpvladder.laps.trainer.ui.screens.BestLapsInset
import ru.fpvladder.laps.trainer.ui.screens.InsetCard
import ru.fpvladder.laps.trainer.ui.components.ScreenTitle
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase

@Composable
fun FlightContent(
    phase: FlightPhase,
    startSignal: StartSignal,
    laps: List<Lap> = emptyList(),
    currentLap: Lap? = null,
    currentLapTime: Long = 0L,
    elapsedMs: Long = 0L,
    timeLimitSeconds: Int = 0,
    maxLaps: Int = Int.MAX_VALUE,
    stopReason: StopReason? = null,
    onSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onLapClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    enabledBestLapKinds: Set<BestLap.Kind> = emptySet(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                            if (phase == FlightPhase.POST && laps.isNotEmpty()) {
                                ScreenTitle(stringResource(R.string.flight_laps_header), Modifier.padding(bottom = 16.dp))
                            }
                            LapList(
                                laps = laps,
                                currentLap = currentLap,
                                currentLapTime = currentLapTime,
                                timerPrecision = timerPrecision,
                                phase = phase
                            )
                            if (phase == FlightPhase.MAIN && laps.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Hint("Нажмите здесь когда пилот пройдет ворота")
                                }
                            }
                            if (phase == FlightPhase.POST) {
                                val completedText = when (stopReason) {
                                    StopReason.TIME_LIMIT -> {
                                        val minsStr = formatMinutesString(context, timeLimitSeconds / 60.0)
                                        stringResource(R.string.flight_completed_time, minsStr)
                                    }
                                    StopReason.MAX_LAPS -> {
                                        val lapsStr = context.resources.getQuantityString(R.plurals.laps, maxLaps, maxLaps)
                                        stringResource(R.string.flight_completed_laps, lapsStr)
                                    }
                                    else -> stringResource(R.string.flight_completed_manual)
                                }
                                Text(
                                    text = completedText,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                val flightRecords = if (enabledBestLapKinds.isNotEmpty()) {
                                    computeFlightRecords(laps, timerPrecision, enabledBestLapKinds)
                                } else emptyList()

                                val validLapCount = laps.count { it.status == Lap.Status.SUCCESS }
                                if (validLapCount > 0) {
                                    ScreenTitle(
                                        text = "Результаты",
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                        BestLapsInset(
                                            bestLaps = flightRecords,
                                            timerPrecision = timerPrecision
                                        )
                                        BulletText(
                                            text = context.resources.getQuantityString(
                                                R.plurals.laps,
                                                validLapCount,
                                                validLapCount
                                            ),
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                }
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Hint("Нажмите GO чтобы начать вылет")
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(240.dp)
            .padding(vertical = 48.dp)
            .alpha(0.5f)
    )
}

@Composable
private fun Summary(
    laps: List<Lap>,
    timerPrecision: TimerPrecision
) {
    val items = remember(laps, timerPrecision) {
        buildSummary(laps, timerPrecision)
    }

    if (items.isNotEmpty()) {
        val maxLabelLen = items.maxOfOrNull { it.first.length } ?: 0
        val maxTimeLen = items.maxOfOrNull {
            it.second?.let { t -> formatTimeDynamic(t, timerPrecision).length } ?: 0
        } ?: 0

        InsetCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                items.forEach { (label, timeMs) ->
                    val timeStr = timeMs?.let { formatTimeDynamic(it, timerPrecision) } ?: "--:--"
                    Text(
                        text = "${label.padStart(maxLabelLen)} ${timeStr.padStart(maxTimeLen)}".trimEnd(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun buildSummary(laps: List<Lap>, timerPrecision: TimerPrecision): List<Pair<String, Long?>> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val n = validLaps.size

    if (n == 0) return listOf("0/" to null)

    val bestTime = validLaps.minOf { timerPrecision.roundMs(it.timeMs) }
    val totalTime = validLaps.sumOf { timerPrecision.roundMs(it.timeMs) as Long }

    if (n == 1) {
        return listOf("1/" to bestTime)
    }

    if (n == 2) {
        return listOf(
            "1/" to bestTime,
            "2/" to totalTime
        )
    }

    if (n == 3) {
        return listOf(
            "1/" to bestTime,
            "3/" to totalTime
        )
    }

    var minSum3 = Long.MAX_VALUE
    for (i in 0..validLaps.size - 3) {
        val sum = timerPrecision.roundMs(validLaps[i].timeMs) +
                  timerPrecision.roundMs(validLaps[i + 1].timeMs) +
                  timerPrecision.roundMs(validLaps[i + 2].timeMs)
        if (sum < minSum3) minSum3 = sum
    }

    return listOf(
        "1/" to bestTime,
        "3/" to minSum3,
        "$n/" to totalTime
    )
}

@Composable
private fun LapList(
    laps: List<Lap>,
    currentLap: Lap?,
    currentLapTime: Long,
    timerPrecision: TimerPrecision,
    phase: FlightPhase
) {
    @Composable
    fun LapRow(
        label: String,
        time: String,
        isFailed: Boolean,
        isCurrentFail: Boolean,
        maxLabelLen: Int,
        maxTimeLen: Int
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${label.padStart(maxLabelLen)} ${time.padStart(maxTimeLen)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isFailed) TextDecoration.LineThrough else null
            )
            if (isFailed || isCurrentFail) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_cross),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }

    val currentVisible = currentLap.takeIf { phase != FlightPhase.POST }

    val maxLabelLen = remember(laps, currentVisible) {
        val candidates = laps.map { if (it.status == Lap.Status.FAIL) "" else it.label } +
                listOfNotNull(currentVisible?.let { if (it.status == Lap.Status.FAIL) "" else it.label })
        candidates.maxOfOrNull { it.length } ?: 0
    }

    val allTimes = remember(laps, currentVisible, currentLapTime, timerPrecision) {
        laps.map { formatTimeDynamic(it.timeMs, timerPrecision) } +
                listOfNotNull(currentVisible?.let { formatTimeDynamic(currentLapTime, timerPrecision) })
    }
    val maxTimeLen = allTimes.maxOfOrNull { it.length } ?: 0

    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        laps.forEach { lap ->
            val isFailed = lap.status == Lap.Status.FAIL
            LapRow(
                label = if (isFailed) "" else lap.label,
                time = formatTimeDynamic(lap.timeMs, timerPrecision),
                isFailed = isFailed,
                isCurrentFail = false,
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
        }
        currentVisible?.let { lap ->
            val isCurrentFail = lap.status == Lap.Status.FAIL
            LapRow(
                label = if (isCurrentFail) "" else lap.label,
                time = formatTimeDynamic(currentLapTime, timerPrecision),
                isFailed = false,
                isCurrentFail = isCurrentFail,
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
        }
    }
}


