package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.TrainingType
import ru.fpvladder.laps.trainer.ui.components.SectionTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainContent(
    trainings: List<Training>,
    onNewTrainingClick: () -> Unit,
    onTrainingClick: (Training) -> Unit,
    onDeleteTraining: (Training) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                items = trainings,
                key = { it.id }
            ) { training ->
                SwipeTrainingCard(
                    training = training,
                    onClick = { onTrainingClick(training) },
                    onDelete = { onDeleteTraining(training) }
                )
            }
        }

        OutlinedButton(
            onClick = onNewTrainingClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Новая тренировка")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeTrainingCard(
    training: Training,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isDeleted by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var dismissKey by remember { mutableStateOf(0) }

    AnimatedVisibility(
        visible = !isDeleted,
        exit = shrinkVertically() + fadeOut()
    ) {
        key(dismissKey) {
            val state = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                        showConfirmDialog = true
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = state,
                backgroundContent = {
                    val color = MaterialTheme.colorScheme.errorContainer
                    val contentColor = MaterialTheme.colorScheme.onErrorContainer

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = contentColor
                            )
                            Text("Удалить", color = contentColor)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Тренировка ${formatDate(training.createdAt)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (training.type == TrainingType.INDIVIDUAL) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                val channelColor = training.channelColor ?: ChannelColor.RED
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = channelColor.color,
                                    border = channelColor.outlineColor?.let { BorderStroke(1.dp, it) }
                                ) {
                                    Text(
                                        text = "${training.channelLetter}${training.channelNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (channelColor == ChannelColor.WHITE) androidx.compose.ui.graphics.Color.Black
                                        else androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                }

                                Text(
                                    text = training.pilotName ?: "",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Командная",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        Dialog(onDismissRequest = {
            showConfirmDialog = false
            dismissKey++
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionTitle("Удалить тренировку?")

                    Text(
                        text = "Это действие нельзя отменить.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
                                dismissKey++
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showConfirmDialog = false
                                isDeleted = true
                                onDelete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
