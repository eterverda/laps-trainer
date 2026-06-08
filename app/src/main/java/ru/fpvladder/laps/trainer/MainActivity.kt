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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.ui.components.ChannelDialog
import ru.fpvladder.laps.trainer.ui.components.HoldButton
import ru.fpvladder.laps.trainer.ui.components.IndividualNameDialog
import ru.fpvladder.laps.trainer.ui.components.TeamNameDialog
import ru.fpvladder.laps.trainer.ui.components.PilotEditorDialog
// import ru.fpvladder.laps.trainer.ui.components.PilotSection
import ru.fpvladder.laps.trainer.ui.components.RulesEditorDialog
import ru.fpvladder.laps.trainer.ui.components.TrainingHeader
import ru.fpvladder.laps.trainer.ui.screens.FlightContent
import ru.fpvladder.laps.trainer.ui.screens.SettingsScreen
import ru.fpvladder.laps.trainer.ui.screens.StatsContent
import ru.fpvladder.laps.trainer.ui.theme.LapsTrainerTheme
import ru.fpvladder.laps.trainer.model.AppTheme
import ru.fpvladder.laps.trainer.model.Channel

import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.description

import ru.fpvladder.laps.trainer.viewmodel.AppScreen
import ru.fpvladder.laps.trainer.viewmodel.KeyboardViewModel
import ru.fpvladder.laps.trainer.viewmodel.PilotViewModel
import ru.fpvladder.laps.trainer.viewmodel.SettingsViewModel
import ru.fpvladder.laps.trainer.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {

    private val keyboardViewModel: KeyboardViewModel by viewModels()
    private val pilotViewModel: PilotViewModel by viewModels()
    private val trainingViewModel: TrainingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by settingsViewModel.appTheme.collectAsState()
            LapsTrainerTheme(appTheme = appTheme) {
                AppRoot(
                    keyboardViewModel = keyboardViewModel,
                    pilotViewModel = pilotViewModel,
                    trainingViewModel = trainingViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    keyboardViewModel: KeyboardViewModel,
    pilotViewModel: PilotViewModel,
    trainingViewModel: TrainingViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by pilotViewModel.currentScreen.collectAsState()
    val trainings by trainingViewModel.trainings.collectAsState()
    val selectedTraining by trainingViewModel.selectedTraining.collectAsState()
    val channelGrid by settingsViewModel.channelGrid.collectAsState()
    val colorCount by settingsViewModel.colorCount.collectAsState()
    val isMuted by settingsViewModel.isMuted.collectAsState()
    val isUsbKeyboardEnabled by settingsViewModel.isUsbKeyboardEnabled.collectAsState()
    val appTheme by settingsViewModel.appTheme.collectAsState()

    var showChannelDialog by remember { mutableStateOf(false) }
    var showPilotEditor by remember { mutableStateOf(false) }
    var showIndividualNameDialog by remember { mutableStateOf(false) }
    var showTeamNameDialog by remember { mutableStateOf(false) }
    var nameEditorIsNew by remember { mutableStateOf(false) }
    var showRulesEditor by remember { mutableStateOf(false) }

    val currentPilot = when (selectedTraining) {
        is Training.Individual -> (selectedTraining as Training.Individual).pilot
        is Training.Team -> (selectedTraining as Training.Team).pilot
    }

    if (currentScreen == AppScreen.Settings) {
        BackHandler {
            pilotViewModel.navigateTo(AppScreen.Training)
        }
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
                    AppScreen.Flight -> TrainingHeader(
                        trainings = trainings,
                        selectedTraining = selectedTraining,
                        enabled = false,
                        onChannelClick = {},
                        onNameLongClick = {},
                        onAddIndividualClick = {},
                        onAddTeamClick = {},
                        onTrainingSelect = {}
                    )
                    AppScreen.Training -> {
                        TrainingHeader(
                            trainings = trainings,
                            selectedTraining = selectedTraining,
                            onChannelClick = { showChannelDialog = true },
                            onNameLongClick = {
                                nameEditorIsNew = false
                                when (selectedTraining) {
                                    is Training.Individual -> showIndividualNameDialog = true
                                    is Training.Team -> showTeamNameDialog = true
                                }
                            },
                            onAddIndividualClick = {
                                nameEditorIsNew = true
                                showIndividualNameDialog = true
                            },
                            onAddTeamClick = {
                                nameEditorIsNew = true
                                showTeamNameDialog = true
                            },
                            onTrainingSelect = { trainingViewModel.selectTraining(it) }
                        )
                    }
                    AppScreen.Settings -> {
                        // SettingsScreen has its own top bar
                    }
                }

                when (currentScreen) {
                    AppScreen.Settings -> SettingsScreen(
                        channelGrid = channelGrid,
                        colorCount = colorCount,
                        isMuted = isMuted,
                        isUsbKeyboardEnabled = isUsbKeyboardEnabled,
                        appTheme = appTheme,
                        onChannelGridChange = { settingsViewModel.setChannelGrid(it) },
                        onColorCountChange = { settingsViewModel.setColorCount(it) },
                        onMutedChange = { settingsViewModel.setMuted(it) },
                        onUsbKeyboardChange = { settingsViewModel.setUsbKeyboardEnabled(it) },
                        onAppThemeChange = { settingsViewModel.setAppTheme(it) },
                        onNavigateBack = { pilotViewModel.navigateTo(AppScreen.Training) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                    else -> Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    (scaleIn(
                                        initialScale = 0.85f,
                                        animationSpec = tween(300)
                                    ) + fadeIn(animationSpec = tween(300))) togetherWith
                                    (scaleOut(
                                        targetScale = 0.85f,
                                        animationSpec = tween(300)
                                    ) + fadeOut(animationSpec = tween(300)))
                                },
                                label = "flight_training_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.Flight -> FlightContent(
                                        onNavigateBack = { pilotViewModel.navigateTo(AppScreen.Training) },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    AppScreen.Training -> {
                                        val isTeam = selectedTraining is Training.Team
                                        StatsContent(
                                            description = selectedTraining.description(LocalContext.current),
                                            onEditRulesClick = { showRulesEditor = true },
                                            isTeam = isTeam,
                                            pilot1Name = (selectedTraining as? Training.Team)?.pilot?.name1 ?: "",
                                            pilot2Name = (selectedTraining as? Training.Team)?.pilot?.name2 ?: "",
                                            pilotOrderSwapped = (selectedTraining as? Training.Team)?.rules?.pilotOrderSwapped ?: false,
                                            onSwapPilots = { trainingViewModel.swapPilotOrder() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }

                if (currentScreen != AppScreen.Settings) {
                    Column(
                        modifier = Modifier.navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(
                            visible = isUsbKeyboardEnabled && currentScreen != AppScreen.Flight,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            Text(
                                text = "Подключите USB-клавиатуру",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { settingsViewModel.setMuted(!isMuted) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute"
                                )
                            }
                            HoldButton(
                                onConfirm = {
                                    if (currentScreen == AppScreen.Flight) {
                                        pilotViewModel.navigateTo(AppScreen.Training)
                                    } else {
                                        pilotViewModel.navigateTo(AppScreen.Flight)
                                    }
                                },
                                text = if (currentScreen == AppScreen.Flight) "Стоп" else "Старт",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .height(54.dp)
                            )
                            IconButton(
                                onClick = { pilotViewModel.navigateTo(AppScreen.Settings) },
                                enabled = currentScreen != AppScreen.Flight,
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
        }
    }

    if (showChannelDialog) {
        ChannelDialog(
            currentChannel = currentPilot.channel,
            channelGrid = channelGrid,
            colorCount = colorCount,
            showApplyToAll = trainings.size > 1,
            onConfirm = { channel ->
                trainingViewModel.updateCurrentPilotChannel(channel)
                showChannelDialog = false
            },
            onConfirmForAll = { channel ->
                trainingViewModel.applyChannelToAll(channel)
                showChannelDialog = false
            },
            onDismiss = { showChannelDialog = false }
        )
    }

    if (showIndividualNameDialog) {
        val currentName = if (nameEditorIsNew) {
            ""
        } else {
            (selectedTraining as? Training.Individual)?.pilot?.name ?: ""
        }
        IndividualNameDialog(
            currentName = currentName,
            onConfirm = { name ->
                if (nameEditorIsNew) {
                    val defaultChannel = when (val last = trainings.lastOrNull()) {
                        is Training.Individual -> last.pilot.channel
                        is Training.Team -> last.pilot.channel
                        null -> Channel()
                    }
                    val newPilot = Pilot.Individual(name = name, channel = defaultChannel)
                    val newTraining = Training.Individual(pilot = newPilot)
                    trainingViewModel.addTraining(newTraining)
                    trainingViewModel.selectTraining(newTraining)
                } else {
                    trainingViewModel.updateCurrentPilotNames(name, "")
                }
                showIndividualNameDialog = false
            },
            onDismiss = { showIndividualNameDialog = false }
        )
    }

    if (showTeamNameDialog) {
        val currentName1 = if (nameEditorIsNew) {
            ""
        } else {
            (selectedTraining as? Training.Team)?.pilot?.name1 ?: ""
        }
        val currentName2 = if (nameEditorIsNew) {
            ""
        } else {
            (selectedTraining as? Training.Team)?.pilot?.name2 ?: ""
        }
        TeamNameDialog(
            name1 = currentName1,
            name2 = currentName2,
            onConfirm = { n1, n2 ->
                if (nameEditorIsNew) {
                    val defaultChannel = when (val last = trainings.lastOrNull()) {
                        is Training.Individual -> last.pilot.channel
                        is Training.Team -> last.pilot.channel
                        null -> Channel()
                    }
                    val newPilot = Pilot.Team(name1 = n1, name2 = n2, channel = defaultChannel)
                    val newTraining = Training.Team(pilot = newPilot)
                    trainingViewModel.addTraining(newTraining)
                    trainingViewModel.selectTraining(newTraining)
                } else {
                    trainingViewModel.updateCurrentPilotNames(n1, n2)
                }
                showTeamNameDialog = false
            },
            onDismiss = { showTeamNameDialog = false }
        )
    }

    if (showPilotEditor) {
        PilotEditorDialog(
            currentPilot = currentPilot,
            onConfirm = { pilot ->
                trainingViewModel.updatePilot(pilot)
                showPilotEditor = false
            },
            onDismiss = { showPilotEditor = false }
        )
    }

    if (showRulesEditor) {
        RulesEditorDialog(
            currentRules = selectedTraining.rules,
            onConfirm = { newRules ->
                trainingViewModel.updateTrainingRules(selectedTraining, newRules)
                showRulesEditor = false
            },
            onDismiss = { showRulesEditor = false }
        )
    }
}
