package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.ui.theme.LocalExtendedColors

private const val STATE_IDLE = 0
private const val STATE_CLICK_PRESSED = 1
private const val STATE_LONG_PRESSED = 2
private const val STATE_LONG_CONFIRMED = 3

@Composable
fun HoldButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Int = 1200,
    text: String = "Старт",
    iconRes: Int = R.drawable.ic_triangle,
    iconSize: Dp = 30.dp,
    textSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Black,
    contentSpacing: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    controllerPressed: StateFlow<Boolean>? = null,
    onPressStart: () -> Unit = {},
    onPressEnd: () -> Unit = {}
) {
    val fillColor = LocalExtendedColors.current.holdButtonFill

    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var state by remember { mutableIntStateOf(STATE_IDLE) }

    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    val currentOnPressEnd by rememberUpdatedState(onPressEnd)
    val currentHoldDurationMs by rememberUpdatedState(holdDurationMs)

    val pressCount = remember { MutableStateFlow(0) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    suspend fun startHold() {
        progress.snapTo(0f)
        holdJob = scope.launch {
            currentOnPressStart()
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = currentHoldDurationMs,
                    easing = LinearEasing
                )
            )
            state = STATE_LONG_CONFIRMED
            currentOnConfirm()
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
            state = STATE_IDLE
            holdJob = null
        }
    }

    fun stopHold() {
        holdJob?.let { job ->
            currentOnPressEnd()
            if (job.isActive) {
                job.cancel()
                scope.launch {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            }
            holdJob = null
        }
    }

    LaunchedEffect(controllerPressed) {
        var wasPressed = false
        controllerPressed?.collect { pressed ->
            when {
                pressed && !wasPressed -> pressCount.update { it + 1 }
                !pressed && wasPressed -> pressCount.update { it - 1 }
            }
            wasPressed = pressed
        }
    }

    LaunchedEffect(Unit) {
        var previousCount = 0
        pressCount.collect { count ->
            when {
                count > previousCount -> {
                    when {
                        currentHoldDurationMs <= 0 -> {
                            state = STATE_CLICK_PRESSED
                            progress.snapTo(1f)
                        }
                        else -> {
                            state = STATE_LONG_PRESSED
                            startHold()
                        }
                    }
                }
                count < previousCount -> {
                    when (state) {
                        STATE_CLICK_PRESSED -> {
                            progress.snapTo(0f)
                            currentOnConfirm()
                        }
                        STATE_LONG_PRESSED -> stopHold()
                        STATE_LONG_CONFIRMED -> currentOnPressEnd()
                    }
                    state = STATE_IDLE
                }
            }
            previousCount = count
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .drawBehind {
                val progressValue = progress.value
                val width = size.width * progressValue
                val xOffset = if (state == STATE_LONG_CONFIRMED) size.width - width else 0f
                drawRect(
                    color = fillColor,
                    topLeft = Offset(xOffset, 0f),
                    size = Size(width, size.height)
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressCount.update { it + 1 }
                        try {
                            awaitRelease()
                        } finally {
                            pressCount.update { it - 1 }
                        }
                    }
                )
            }
            .height(IntrinsicSize.Min),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(contentSpacing))
            Text(
                text = text.uppercase(),
                fontWeight = fontWeight,
                fontSize = textSize,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
