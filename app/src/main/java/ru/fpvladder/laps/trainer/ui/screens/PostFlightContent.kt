package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.IMMEDIATE_START_ENABLED
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TeamCounterScope
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.model.computeFlightCounters
import ru.fpvladder.laps.trainer.model.computeFlightRecords
import ru.fpvladder.laps.trainer.model.computeTeamFlightCounters
import ru.fpvladder.laps.trainer.model.computeTeamFlightRecords
import ru.fpvladder.laps.trainer.ui.components.LapList
import ru.fpvladder.laps.trainer.ui.components.MeasuredHorizontalPager
import ru.fpvladder.laps.trainer.ui.components.ScreenTitle

private val PostFlightSurfaceShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

@Composable
fun PostFlightContent(
    laps: List<Lap>,
    currentLap: Lap?,
    currentLapTime: Long,
    timerPrecision: TimerPrecision,
    timeLimitSeconds: Int,
    maxLaps: Int,
    stopReason: StopReason?,
    enabledRecordKinds: Set<Record.Kind>,
    pilot: Pilot?,
    pilotSwapIndex: Int?,
    pilotOrderSwapped: Boolean,
    shouldSaveResult: Boolean,
    onShouldSaveResultChange: (Boolean) -> Unit,
    swapPilotsForNextFlight: Boolean,
    onSwapPilotsForNextFlightChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    hasPagerWiggled: Boolean,
    onPagerWiggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTeam = pilot is Pilot.Team
    val (rawName1, rawName2) = when (pilot) {
        is Pilot.Team -> pilot.name1 to pilot.name2
        else -> "" to ""
    }
    val headPilot = if (pilotOrderSwapped) rawName2 else rawName1
    val tailPilot = if (pilotOrderSwapped) rawName1 else rawName2

    val scrollState = rememberScrollState()
    LaunchedEffect(laps.size) {
        delay(50)
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    var boxHeight by remember { mutableIntStateOf(0) }
    var contentHeight by remember { mutableIntStateOf(0) }
    var buttonsHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gapPx = with(density) { 16.dp.roundToPx() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        shape = PostFlightSurfaceShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boxHeight = it.height }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp)
            ) {
                Box(modifier = Modifier.onSizeChanged { contentHeight = it.height }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ScreenTitle(
                            stringResource(R.string.flight_laps_header),
                            Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp)
                        )
                        LapList(
                            laps = laps,
                            currentLap = currentLap,
                            currentLapTime = currentLapTime,
                            timerPrecision = timerPrecision,
                            isPostFlight = true,
                            pilot = pilot,
                            pilotSwapIndex = pilotSwapIndex,
                            pilotOrderSwapped = pilotOrderSwapped,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
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
                                val lapsStr = context.resources.getQuantityString(
                                    R.plurals.laps,
                                    maxLaps,
                                    maxLaps
                                )
                                stringResource(R.string.flight_completed_laps, lapsStr)
                            }

                            else -> stringResource(R.string.flight_completed_manual)
                        }
                        Text(
                            text = completedText,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(top = if (laps.isNotEmpty()) 16.dp else 0.dp, bottom = 16.dp)
                        )
                        if (isTeam) {
                            val teamRecords = computeTeamFlightRecords(
                                laps = laps,
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
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                ScreenTitle("Результаты")
                                Spacer(modifier = Modifier.height(8.dp))
                                val flightRecords = if (enabledRecordKinds.isNotEmpty()) {
                                    computeFlightRecords(laps, enabledRecordKinds)
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
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
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
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            ),
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
                    .padding(horizontal = 24.dp)
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
                    .padding(horizontal = 24.dp)
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
                    .padding(horizontal = 24.dp)
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
