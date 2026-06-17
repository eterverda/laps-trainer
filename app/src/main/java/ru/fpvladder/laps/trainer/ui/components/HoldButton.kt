package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.R

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
    enabled: Boolean = true,
    onPressStart: () -> Unit = {},
    onPressEnd: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    var isSuccess by remember { mutableStateOf(false) }
    var activeGesture by remember { mutableStateOf<Boolean?>(null) }

    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    val currentOnPressEnd by rememberUpdatedState(onPressEnd)
    val currentHoldDurationMs by rememberUpdatedState(holdDurationMs)

    val fillColor = MaterialTheme.colorScheme.error

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor = if (isPressed) fillColor else MaterialTheme.colorScheme.primary

    val baseModifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(backgroundColor)

    val content: @Composable BoxScope.() -> Unit = {
        if (currentHoldDurationMs > 0) {
            Box(
                modifier = Modifier
                    .align(if (isSuccess) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(fillColor)
            )
        }

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

    val isClickMode = activeGesture == null && currentHoldDurationMs <= 0

    val gestureModifier = when {
        activeGesture == true -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    activeGesture = true
                    isSuccess = false
                    currentOnPressStart()
                    val confirmAtStart = currentOnConfirm
                    val job = scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = currentHoldDurationMs,
                                easing = LinearEasing
                            )
                        )
                        isSuccess = true
                        confirmAtStart()
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 300)
                        )
                        isSuccess = false
                    }
                    try {
                        awaitRelease()
                    } finally {
                        activeGesture = null
                        currentOnPressEnd()
                        if (job.isActive) {
                            job.cancel()
                            isSuccess = false
                            scope.launch {
                                progress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                        }
                    }
                }
            )
        }
        !enabled -> Modifier
        isClickMode -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                currentOnConfirm()
            }
        )
        else -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    activeGesture = true
                    isSuccess = false
                    currentOnPressStart()
                    val confirmAtStart = currentOnConfirm
                    val job = scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = currentHoldDurationMs,
                                easing = LinearEasing
                            )
                        )
                        isSuccess = true
                        confirmAtStart()
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 300)
                        )
                        isSuccess = false
                    }
                    try {
                        awaitRelease()
                    } finally {
                        activeGesture = null
                        currentOnPressEnd()
                        if (job.isActive) {
                            job.cancel()
                            isSuccess = false
                            scope.launch {
                                progress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    Box(
        modifier = baseModifier.then(gestureModifier).height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
