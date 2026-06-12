package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.model.AppTheme
import ru.fpvladder.laps.trainer.ui.components.FlightTimer
import ru.fpvladder.laps.trainer.ui.components.HoldButton


private val FlightSurfaceShape = RoundedCornerShape(24.dp)

@Composable
fun FlightScreen(
    isPostFlight: Boolean,
    isStarted: Boolean,
    startSignal: StartSignal,
    laps: List<Lap> = emptyList(),
    currentLap: Lap? = null,
    currentLapTime: Long = 0L,
    elapsedMs: Long = 0L,
    preStartCountdownMs: Long = 0L,
    isPreBlinking: Boolean = false,
    timeLimitSeconds: Int = 0,
    maxLaps: Int = Int.MAX_VALUE,
    stopReason: StopReason? = null,
    shouldSaveResult: Boolean = false,
    onShouldSaveResultChange: (Boolean) -> Unit = {},
    swapPilotsForNextFlight: Boolean = false,
    onSwapPilotsForNextFlightChange: (Boolean) -> Unit = {},
    useLapButton: Boolean = false,
    useErrorFixButtons: Boolean = false,
    onLapClick: () -> Unit = {},
    onErrorClick: () -> Unit = {},
    onFixClick: () -> Unit = {},
    timerPrecision: TimerPrecision,
    enabledRecordKinds: Set<Record.Kind> = emptySet(),
    holeshotEnabled: Boolean = false,
    pilot: Pilot? = null,
    pilotSwapIndex: Int? = null,
    pilotOrderSwapped: Boolean = false,
    swapRemainingMs: Long? = null,
    hasPagerWiggled: Boolean = false,
    onPagerWiggleComplete: () -> Unit = {},
    onBackClick: () -> Unit = {},
    appTheme: AppTheme,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDarkTheme = appTheme == AppTheme.DARK || (appTheme == AppTheme.SYSTEM && isSystemInDarkTheme())
    val timerSurfaceColor = if (isDarkTheme) {
        Color(
            red = primary.red * 0.55f,
            green = primary.green * 0.55f,
            blue = primary.blue * 0.55f
        )
    } else {
        Color(
            red = (primary.red + (1f - primary.red) * 0.35f).coerceIn(0f, 1f),
            green = (primary.green + (1f - primary.green) * 0.35f).coerceIn(0f, 1f),
            blue = (primary.blue + (1f - primary.blue) * 0.35f).coerceIn(0f, 1f)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        var timerHeight by remember { mutableIntStateOf(0) }
        var buttonsHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current

        val timerVisible = !isPostFlight
        val overlapPx = with(density) { 45.dp.roundToPx() }
        val targetTopPadding = if (timerVisible) (timerHeight - overlapPx).coerceAtLeast(0) else 0
        val animatedTopPadding by animateDpAsState(
            targetValue = with(density) { targetTopPadding.toDp() },
            label = "timer_reveal"
        )

        val buttonsVisible = !isPostFlight && isStarted && (useLapButton || useErrorFixButtons)
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
                            isStarted = isStarted,
                            startSignal = startSignal,
                            elapsedMs = elapsedMs,
                            preStartCountdownMs = preStartCountdownMs,
                            isPreBlinking = isPreBlinking,
                            timerPrecision = timerPrecision,
                            timeLimitSeconds = timeLimitSeconds,
                            swapRemainingMs = swapRemainingMs
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .padding(top = animatedTopPadding, bottom = animatedBottomPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            shape = FlightSurfaceShape
        ) {
            if (!isPostFlight) {
                FlightContent(
                    isStarted = isStarted,
                    startSignal = startSignal,
                    laps = laps,
                    currentLap = currentLap,
                    currentLapTime = currentLapTime,
                    timerPrecision = timerPrecision,
                    holeshotEnabled = holeshotEnabled,
                    useLapButton = useLapButton,
                    onLapClick = onLapClick,
                    pilot = pilot,
                    pilotSwapIndex = pilotSwapIndex,
                    pilotOrderSwapped = pilotOrderSwapped
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
                    enabledRecordKinds = enabledRecordKinds,
                    pilot = pilot,
                    pilotSwapIndex = pilotSwapIndex,
                    pilotOrderSwapped = pilotOrderSwapped,
                    shouldSaveResult = shouldSaveResult,
                    onShouldSaveResultChange = onShouldSaveResultChange,
                    swapPilotsForNextFlight = swapPilotsForNextFlight,
                    onSwapPilotsForNextFlightChange = onSwapPilotsForNextFlightChange,
                    onBackClick = onBackClick,
                    hasPagerWiggled = hasPagerWiggled,
                    onPagerWiggleComplete = onPagerWiggleComplete
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
                                            .height(54.dp)
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
                                    Button(
                                        onClick = onErrorClick,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 24.dp,
                                            vertical = 6.dp
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_cross),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ОШИБКА", fontSize = 14.sp)
                                    }
                                    Button(
                                        onClick = onFixClick,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 24.dp,
                                            vertical = 6.dp
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_square),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ИСПРАВИЛ", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


