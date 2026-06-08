package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    iconRes: Int = R.drawable.ic_triangle
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    var isFilling by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val fillColor = Color(0xFFcc5555)

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(holdDurationMs) {
                detectTapGestures(
                    onPress = {
                        isSuccess = false
                        isFilling = true
                        val job = scope.launch {
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = holdDurationMs,
                                    easing = LinearEasing
                                )
                            )
                            isFilling = false
                            isSuccess = true
                            onConfirm()
                            progress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                            isSuccess = false
                        }
                        try {
                            awaitRelease()
                        } finally {
                            if (job.isActive) {
                                job.cancel()
                                isFilling = false
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
    ) {
        Box(
            modifier = Modifier
                .align(if (isSuccess) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(fillColor)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
