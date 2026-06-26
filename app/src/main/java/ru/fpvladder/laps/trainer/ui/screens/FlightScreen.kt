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
import ru.fpvladder.laps.trainer.model.NO_PILOT_CHANGE
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.components.FlightTimer
import ru.fpvladder.laps.trainer.ui.components.HoldButton
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase

import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect


private val FlightSurfaceCornerRadius = 24.dp
private val FlightSurfaceInset = 4.dp
private val FlightSurfaceShape = RoundedCornerShape(FlightSurfaceCornerRadius)

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
    lapButtonPressed: StateFlow<Boolean>? = null,
    errorButtonPressed: StateFlow<Boolean>? = null,
    fixButtonPressed: StateFlow<Boolean>? = null,
    onLapClick: () -> Unit = {},
    onErrorClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    showRecordKinds: Set<Record.Kind> = emptySet(),
    holeshotEnabled: Boolean = false,
    pilot: Pilot? = null,
    pilotChangeIndex: Int = NO_PILOT_CHANGE,
    teamFlight: Flight.Team? = null,
    swapMode: Rules.Team.SwapMode = Rules.Team.SwapMode.STRAIGHT,
    changeRemainingMs: Long? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timerSurfaceColor = LocalExtendedColors.current.timerSurface

    Box(modifier = modifier.fillMaxSize()) {
        var buttonsHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current

        val timerVisible = flightPhase == FlightPhase.PRE_FLIGHT || flightPhase == FlightPhase.FLIGHT
        val targetTopPadding = if (timerVisible) with(density) { 74.sp.roundToPx() + 36.dp.roundToPx() } else 0
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

        val isPostFlight = flightPhase == FlightPhase.POST_FLIGHT
        val contentClipInset by animateDpAsState(
            targetValue = if (isPostFlight) 0.dp else FlightSurfaceInset,
            label = "contentClipInset"
        )
        val contentClipRadius by animateDpAsState(
            targetValue = if (isPostFlight) FlightSurfaceCornerRadius else FlightSurfaceCornerRadius - FlightSurfaceInset,
            label = "contentClipRadius"
        )
        val contentShape = remember(contentClipInset, contentClipRadius, density) {
            GenericShape { size, _ ->
                val insetPx = with(density) { contentClipInset.toPx() }
                val radiusPx = with(density) { contentClipRadius.toPx() }.coerceAtLeast(0f)
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = insetPx,
                            top = insetPx,
                            right = size.width - insetPx,
                            bottom = size.height - insetPx
                        ),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                )
            }
        }

        if (timerVisible) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = animatedBottomPadding),
                color = timerSurfaceColor,
                shape = FlightSurfaceShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                    contentAlignment = Alignment.TopCenter
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

        Surface(
            modifier = Modifier
                .padding(top = animatedTopPadding, bottom = animatedBottomPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = contentShape
        ) {
            val contentModifier = Modifier.clip(contentShape)

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
                    swapMode = swapMode,
                    modifier = contentModifier
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
                    onBackClick = onBackClick,
                    modifier = contentModifier
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
                                    Box(modifier = Modifier.size(56.dp))
                                    HoldButton(
                                        onConfirm = onLapClick,
                                        text = "Круг",
                                        iconRes = R.drawable.ic_circle,
                                        holdDurationMs = 0,
                                        controllerPressed = lapButtonPressed,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Box(modifier = Modifier.size(56.dp))
                                }
                            }
                            if (useErrorFixButtons) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
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
                                        iconSize = 24.dp,
                                        textSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        contentSpacing = 8.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        holdDurationMs = 0,
                                        controllerPressed = errorButtonPressed,
                                    )
                                    HoldButton(
                                        onConfirm = onFixClick,
                                        text = "ИСПРАВИЛ",
                                        iconRes = R.drawable.ic_square,
                                        iconSize = 24.dp,
                                        textSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        contentSpacing = 8.dp,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        holdDurationMs = 0,
                                        controllerPressed = fixButtonPressed,
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


