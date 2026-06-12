package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Training

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrainingHeader(
    trainings: List<Training>,
    selectedTraining: Training,
    enabled: Boolean = true,
    onChannelClick: () -> Unit = {},
    onNameLongClick: () -> Unit = {},
    onAddIndividualClick: () -> Unit = {},
    onAddTeamClick: () -> Unit = {},
    onTrainingSelect: (Training) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val hasMultipleTrainings = trainings.size > 1
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val maxListHeight = screenHeightDp / 2

    val otherTrainings = remember(trainings, selectedTraining) {
        trainings
            .filter { it.id != selectedTraining.id }
            .sortedByDescending { it.createdAt }
    }

    val channel = when (selectedTraining) {
        is Training.Individual -> selectedTraining.pilot.channel
        is Training.Team -> selectedTraining.pilot.channel
    }
    val pilot = when (selectedTraining) {
        is Training.Individual -> selectedTraining.pilot
        is Training.Team -> selectedTraining.pilot
    }

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val extraOffsetPx = with(density) { 12.dp.toPx() }.toInt()
    val screenHeightPx = with(density) { screenHeightDp.toPx() }.toInt()
    val popupHeightPx = (screenHeightPx - headerHeightPx - extraOffsetPx).coerceAtLeast(0)
    val popupHeightDp = with(density) { popupHeightPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { headerHeightPx = it.size.height }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            ChannelBadge(
                channelLetter = channel.letter,
                channelNumber = channel.number,
                channelColor = channel.color,
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        enabled = !expanded && enabled,
                        onClick = onChannelClick
                    )
            )

            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        enabled = !expanded && enabled,
                        onLongClick = onNameLongClick,
                        onClick = {}
                    )
            ) {
                PilotNameDisplay(
                    pilot = pilot,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable(enabled = enabled) { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }

        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(0, headerHeightPx + extraOffsetPx),
            properties = PopupProperties(focusable = false)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Box(modifier = Modifier.height(popupHeightDp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                expanded = false
                            }
                    )

                    val scrollState = rememberScrollState()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .heightIn(max = maxListHeight)
                            .align(Alignment.TopCenter)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            otherTrainings.forEach { training ->
                                TrainingListItem(
                                    training = training,
                                    onClick = {
                                        onTrainingSelect(training)
                                        expanded = false
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        expanded = false
                                        onAddIndividualClick()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Пилот")
                                }
                                OutlinedButton(
                                    onClick = {
                                        expanded = false
                                        onAddTeamClick()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Команда")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBadge(
    channelLetter: String,
    channelNumber: Int,
    channelColor: ChannelColor,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val hasOutline = channelColor.outlineColor != null
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = channelColor.color,
        border = if (hasOutline) BorderStroke(
            if (fontSize.value >= 20) 2.dp else 1.5.dp,
            channelColor.outlineColor!!
        ) else null,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (fontSize.value >= 20) 8.dp else 6.dp,
                vertical = if (fontSize.value >= 20) 4.dp else 3.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$channelLetter$channelNumber",
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (channelColor == ChannelColor.WHITE) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun PilotNameDisplay(
    pilot: Pilot,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    when (pilot) {
        is Pilot.Individual -> {
            if (pilot.name.isBlank()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                            append("Laps")
                        }
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(".Trainer")
                        }
                    },
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            } else {
                Text(
                    text = pilot.name,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }
        }
        is Pilot.Team -> {
            val hasName1 = pilot.name1.isNotBlank()
            val hasName2 = pilot.name2.isNotBlank()
            when {
                !hasName1 && !hasName2 -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Laps")
                            }
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(".Trainer")
                            }
                        },
                        fontSize = fontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = modifier
                    )
                }
                hasName1 && hasName2 -> {
                    Column(modifier = modifier) {
                        Text(
                            text = pilot.name1,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pilot.name2,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {
                    Text(
                        text = if (hasName1) pilot.name1 else pilot.name2,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingListItem(
    training: Training,
    onClick: () -> Unit
) {
    val channel = when (training) {
        is Training.Individual -> training.pilot.channel
        is Training.Team -> training.pilot.channel
    }
    val pilot = when (training) {
        is Training.Individual -> training.pilot
        is Training.Team -> training.pilot
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChannelBadge(
            channelLetter = channel.letter,
            channelNumber = channel.number,
            channelColor = channel.color,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxHeight()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 3.dp)
        ) {
            when (pilot) {
                is Pilot.Individual -> {
                    if (pilot.name.isBlank()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append("Laps")
                                }
                                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(".Trainer")
                                }
                            },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = pilot.name,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is Pilot.Team -> {
                    val n1Blank = pilot.name1.isBlank()
                    val n2Blank = pilot.name2.isBlank()
                    when {
                        n1Blank && n2Blank -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                        append("Laps")
                                    }
                                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(".Trainer")
                                    }
                                },
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        n1Blank -> {
                            Text(
                                text = pilot.name2,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        n2Blank -> {
                            Text(
                                text = pilot.name1,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = pilot.name1,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pilot.name2,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
