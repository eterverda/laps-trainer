package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Pilot

@Composable
fun PilotEditorDialog(
    currentPilot: Pilot,
    onConfirm: (Pilot) -> Unit,
    onDismiss: () -> Unit
) {
    var isTeam by remember { mutableStateOf(currentPilot is Pilot.Team) }
    var name1 by remember { mutableStateOf(currentPilot.name) }
    var name2 by remember {
        mutableStateOf(if (currentPilot is Pilot.Team) currentPilot.name2 else "")
    }
    val currentChannel = when (currentPilot) {
        is Pilot.Individual -> currentPilot.channel
        is Pilot.Team -> currentPilot.channel
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabButton(
                        text = "Один",
                        selected = !isTeam,
                        onClick = { isTeam = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Команда",
                        selected = isTeam,
                        onClick = { isTeam = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val focusRequester = remember { FocusRequester() }

                OutlinedTextField(
                    value = name1,
                    onValueChange = { name1 = it.take(24) },
                    label = { Text(if (isTeam) "Первый пилот" else "Пилот") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = if (isTeam) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name1.isNotBlank() && (!isTeam || name2.isNotBlank())) {
                                val pilot = if (isTeam) {
                                    Pilot.Team(name1 = name1.trim(), name2 = name2.trim(), channel = currentChannel)
                                } else {
                                    Pilot.Individual(name = name1.trim(), channel = currentChannel)
                                }
                                onConfirm(pilot)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (isTeam) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name2,
                        onValueChange = { name2 = it.take(24) },
                        label = { Text("Второй пилот") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (name1.isNotBlank() && name2.isNotBlank()) {
                                    onConfirm(Pilot.Team(name1 = name1.trim(), name2 = name2.trim(), channel = currentChannel))
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Отмена")
                    }
                    val canConfirm = if (isTeam) {
                        name1.isNotBlank() && name2.isNotBlank()
                    } else {
                        name1.isNotBlank()
                    }
                    Button(
                        onClick = {
                            val pilot = if (isTeam) {
                                Pilot.Team(
                                    name1 = name1.trim(),
                                    name2 = name2.trim(),
                                    channel = currentChannel
                                )
                            } else {
                                Pilot.Individual(
                                    name = name1.trim(),
                                    channel = currentChannel
                                )
                            }
                            onConfirm(pilot)
                        },
                        enabled = canConfirm
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
