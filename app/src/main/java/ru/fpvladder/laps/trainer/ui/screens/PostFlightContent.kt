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
import androidx.compose.material3.CheckboxDefaults
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
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.viewmodel.LapMark
import ru.fpvladder.laps.trainer.model.NO_PILOT_CHANGE
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight
import ru.fpvladder.laps.trainer.ui.helpers.displayNameHead
import ru.fpvladder.laps.trainer.ui.helpers.displayNameTail
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
    showRecordKinds: Set<Record.Kind>,
    pilot: Pilot?,
    pilotChangeIndex: Int = NO_PILOT_CHANGE,
    lapMarks: Map<Int, List<LapMark>> = emptyMap(),
    teamFlight: Flight.Team? = null,
    swapMode: Rules.Team.SwapMode,
    shouldSaveResult: Boolean,
    onShouldSaveResultChange: (Boolean) -> Unit,
    rotatePilotsForNextFlight: Boolean,
    onRotatePilotsForNextFlightChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                            lapMarks = lapMarks,
                            pilot = pilot,
                            pilotChangeIndex = pilotChangeIndex,
                            swapMode = swapMode,
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
                            StopReason.LAPS_LIMIT -> {
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
                        if (pilot is Pilot.Team) {
                            val teamResults = teamFlight
                                ?: computeTeamFlight(
                                    laps,
                                    stopReason ?: StopReason.MANUAL,
                                    pilotChangeIndex,
                                    swapMode
                                )
                            val headPilot = pilot.displayNameHead(swapMode, context)
                            val tailPilot = pilot.displayNameTail(swapMode, context)
                            TeamFlightPostResults(
                                commonRecords = teamResults.results.records.filter { it.kind in showRecordKinds }.distinctBy { it.count },
                                headRecords = teamResults.headResults.records.filter { it.kind in showRecordKinds }.distinctBy { it.count },
                                tailRecords = teamResults.tailResults.records.filter { it.kind in showRecordKinds }.distinctBy { it.count },
                                commonCounters = teamResults.results.counters,
                                headCounters = teamResults.headResults.counters,
                                tailCounters = teamResults.tailResults.counters,
                                headPilotName = headPilot,
                                tailPilotName = tailPilot,
                                timerPrecision = timerPrecision
                            )
                        } else {
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                ScreenTitle("Результаты")
                                Spacer(modifier = Modifier.height(8.dp))
                                val result = computeIndividualFlight(laps, stopReason ?: StopReason.MANUAL).results
                                val flightRecords = result.records.filter { it.kind in showRecordKinds }
                                RecordsInset(
                                    records = flightRecords.distinctBy { it.count },
                                    timerPrecision = timerPrecision
                                )
                                if (flightRecords.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                CountersSummary(counters = result.counters)
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
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Сохранить результат",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (pilot is Pilot.Team) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = rotatePilotsForNextFlight,
                                    onValueChange = onRotatePilotsForNextFlightChange,
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
                                checked = rotatePilotsForNextFlight,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = stringResource(R.string.rotate_pilots_for_next_flight),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
    timerPrecision: TimerPrecision
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
            .padding(bottom = 12.dp)
    )

    MeasuredHorizontalPager(
        state = pagerState,
        pageCount = pages.size,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        pages[page].second()
    }
}
