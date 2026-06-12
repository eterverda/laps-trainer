package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import ru.fpvladder.laps.trainer.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.computeFlightCounters
import ru.fpvladder.laps.trainer.model.computeFlightRecords
import ru.fpvladder.laps.trainer.model.computeTeamFlightRecords
import ru.fpvladder.laps.trainer.model.computeTeamFlightCounters
import ru.fpvladder.laps.trainer.model.TeamCounterScope
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.model.IMMEDIATE_START_ENABLED
import ru.fpvladder.laps.trainer.ui.components.MeasuredHorizontalPager
import ru.fpvladder.laps.trainer.ui.screens.RecordsInset
import ru.fpvladder.laps.trainer.ui.screens.InsetCard
import ru.fpvladder.laps.trainer.ui.screens.PageIndicator
import ru.fpvladder.laps.trainer.ui.screens.CountersSummary
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
    shouldSaveResult: Boolean = false,
    onShouldSaveResultChange: (Boolean) -> Unit = {},
    swapPilotsForNextFlight: Boolean = false,
    onSwapPilotsForNextFlightChange: (Boolean) -> Unit = {},
    useLapButton: Boolean = false,
    onLapClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    enabledRecordKinds: Set<Record.Kind> = emptySet(),
    holeshotEnabled: Boolean = false,
    pilot: Pilot? = null,
    pilotSwapIndex: Int? = null,
    pilotOrderSwapped: Boolean = false,
    hasPagerWiggled: Boolean = false,
    onPagerWiggleComplete: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTeam = pilot is Pilot.Team
    val (rawName1, rawName2) = when (pilot) {
        is Pilot.Team -> pilot.name1 to pilot.name2
        else -> "" to ""
    }
    val headPilot = if (pilotOrderSwapped) rawName2 else rawName1
    val tailPilot = if (pilotOrderSwapped) rawName1 else rawName2
    val context = LocalContext.current
    val mainClickModifier = if (phase == FlightPhase.MAIN && !useLapButton) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onLapClick() })
        }
    } else Modifier

    Column(
        modifier = modifier.then(mainClickModifier).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (phase == FlightPhase.MAIN || phase == FlightPhase.POST || phase == FlightPhase.PRE) {
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
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Box(modifier = Modifier.onSizeChanged { contentHeight = it.height }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (phase == FlightPhase.POST && laps.isNotEmpty()) {
                                ScreenTitle(stringResource(R.string.flight_laps_header), Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
                            }
                            LapList(
                                laps = laps,
                                currentLap = currentLap,
                                currentLapTime = currentLapTime,
                                timerPrecision = timerPrecision,
                                phase = phase,
                                pilot = pilot,
                                pilotSwapIndex = pilotSwapIndex,
                                pilotOrderSwapped = pilotOrderSwapped,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            if (phase == FlightPhase.POST) {
                                val completedText = when (stopReason) {
                                    StopReason.TIME_LIMIT -> {
                                        val minutes = timeLimitSeconds / 60.0
                                        if (minutes == minutes.toInt().toDouble()) {
                                            context.resources.getQuantityString(
                                                R.plurals.flight_completed_time_minutes,
                                                minutes.toInt(),
                                                minutes.toInt()
                                            )
                                        } else {
                                            context.getString(
                                                R.string.flight_completed_time_fraction,
                                                String.format("%.1f", minutes).replace('.', ',')
                                            )
                                        }
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                                )
                                if (isTeam) {
                                    val teamRecords = computeTeamFlightRecords(
                                        laps = laps,
                                        timerPrecision = timerPrecision,
                                        enabledKinds = enabledRecordKinds,
                                        pilotSwapIndex = pilotSwapIndex
                                    )
                                    TeamFlightPostResults(
                                        commonRecords = teamRecords.common.distinctBy { it.count },
                                        headRecords = teamRecords.head.distinctBy { it.count },
                                        tailRecords = teamRecords.tail.distinctBy { it.count },
                                        commonCounters = computeTeamFlightCounters(
                                            laps, TeamCounterScope.COMMON, pilotSwapIndex
                                        ),
                                        headCounters = computeTeamFlightCounters(
                                            laps, TeamCounterScope.HEAD, pilotSwapIndex
                                        ),
                                        tailCounters = computeTeamFlightCounters(
                                            laps, TeamCounterScope.TAIL, pilotSwapIndex
                                        ),
                                        headPilotName = headPilot,
                                        tailPilotName = tailPilot,
                                        timerPrecision = timerPrecision,
                                        hasPagerWiggled = hasPagerWiggled,
                                        onPagerWiggleComplete = onPagerWiggleComplete
                                    )
                                } else {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        ScreenTitle("Результаты")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val flightRecords = if (enabledRecordKinds.isNotEmpty()) {
                                            computeFlightRecords(laps, timerPrecision, enabledRecordKinds)
                                        } else emptyList()
                                        RecordsInset(
                                            records = flightRecords.distinctBy { it.count },
                                            timerPrecision = timerPrecision
                                        )
                                        if (flightRecords.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        CountersSummary(counters = computeFlightCounters(laps))
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { buttonsHeight = it.height },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = shouldSaveResult,
                                        onValueChange = onShouldSaveResultChange,
                                        role = Role.Checkbox
                                    )
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = shouldSaveResult,
                                    onCheckedChange = null
                                )
                                Text(
                                    text = "Сохранить результат",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isTeam) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = swapPilotsForNextFlight,
                                            onValueChange = onSwapPilotsForNextFlightChange,
                                            role = Role.Checkbox
                                        )
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = swapPilotsForNextFlight,
                                        onCheckedChange = null
                                    )
                                    Text(
                                        text = stringResource(R.string.swap_pilots_for_next_flight),
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (IMMEDIATE_START_ENABLED) {
                                TextButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RectangleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Назад",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            val showTapHint = (phase == FlightPhase.MAIN || phase == FlightPhase.PRE) && laps.size < 3 && !useLapButton
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTapHint,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1200)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 1200))
                ) {
                    Hint(
                        text = if (holeshotEnabled) {
                            "Нажимайте на экран каждый раз, когда пройдены стартовые ворота"
                        } else {
                            "Нажимайте на экран каждый раз, когда завершен круг"
                        }
                    )
                }
            }
            if (phase == FlightPhase.PRE && startSignal == StartSignal.MANUAL) {
                Hint(
                    text = "Нажмите GO чтобы начать вылет",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    paddingValues = PaddingValues(bottom = 16.dp)
                )
            }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

    }
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(vertical = 48.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .width(240.dp)
            .padding(paddingValues)
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
    phase: FlightPhase,
    pilot: Pilot? = null,
    pilotSwapIndex: Int? = null,
    pilotOrderSwapped: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isTeam = pilot is Pilot.Team
    val (rawName1, rawName2) = when (pilot) {
        is Pilot.Team -> pilot.name1 to pilot.name2
        is Pilot.Individual -> pilot.name to ""
        else -> "" to ""
    }
    val headPilot = if (pilotOrderSwapped) rawName2 else rawName1
    val tailPilot = if (pilotOrderSwapped) rawName1 else rawName2
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
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        if (isTeam) {
            val startingPilot = headPilot
            if (startingPilot.isNotBlank()) {
                Text(
                    text = "Стартует $startingPilot",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        laps.forEachIndexed { index, lap ->
            val isFailed = lap.status == Lap.Status.FAIL
            LapRow(
                label = if (isFailed) "" else lap.label,
                time = formatTimeDynamic(lap.timeMs, timerPrecision),
                isFailed = isFailed,
                isCurrentFail = false,
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
            if (isTeam && pilotSwapIndex != null && index == pilotSwapIndex) {
                val nextPilot = tailPilot
                if (nextPilot.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Смена пилота. Стартует $nextPilot",
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        currentVisible?.let { lap ->
            val isCurrentFail = lap.status == Lap.Status.FAIL
            LapRow(
                label = lap.label,
                time = formatTimeDynamic(currentLapTime, timerPrecision),
                isFailed = false,
                isCurrentFail = isCurrentFail,
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
        }
        if (isTeam && pilotSwapIndex != null && pilotSwapIndex == laps.size) {
            val nextPilot = tailPilot
            if (nextPilot.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Смена пилота. Стартует $nextPilot",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeamFlightPostResults(
    commonRecords: List<Record>,
    headRecords: List<Record>,
    tailRecords: List<Record>,
    commonCounters: List<Counter>,
    headCounters: List<Counter>,
    tailCounters: List<Counter>,
    headPilotName: String,
    tailPilotName: String,
    timerPrecision: TimerPrecision,
    hasPagerWiggled: Boolean = false,
    onPagerWiggleComplete: () -> Unit = {}
) {
    val pages: List<Pair<String, @Composable () -> Unit>> = listOf(
        "Результаты" to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ScreenTitle("Результаты")
                Spacer(modifier = Modifier.height(8.dp))
                if (commonRecords.isNotEmpty()) {
                    RecordsInset(records = commonRecords, timerPrecision = timerPrecision)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(counters = commonCounters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        "Результаты: $headPilotName" to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ScreenTitle("Результаты: $headPilotName")
                Spacer(modifier = Modifier.height(8.dp))
                if (headRecords.isNotEmpty()) {
                    RecordsInset(records = headRecords, timerPrecision = timerPrecision)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(counters = headCounters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        "Результаты: $tailPilotName" to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ScreenTitle("Результаты: $tailPilotName")
                Spacer(modifier = Modifier.height(8.dp))
                if (tailRecords.isNotEmpty()) {
                    RecordsInset(records = tailRecords, timerPrecision = timerPrecision)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(counters = tailCounters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    PageIndicator(
        pageCount = pages.size,
        currentPage = pagerState.currentPage,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    MeasuredHorizontalPager(
        state = pagerState,
        pageCount = pages.size,
        modifier = Modifier.fillMaxWidth(),
        wiggleOnAppear = !hasPagerWiggled,
        onWiggleComplete = onPagerWiggleComplete
    ) { page ->
        pages[page].second()
    }
}
