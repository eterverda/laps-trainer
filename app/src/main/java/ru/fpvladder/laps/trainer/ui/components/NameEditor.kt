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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.R

@Composable
fun IndividualNameDialog(
    currentName: String,
    isEmpty: Boolean,
    onConfirm: (String) -> Unit,
    onRequestConfirm: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
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
                var nameField by remember { mutableStateOf(TextFieldValue(currentName, selection = TextRange(0, currentName.length))) }
                val focusRequester = remember { FocusRequester() }

                Text(
                    text = stringResource(R.string.individual_pilot),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it.copy(text = it.text.take(24)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm(nameField.text.trim()) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val newName = nameField.text.trim()
                            if (isEmpty || !isSignificantNameChange(currentName, newName)) {
                                onConfirm(newName)
                            } else {
                                onRequestConfirm?.invoke(newName)
                            }
                        },
                        enabled = true
                    ) {
                        Text("OK")
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

private fun isSignificantNameChange(currentName: String, newName: String): Boolean {
    return currentName.isNotBlank() && (newName.isBlank() || newName != currentName)
}

private fun isSignificantNameChange(
    currentName1: String,
    newName1: String,
    currentName2: String,
    newName2: String
): Boolean {
    return isSignificantNameChange(currentName1, newName1) ||
            isSignificantNameChange(currentName2, newName2)
}

@Composable
fun TeamNameDialog(
    name1: String,
    name2: String,
    isEmpty: Boolean,
    onConfirm: (String, String) -> Unit,
    onRequestConfirm: ((String, String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
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
                var name1Field by remember { mutableStateOf(TextFieldValue(name1, selection = TextRange(0, name1.length))) }
                var name2Field by remember { mutableStateOf(TextFieldValue(name2)) }
                val focusRequester1 = remember { FocusRequester() }
                val focusRequester2 = remember { FocusRequester() }

                Text(
                    text = stringResource(R.string.team_pilot_1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = name1Field,
                    onValueChange = { name1Field = it.copy(text = it.text.take(24)) },
                    singleLine = true,
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
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.team_pilot_2),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = name2Field,
                    onValueChange = { name2Field = it.copy(text = it.text.take(24)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm(name1Field.text.trim(), name2Field.text.trim()) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester2)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val newName1 = name1Field.text.trim()
                            val newName2 = name2Field.text.trim()
                            if (isEmpty || !isSignificantNameChange(name1, newName1, name2, newName2)) {
                                onConfirm(newName1, newName2)
                            } else {
                                onRequestConfirm?.invoke(newName1, newName2)
                            }
                        },
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
