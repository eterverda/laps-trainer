package ru.fpvladder.laps.trainer.ui.components

import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.fpvladder.laps.trainer.ui.components.DoubleBottomSurface
import ru.fpvladder.laps.trainer.ui.helpers.ChannelColor
import ru.fpvladder.laps.trainer.ui.helpers.toComposeColor
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.ui.helpers.label
import ru.fpvladder.laps.trainer.ui.helpers.label1
import ru.fpvladder.laps.trainer.ui.helpers.label2

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrainingHeader(
    trainings: List<Training>,
    selectedTraining: Training,
    enabled: Boolean = true,
    onChannelClick: () -> Unit = {},
    onNameClick: () -> Unit = {},
    onAddIndividualClick: () -> Unit = {},
    onAddTeamClick: () -> Unit = {},
    onTrainingSelect: (Training) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val maxListHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        screenHeightDp * 3 / 4
    } else {
        screenHeightDp / 2
    }

    val channel = when (selectedTraining) {
        is Training.Individual -> selectedTraining.pilot.channel
        is Training.Team -> selectedTraining.pilot.channel
    }
    val pilot = when (selectedTraining) {
        is Training.Individual -> selectedTraining.pilot
        is Training.Team -> selectedTraining.pilot
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            ChannelBadge(
                channel = channel,
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        enabled = enabled,
                        onClick = onChannelClick
                    )
            )

            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = enabled,
                        onClick = onNameClick
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
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable(enabled = enabled) { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    val isSingleTraining = trainings.size <= 1
                    Icon(
                        imageVector = when {
                            expanded -> Icons.Default.KeyboardArrowUp
                            isSingleTraining -> Icons.Default.Add
                            else -> Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = when {
                            expanded -> "Свернуть"
                            isSingleTraining -> "Добавить"
                            else -> "Развернуть"
                        },
                        tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        }

        if (expanded) {
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val density = LocalDensity.current
                val context = LocalContext.current
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = windowManager.currentWindowMetrics
                val insetsCompat = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(metrics.windowInsets)
                val cutout = insetsCompat.getInsetsIgnoringVisibility(
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )
                val screenWidthPx = metrics.bounds.width()
                val safeWidthPx = screenWidthPx - cutout.left - cutout.right
                val safeWidthDp = with(density) { safeWidthPx.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expanded = false },
                    contentAlignment = Alignment.TopStart
                ) {
                    val scrollState = rememberScrollState()
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        DoubleBottomSurface(
                            modifier = Modifier
                                .width(safeWidthDp)
                                .padding(8.dp)
                                .heightIn(max = maxListHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {},
                            upperContentPadding = 0.dp,
                            lowerPaddingTop = 72.dp,
                            lowerPaddingBottom = 16.dp,
                            upperContent = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState),
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    trainings.forEach { training ->
                                        TrainingListItem(
                                            training = training,
                                            isSelected = training.id == selectedTraining.id,
                                            onClick = {
                                                onTrainingSelect(training)
                                                expanded = false
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            },
                            lowerContent = {
                                Row(
                                    modifier = Modifier
                                        .width(IntrinsicSize.Max)
                                        .align(Alignment.Center),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBadge(
    channel: Channel,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val preset = remember(channel.color) { ChannelColor.entries.find { it.color == channel.color } }
    val hasOutline = preset?.outlineColor != null
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = channel.color.toComposeColor(),
        border = if (hasOutline) BorderStroke(
            if (fontSize.value >= 20) 2.dp else 1.5.dp,
            preset!!.outlineColor!!.toComposeColor()
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
                text = "${channel.letter}${channel.number}",
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (preset == ChannelColor.WHITE) Color.Black else Color.White
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
    val context = LocalContext.current
    when (pilot) {
        is Pilot.Individual -> {
            Text(
                text = pilot.label(context),
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }

        is Pilot.Team -> {
            Column(modifier = modifier) {
                Text(
                    text = pilot.label1(context),
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pilot.label2(context),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrainingListItem(
    training: Training,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val channel = when (training) {
        is Training.Individual -> training.pilot.channel
        is Training.Team -> training.pilot.channel
    }
    val pilot = when (training) {
        is Training.Individual -> training.pilot
        is Training.Team -> training.pilot
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelBadge(
                channel = channel,
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
                        Text(
                            text = pilot.label(context),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    is Pilot.Team -> {
                        Column {
                            Text(
                                text = pilot.label1(context),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pilot.label2(context),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
