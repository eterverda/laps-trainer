package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import ru.fpvladder.laps.trainer.ui.theme.LocalExtendedColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Results
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.components.FlightTimer
import ru.fpvladder.laps.trainer.ui.components.HoldButton
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase


private val FlightSurfaceShape = RoundedCornerShape(24.dp)

@Composable
fun FlightScreen(
    flightPhase: FlightPhase,
    startSignal: StartSignal,
    laps: List<Lap> = emptyList(),
    currentLap: Lap? = null,
    currentLapTime: Long = 0L,
    elapsedMs: Long = 0L,
    isPreBlinking: Boolean = false,
    timeLimitSeconds: Int = 0,
    maxLaps: Int = Int.MAX_VALUE,
    stopReason: StopReason? = null,
    shouldSaveResult: Boolean = false,
    onShouldSaveResultChange: (Boolean) -> Unit = {},
    rotatePilotsForNextFlight: Boolean = false,
    onRotatePilotsForNextFlightChange: (Boolean) -> Unit = {},
    useLapButton: Boolean = false,
    useErrorFixButtons: Boolean = false,
    onLapClick: () -> Unit = {},
    onErrorClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    showRecordKinds: Set<Record.Kind> = emptySet(),
    holeshotEnabled: Boolean = false,
    pilot: Pilot? = null,
    pilotChangeIndex: Int? = null,
    teamFlight: Flight.Team? = null,
    swapMode: Rules.Team.SwapMode = Rules.Team.SwapMode.STRAIGHT,
    changeRemainingMs: Long? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timerSurfaceColor = LocalExtendedColors.current.timerSurface

    Box(modifier = modifier.fillMaxSize()) {
        var timerHeight by remember { mutableIntStateOf(0) }
        var buttonsHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current

        val timerVisible = flightPhase != FlightPhase.POST_FLIGHT
        val overlapPx = with(density) { 45.dp.roundToPx() }
        val targetTopPadding = if (timerVisible) (timerHeight - overlapPx).coerceAtLeast(0) else 0
        val animatedTopPadding by animateDpAsState(
            targetValue = with(density) { targetTopPadding.toDp() },
            label = "timer_reveal"
        )

        val buttonsVisible = flightPhase == FlightPhase.FLIGHT && (useLapButton || useErrorFixButtons)
        val bottomOverlapPx = with(density) { 0.dp.roundToPx() }
        val targetBottomPadding = if (buttonsVisible) (buttonsHeight - bottomOverlapPx).coerceAtLeast(0) else 0
        val animatedBottomPadding by animateDpAsState(
            targetValue = with(density) { targetBottomPadding.toDp() },
            label = "buttons_reveal"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { if (it.height > 0) timerHeight = it.height },
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = timerVisible,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = timerSurfaceColor,
                    shape = FlightSurfaceShape
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlightTimer(
                            flightPhase = flightPhase,
                            startSignal = startSignal,
                            elapsedMs = elapsedMs,
                            isPreBlinking = isPreBlinking,
                            timerPrecision = timerPrecision,
                            timeLimitSeconds = timeLimitSeconds,
                            changeRemainingMs = changeRemainingMs
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .padding(top = animatedTopPadding, bottom = animatedBottomPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = FlightSurfaceShape
        ) {
            if (flightPhase != FlightPhase.POST_FLIGHT) {
                FlightContent(
                    flightPhase = flightPhase,
                    startSignal = startSignal,
                    laps = laps,
                    currentLap = currentLap,
                    currentLapTime = currentLapTime,
                    timerPrecision = timerPrecision,
                    holeshotEnabled = holeshotEnabled,
                    useLapButton = useLapButton,
                    onLapClick = onLapClick,
                    pilot = pilot,
                    pilotChangeIndex = pilotChangeIndex,
                    swapMode = swapMode
                )
            } else {
                PostFlightContent(
                    laps = laps,
                    currentLap = currentLap,
                    currentLapTime = currentLapTime,
                    timerPrecision = timerPrecision,
                    timeLimitSeconds = timeLimitSeconds,
                    maxLaps = maxLaps,
                    stopReason = stopReason,
                    showRecordKinds = showRecordKinds,
                    pilot = pilot,
                    pilotChangeIndex = pilotChangeIndex,
                    teamFlight = teamFlight,
                    swapMode = swapMode,
                    shouldSaveResult = shouldSaveResult,
                    onShouldSaveResultChange = onShouldSaveResultChange,
                    rotatePilotsForNextFlight = rotatePilotsForNextFlight,
                    onRotatePilotsForNextFlightChange = onRotatePilotsForNextFlightChange,
                    onBackClick = onBackClick
                )
            }
        }

        if (useLapButton || useErrorFixButtons) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { if (it.height > 0) buttonsHeight = it.height }
            ) {
                AnimatedVisibility(
                    visible = buttonsVisible,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (useLapButton) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(48.dp))
                                    HoldButton(
                                        onConfirm = onLapClick,
                                        text = "Круг",
                                        iconRes = R.drawable.ic_circle,
                                        holdDurationMs = 0,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Box(modifier = Modifier.size(48.dp))
                                }
                            }
                            if (useErrorFixButtons) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        16.dp,
                                        Alignment.CenterHorizontally
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HoldButton(
                                        onConfirm = onErrorClick,
                                        text = "ОШИБКА",
                                        iconRes = R.drawable.ic_cross,
                                        iconSize = 18.dp,
                                        textSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        contentSpacing = 4.dp,
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                        holdDurationMs = 0
                                    )
                                    HoldButton(
                                        onConfirm = onFixClick,
                                        text = "ИСПРАВИЛ",
                                        iconRes = R.drawable.ic_square,
                                        iconSize = 18.dp,
                                        textSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        contentSpacing = 4.dp,
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                        holdDurationMs = 0
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


