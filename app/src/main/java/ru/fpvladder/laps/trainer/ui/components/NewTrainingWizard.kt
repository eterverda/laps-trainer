package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.ChannelConfig
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.TrainingType

@Composable
fun NewTrainingWizard(
    defaultPilotName: String,
    defaultLetter: String,
    defaultNumber: Int,
    defaultColor: ChannelColor,
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    onCreateTraining: (Training) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            WizardContent(
                defaultPilotName = defaultPilotName,
                defaultLetter = defaultLetter,
                defaultNumber = defaultNumber,
                defaultColor = defaultColor,
                channelGrid = channelGrid,
                colorCount = colorCount,
                onCreateTraining = onCreateTraining,
                onDismiss = onDismiss
            )
        }
    }
}

private enum class WizardStep {
    Type, Name, Channel, TeamPlaceholder
}

@Composable
private fun WizardContent(
    defaultPilotName: String,
    defaultLetter: String,
    defaultNumber: Int,
    defaultColor: ChannelColor,
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    onCreateTraining: (Training) -> Unit,
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(WizardStep.Type) }
    var selectedType by rememberSaveable { mutableStateOf(TrainingType.INDIVIDUAL.name) }
    var pilotName by rememberSaveable { mutableStateOf(defaultPilotName) }
    var channelLetter by rememberSaveable { mutableStateOf(defaultLetter) }
    var channelNumber by rememberSaveable { mutableStateOf(defaultNumber) }
    var channelColor by rememberSaveable { mutableStateOf(defaultColor.name) }

    val trainingType = TrainingType.valueOf(selectedType)
    val color = ChannelColor.valueOf(channelColor)
    val channelValid = ChannelConfig.isValidChannel(channelGrid, channelLetter, channelNumber) &&
            ChannelConfig.isValidColor(colorCount, color)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (step) {
            WizardStep.Type -> {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionTitle("Тренировка")
                    TrainingTypeSelector(
                        selected = trainingType,
                        onSelect = { selectedType = it.name }
                    )
                }
            }

            WizardStep.Name -> {
                NameEditorContent(
                    currentName = pilotName,
                    onConfirm = { name ->
                        pilotName = name
                        if (name.isNotBlank()) {
                            step = WizardStep.Channel
                        }
                    },
                    onDismiss = { },
                    showActions = false,
                    onValueChange = { pilotName = it }
                )
            }

            WizardStep.Channel -> {
                ChannelEditorContent(
                    currentLetter = channelLetter,
                    currentNumber = channelNumber,
                    currentColor = color,
                    channelGrid = channelGrid,
                    colorCount = colorCount,
                    onConfirm = { _, _, _ -> },
                    onDismiss = { },
                    showActions = false,
                    onValuesChange = { l, n, c ->
                        channelLetter = l
                        channelNumber = n
                        channelColor = c.name
                    }
                )
            }

            WizardStep.TeamPlaceholder -> {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TeamPlaceholderContent()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            when (step) {
                WizardStep.Type -> {
                    Button(
                        onClick = {
                            step = if (trainingType == TrainingType.INDIVIDUAL) {
                                WizardStep.Name
                            } else {
                                WizardStep.TeamPlaceholder
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Далее")
                    }
                }

                WizardStep.Name -> {
                    Button(
                        onClick = {
                            if (pilotName.isNotBlank()) {
                                step = WizardStep.Channel
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pilotName.isNotBlank()
                    ) {
                        Text("Далее")
                    }
                }

                WizardStep.Channel -> {
                    Button(
                        onClick = {
                            val training = Training(
                                type = TrainingType.INDIVIDUAL,
                                pilotName = pilotName.trim(),
                                channelLetter = channelLetter,
                                channelNumber = channelNumber,
                                channelColor = color
                            )
                            onCreateTraining(training)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = channelValid
                    ) {
                        Text("OK")
                    }
                }

                WizardStep.TeamPlaceholder -> {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingTypeSelector(
    selected: TrainingType,
    onSelect: (TrainingType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TrainingType.entries.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == type,
                        onClick = { onSelect(type) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == type,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun TeamPlaceholderContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Командные тренировки пока не реализованы",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
