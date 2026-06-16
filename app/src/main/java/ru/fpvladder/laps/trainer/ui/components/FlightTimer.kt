package ru.fpvladder.laps.trainer.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase
import ru.fpvladder.laps.trainer.ui.screens.formatCountdown
import ru.fpvladder.laps.trainer.ui.theme.LocalExtendedColors
import ru.fpvladder.laps.trainer.ui.screens.formatTime

@Composable
internal fun FlightTimer(
    flightPhase: FlightPhase,
    startSignal: StartSignal,
    elapsedMs: Long,
    isPreBlinking: Boolean,
    timerPrecision: TimerPrecision,
    timeLimitSeconds: Int,
    changeRemainingMs: Long? = null
) {
    val context = LocalContext.current
    DisposableEffect(flightPhase) {
        val window = (context as? Activity)?.window
        val keepOn = flightPhase == FlightPhase.PRE_FLIGHT || flightPhase == FlightPhase.FLIGHT
        if (keepOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isBlinking = flightPhase == FlightPhase.PRE_FLIGHT && isPreBlinking
    val alpha by animateFloatAsState(
        targetValue = if (isBlinking) 0f else 1f,
        animationSpec = tween(200),
        label = "timer_blink"
    )

    val hasChange = changeRemainingMs != null
    val hasLimit = timeLimitSeconds != Int.MAX_VALUE
    val secondLine: AnnotatedString? = when {
        hasChange -> {
            when {
                changeRemainingMs > 0 -> buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    ) {
                        append(formatCountdown(changeRemainingMs, timerPrecision))
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

    val alphaValue = if (flightPhase == FlightPhase.PRE_FLIGHT && startSignal == StartSignal.FIXED) 1f else alpha

    Column(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .alpha(alphaValue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val timerTextColor = LocalExtendedColors.current.timerOnSurface
        val fractionFontSize = when (timerPrecision.fractionDigits) {
            3 -> 30.sp
            2 -> 40.sp
            else -> 60.sp
        }
        val timeText = formatTime(
            elapsedMs,
            timerPrecision,
            timeLimitSeconds
        )
        val dotIndex = timeText.indexOf('.')
        val annotatedTimeText = buildAnnotatedString {
            append(timeText.substring(0, dotIndex))
            withStyle(
                style = SpanStyle(
                    fontSize = fractionFontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(timeText.substring(dotIndex, timeText.length))
                append(" ")
            }
        }

        Text(
            text = annotatedTimeText,
            color = timerTextColor,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        if (secondLine != null) {
            Text(
                text = secondLine,
                color = timerTextColor,
                style = TextStyle(fontSize = 14.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}
