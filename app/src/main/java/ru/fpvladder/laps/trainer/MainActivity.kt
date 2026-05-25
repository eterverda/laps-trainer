package ru.fpvladder.laps.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.ui.components.ActionButtonsRow
import ru.fpvladder.laps.trainer.ui.components.AppHeader
import ru.fpvladder.laps.trainer.ui.components.ChannelDialog
import ru.fpvladder.laps.trainer.ui.components.NameEditorDialog
import ru.fpvladder.laps.trainer.ui.components.NewTrainingWizard
import ru.fpvladder.laps.trainer.ui.components.PilotSection
import ru.fpvladder.laps.trainer.ui.components.TrainingHeader
import ru.fpvladder.laps.trainer.ui.screens.MainContent
import ru.fpvladder.laps.trainer.ui.screens.RaceContent
import ru.fpvladder.laps.trainer.ui.screens.TrainingContent
import ru.fpvladder.laps.trainer.ui.theme.LapsTrainerTheme
import ru.fpvladder.laps.trainer.viewmodel.AppScreen
import ru.fpvladder.laps.trainer.viewmodel.KeyboardViewModel
import ru.fpvladder.laps.trainer.viewmodel.PilotViewModel
import ru.fpvladder.laps.trainer.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {

    private val keyboardViewModel: KeyboardViewModel by viewModels()
    private val pilotViewModel: PilotViewModel by viewModels()
    private val trainingViewModel: TrainingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LapsTrainerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppRoot(
                        keyboardViewModel = keyboardViewModel,
                        pilotViewModel = pilotViewModel,
                        trainingViewModel = trainingViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    keyboardViewModel: KeyboardViewModel,
    pilotViewModel: PilotViewModel,
    trainingViewModel: TrainingViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by pilotViewModel.currentScreen.collectAsState()
    val pilot by pilotViewModel.pilot.collectAsState()
    val trainings by trainingViewModel.trainings.collectAsState()
    val selectedTraining by trainingViewModel.selectedTraining.collectAsState()

    var showChannelDialog by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showNewTrainingWizard by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen == AppScreen.Training) {
        pilotViewModel.navigateTo(AppScreen.Main)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Main -> AppHeader()
                    AppScreen.Race -> PilotSection(
                        pilot = pilot,
                        editable = false,
                        onChannelClick = { showChannelDialog = true },
                        onNameClick = { showNameEditor = true }
                    )
                    AppScreen.Training -> {
                        val training = selectedTraining
                        if (training != null) {
                            TrainingHeader(
                                channelLetter = training.channelLetter ?: pilot.channelLetter,
                                channelNumber = training.channelNumber ?: pilot.channelNumber,
                                channelColor = training.channelColor ?: pilot.channelColor,
                                pilotName = when (training.type) {
                                    ru.fpvladder.laps.trainer.model.TrainingType.INDIVIDUAL -> training.pilotName ?: pilot.name
                                    ru.fpvladder.laps.trainer.model.TrainingType.TEAM -> "Командная"
                                },
                                onChannelClick = { showChannelDialog = true },
                                onNameClick = { showNameEditor = true }
                            )
                        } else {
                            AppHeader()
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        when (currentScreen) {
                            AppScreen.Main -> MainContent(
                                trainings = trainings,
                                onNewTrainingClick = { showNewTrainingWizard = true },
                                onTrainingClick = { training ->
                                    trainingViewModel.selectTraining(training)
                                    pilotViewModel.navigateTo(AppScreen.Training)
                                },
                                onDeleteTraining = { training ->
                                    trainingViewModel.deleteTraining(training)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            AppScreen.Race -> RaceContent(
                                onNavigateToMain = { pilotViewModel.navigateTo(AppScreen.Main) },
                                modifier = Modifier.fillMaxSize()
                            )

                            AppScreen.Training -> TrainingContent(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = "Подключите USB-клавиатуру",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute"
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        ActionButtonsRow()
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                }
            }
        }
    }

    if (showChannelDialog) {
        ChannelDialog(
            currentLetter = pilot.channelLetter,
            currentNumber = pilot.channelNumber,
            currentColor = pilot.channelColor,
            onConfirm = { letter, number, color ->
                pilotViewModel.updateChannel(letter, number, color)
                showChannelDialog = false
            },
            onDismiss = { showChannelDialog = false }
        )
    }

    if (showNameEditor) {
        NameEditorDialog(
            currentName = pilot.name,
            onConfirm = { name ->
                pilotViewModel.updateName(name)
                showNameEditor = false
            },
            onDismiss = { showNameEditor = false }
        )
    }

    if (showNewTrainingWizard) {
        NewTrainingWizard(
            defaultPilotName = pilot.name,
            defaultLetter = pilot.channelLetter,
            defaultNumber = pilot.channelNumber,
            defaultColor = pilot.channelColor,
            onCreateTraining = { training ->
                trainingViewModel.addTraining(training)
                showNewTrainingWizard = false
            },
            onDismiss = { showNewTrainingWizard = false }
        )
    }
}
