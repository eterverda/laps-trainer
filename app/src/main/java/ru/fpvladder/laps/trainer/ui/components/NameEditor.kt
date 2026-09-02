package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Pilot

sealed class PilotNameDialogState {
    abstract val pilot: Pilot
    data class Rename(override val pilot: Pilot) : PilotNameDialogState()
    data class Create(override val pilot: Pilot) : PilotNameDialogState()
}

sealed class PilotNameDialogResult {
    data class Rename(val pilot: Pilot) : PilotNameDialogResult()
    data class Create(val pilot: Pilot) : PilotNameDialogResult()
}

@Composable
fun PilotNameDialog(
    state: PilotNameDialogState,
    onResult: (PilotNameDialogResult) -> Unit,
    onDismiss: () -> Unit
) {
    val originalPilot = state.pilot
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingNewPilot by remember { mutableStateOf<Pilot?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
        ) {
            when (originalPilot) {
                is Pilot.Individual -> {
                    var nameField by remember(originalPilot) {
                        mutableStateOf(
                            TextFieldValue(
                                originalPilot.name,
                                selection = TextRange(0, originalPilot.name.length)
                            )
                        )
                    }
                    val focusRequester = remember { FocusRequester() }

                    val handleConfirm = {
                        val newName = nameField.text.trim()
                        val newPilot = Pilot.Individual(name = newName, channel = originalPilot.channel)
                        if (state is PilotNameDialogState.Create ||
                            !isSignificantNameChange(originalPilot.name, newName)
                        ) {
                            onResult(
                                when (state) {
                                    is PilotNameDialogState.Create -> PilotNameDialogResult.Create(newPilot)
                                    is PilotNameDialogState.Rename -> PilotNameDialogResult.Rename(newPilot)
                                }
                            )
                        } else {
                            pendingNewPilot = newPilot
                            showConfirmation = true
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PilotNameInputField(
                            label = stringResource(R.string.individual_pilot),
                            value = nameField,
                            onValueChange = { nameField = it.copy(text = it.text.take(24)) },
                            focusRequester = focusRequester,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleConfirm() }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Отмена")
                            }
                            Button(onClick = { handleConfirm() }) {
                                Text("OK")
                            }
                        }

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }

                is Pilot.Team -> {
                    var name1Field by remember(originalPilot) {
                        mutableStateOf(
                            TextFieldValue(
                                originalPilot.name1,
                                selection = TextRange(0, originalPilot.name1.length)
                            )
                        )
                    }
                    var name2Field by remember(originalPilot) {
                        mutableStateOf(TextFieldValue(originalPilot.name2))
                    }
                    val focusRequester1 = remember { FocusRequester() }
                    val focusRequester2 = remember { FocusRequester() }

                    val handleConfirm = {
                        val newName1 = name1Field.text.trim()
                        val newName2 = name2Field.text.trim()
                        val newPilot = Pilot.Team(
                            name1 = newName1,
                            name2 = newName2,
                            channel = originalPilot.channel
                        )
                        if (state is PilotNameDialogState.Create ||
                            !(isSignificantNameChange(originalPilot.name1, newName1) ||
                                    isSignificantNameChange(originalPilot.name2, newName2))
                        ) {
                            onResult(
                                when (state) {
                                    is PilotNameDialogState.Create -> PilotNameDialogResult.Create(newPilot)
                                    is PilotNameDialogState.Rename -> PilotNameDialogResult.Rename(newPilot)
                                }
                            )
                        } else {
                            pendingNewPilot = newPilot
                            showConfirmation = true
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PilotNameInputField(
                            label = stringResource(R.string.team_pilot_1),
                            value = name1Field,
                            onValueChange = { name1Field = it.copy(text = it.text.take(24)) },
                            focusRequester = focusRequester1,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    name2Field = name2Field.copy(selection = TextRange(0, name2Field.text.length))
                                    focusRequester2.requestFocus()
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PilotNameInputField(
                            label = stringResource(R.string.team_pilot_2),
                            value = name2Field,
                            onValueChange = { name2Field = it.copy(text = it.text.take(24)) },
                            focusRequester = focusRequester2,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleConfirm() }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Отмена")
                            }
                            Button(
                                onClick = { handleConfirm() },
                                enabled = name1Field.text.isNotBlank() == name2Field.text.isNotBlank()
                            ) {
                                Text("OK")
                            }
                        }

                        LaunchedEffect(Unit) {
                            focusRequester1.requestFocus()
                        }
                    }
                }
            }
        }
    }

    pendingNewPilot?.let { newPilot ->
        ConfirmNameChangeDialog(
            message = when (originalPilot) {
                is Pilot.Individual -> "Имя изменено. Вы уверены что хотите продолжить текущую тренировку с новым пилотом?"
                is Pilot.Team -> {
                    val team = newPilot as Pilot.Team
                    val changedNames = listOfNotNull(
                        originalPilot.name1.takeIf { it != team.name1 },
                        originalPilot.name2.takeIf { it != team.name2 }
                    ).size
                    if (changedNames >= 2) {
                        "Имена изменены. Вы уверены что хотите продолжить текущую тренировку с новой командой?"
                    } else {
                        "Имя изменено. Вы уверены что хотите продолжить текущую тренировку с новым пилотом?"
                    }
                }
            },
            onContinue = {
                onResult(PilotNameDialogResult.Rename(newPilot))
                showConfirmation = false
                pendingNewPilot = null
            },
            onCreateNew = {
                onResult(PilotNameDialogResult.Create(newPilot))
                showConfirmation = false
                pendingNewPilot = null
            },
            onDismiss = {
                showConfirmation = false
                pendingNewPilot = null
            }
        )
    }
}

@Composable
private fun PilotNameInputField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }
}

private fun isSignificantNameChange(currentName: String, newName: String): Boolean {
    return currentName.isNotBlank() && (newName.isBlank() || newName != currentName)
}
