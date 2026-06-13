package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.screens.formatCountdown
import ru.fpvladder.laps.trainer.ui.screens.formatTime

@Composable
internal fun FlightTimer(
    isStarted: Boolean,
    startSignal: StartSignal,
    elapsedMs: Long,
    preStartCountdownMs: Long,
    isPreBlinking: Boolean,
    timerPrecision: TimerPrecision,
    timeLimitSeconds: Int,
    changeRemainingMs: Long? = null
) {
    val isBlinking = !isStarted && isPreBlinking
    val alpha by animateFloatAsState(
        targetValue = if (isBlinking) 0f else 1f,
        animationSpec = tween(200),
        label = "timer_blink"
    )

    val timeText = if (!isStarted && startSignal == StartSignal.FIXED) {
        formatCountdown(preStartCountdownMs, timerPrecision) + " "
    } else {
        formatTime(
            if (!isStarted) 0L else elapsedMs,
            timerPrecision,
            timeLimitSeconds
        )
    }

    val hasChange = changeRemainingMs != null
    val hasLimit = timeLimitSeconds != Int.MAX_VALUE
    val secondLine: AnnotatedString? = when {
        hasChange -> {
            val remainingToChange = changeRemainingMs!!
            when {
                remainingToChange > 0 -> buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    ) {
                        append(formatCountdown(remainingToChange, timerPrecision))
                    }
                    withStyle(style = SpanStyle(fontSize = 14.sp)) {
                        append(" до смены")
                    }
                }
                hasLimit -> {
                    val remainingMs = (timeLimitSeconds * 1000L - elapsedMs).coerceAtLeast(0)
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        ) {
                            append(formatCountdown(remainingMs, timerPrecision))
                        }
                        withStyle(style = SpanStyle(fontSize = 14.sp)) {
                            append(" до конца")
                        }
                    }
                }
                else -> null
            }
        }
        hasLimit -> {
            val remainingMs = (timeLimitSeconds * 1000L - elapsedMs).coerceAtLeast(0)
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                ) {
                    append(formatCountdown(remainingMs, timerPrecision))
                }
            }
        }
        else -> null
    }

    val alphaValue = if (!isStarted && startSignal == StartSignal.FIXED) 1f else alpha

    Column(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .alpha(alphaValue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedText(
            text = timeText,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        if (secondLine != null) {
            OutlinedText(
                text = secondLine,
                strokeWidth = 4f,
                style = TextStyle(fontSize = 14.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}
