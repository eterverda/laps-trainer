package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.ui.components.Hint
import ru.fpvladder.laps.trainer.ui.components.LapList

@Composable
internal fun FlightContent(
    isStarted: Boolean,
    startSignal: StartSignal,
    laps: List<Lap>,
    currentLap: Lap?,
    currentLapTime: Long,
    timerPrecision: TimerPrecision,
    holeshotEnabled: Boolean,
    useLapButton: Boolean,
    onLapClick: () -> Unit = {},
    pilot: Pilot?,
    pilotSwapIndex: Int?,
    pilotOrderSwapped: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(laps.size, isStarted) {
        delay(50)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val tapModifier = if (isStarted && !useLapButton) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onLapClick() })
        }
    } else Modifier

    Box(
        modifier = modifier
            .then(tapModifier)
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            LapList(
                laps = laps,
                currentLap = currentLap,
                currentLapTime = currentLapTime,
                timerPrecision = timerPrecision,
                isPostFlight = false,
                isStarted = isStarted,
                pilot = pilot,
                pilotSwapIndex = pilotSwapIndex,
                pilotOrderSwapped = pilotOrderSwapped,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        val showTapHint = laps.size < 3 && !useLapButton
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
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

        if (!isStarted && startSignal == StartSignal.MANUAL) {
            Hint(
                text = "Нажмите GO чтобы начать вылет",
                modifier = Modifier.align(Alignment.BottomCenter),
                paddingValues = PaddingValues(bottom = 16.dp)
            )
        }
    }
}
