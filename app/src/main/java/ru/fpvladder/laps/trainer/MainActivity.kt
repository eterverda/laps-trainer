package ru.fpvladder.laps.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import kotlinx.coroutines.Job
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.ChannelResult
import java.time.LocalDate
import ru.fpvladder.laps.trainer.ui.components.ChannelDialog
import ru.fpvladder.laps.trainer.ui.components.ConfirmFinishTrainingDialog
import ru.fpvladder.laps.trainer.ui.components.ConfirmNameChangeDialog
import ru.fpvladder.laps.trainer.ui.components.ConfirmRulesChangeDialog
import ru.fpvladder.laps.trainer.ui.components.HoldButton
import ru.fpvladder.laps.trainer.ui.components.IndividualNameDialog
import ru.fpvladder.laps.trainer.ui.components.TeamNameDialog
import ru.fpvladder.laps.trainer.ui.components.RulesEditorDialog
import ru.fpvladder.laps.trainer.ui.components.TrainingHeader
import ru.fpvladder.laps.trainer.ui.screens.FlightScreen
import ru.fpvladder.laps.trainer.ui.screens.SettingsScreen
import ru.fpvladder.laps.trainer.ui.screens.StatsScreen
import ru.fpvladder.laps.trainer.ui.theme.LapsTrainerTheme
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.settings.USB_ENABLED
import ru.fpvladder.laps.trainer.settings.WIGGLE_ONCE_ENABLED
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.ui.helpers.description
import ru.fpvladder.laps.trainer.audio.SoundManager
import ru.fpvladder.laps.trainer.audio.STAGE_DURATION_MS
import ru.fpvladder.laps.trainer.audio.STAGE_DELAY_MS

import ru.fpvladder.laps.trainer.viewmodel.AppScreen
import ru.fpvladder.laps.trainer.viewmodel.FlightViewModel
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase
import ru.fpvladder.laps.trainer.viewmodel.KeyboardViewModel
import ru.fpvladder.laps.trainer.viewmodel.PilotViewModel
import ru.fpvladder.laps.trainer.viewmodel.SettingsViewModel
import ru.fpvladder.laps.trainer.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {

    private val keyboardViewModel: KeyboardViewModel by viewModels()
    private val pilotViewModel: PilotViewModel by viewModels()
    private val trainingViewModel: TrainingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val flightViewModel: FlightViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SoundManager.init(this)
        setContent {
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val darkThemeVariant by settingsViewModel.darkThemeVariant.collectAsState()
            LapsTrainerTheme(
                appTheme = appTheme,
                darkThemeVariant = darkThemeVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    AppRoot(
                        keyboardViewModel = keyboardViewModel,
                        pilotViewModel = pilotViewModel,
                        trainingViewModel = trainingViewModel,
                        settingsViewModel = settingsViewModel,
                        flightViewModel = flightViewModel,
                        modifier = Modifier.displayCutoutPadding(),
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
    settingsViewModel: SettingsViewModel,
    flightViewModel: FlightViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by pilotViewModel.currentScreen.collectAsState()
    val trainings by trainingViewModel.trainings.collectAsState()
    val selectedTraining by trainingViewModel.selectedTraining.collectAsState()
    val hasPagerWiggled by trainingViewModel.hasPagerWiggled.collectAsState()
    val channelGrid by settingsViewModel.channelGrid.collectAsState()
    val colorCount by settingsViewModel.colorCount.collectAsState()
    val isMuted by settingsViewModel.isMuted.collectAsState()
    val isUsbKeyboardEnabled by settingsViewModel.isUsbKeyboardEnabled.collectAsState()
    val effectiveUsbKeyboardEnabled by settingsViewModel.effectiveUsbKeyboardEnabled.collectAsState()
    val appTheme by settingsViewModel.appTheme.collectAsState()
    val darkThemeVariant by settingsViewModel.darkThemeVariant.collectAsState()
    val timerPrecision by settingsViewModel.timerPrecision.collectAsState()
    val effectiveStartSignal by settingsViewModel.effectiveStartSignal.collectAsState()
    val useLapButton by settingsViewModel.useLapButton.collectAsState()
    val useErrorFixButtons by settingsViewModel.useErrorFixButtons.collectAsState()
    val flightPhase by flightViewModel.flightPhase.collectAsState()
    val elapsedMs by flightViewModel.elapsedMs.collectAsState()
    val isPreBlinking by flightViewModel.isPreBlinking.collectAsState()

    val scope = rememberCoroutineScope()
    var soundJob by remember { mutableStateOf<Job?>(null) }
    var wasHoldConfirmed by remember { mutableStateOf(false) }

    val laps by flightViewModel.laps.collectAsState()
    val currentLap by flightViewModel.currentLap.collectAsState()
    val currentLapTime by flightViewModel.currentLapTime.collectAsState()
    val stopReason by flightViewModel.stopReason.collectAsState()
    val flightPilotChangeIndex by flightViewModel.pilotChangeIndex.collectAsState()
    val teamFlight by flightViewModel.teamFlight.collectAsState()
    val shouldSaveResult by flightViewModel.shouldSaveResult.collectAsState()
    val rotatePilotsForNextFlight by flightViewModel.rotatePilotsForNextFlight.collectAsState()

    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.Flight) {
            if (flightPhase == FlightPhase.IDLE) {
                flightViewModel.setRules(selectedTraining.rules)
                flightViewModel.prepareRace(effectiveStartSignal, isMuted)
            }
        } else {
            flightViewModel.reset()
        }
    }

    var showChannelDialog by remember { mutableStateOf(false) }
    var showIndividualNameDialog by remember { mutableStateOf(false) }
    var showTeamNameDialog by remember { mutableStateOf(false) }
    var nameEditorIsNew by remember { mutableStateOf(false) }
    var showRulesEditor by remember { mutableStateOf(false) }
    var pendingRulesChange by remember { mutableStateOf<Rules?>(null) }
    var pendingIndividualName by remember { mutableStateOf<String?>(null) }
    var pendingTeamName by remember { mutableStateOf<Pair<String, String>?>(null) }
    var trainingToFinish by remember { mutableStateOf<Training?>(null) }

    val changeRemainingMs = when (val r = selectedTraining.rules) {
        is Rules.Team -> if (r.changeMode == Rules.Team.ChangeMode.TIME) {
            (r.timeLimitSeconds * 1000L / 2 - elapsedMs).coerceAtLeast(0)
        } else null
        else -> null
    }

    val finishFlight: (Boolean, Boolean) -> Unit = { shouldSave, navigateToTraining ->
        if (shouldSave) {
            val flight = flightViewModel.buildFlight()
            trainingViewModel.addFlight(selectedTraining, flight)
        }
        if (selectedTraining is Training.Team && rotatePilotsForNextFlight) {
            trainingViewModel.rotatePilotOrder()
        }
        if (navigateToTraining) {
            pilotViewModel.navigateTo(AppScreen.Training)
        }
        flightViewModel.reset()
    }

    BackHandler(
        enabled = currentScreen == AppScreen.Settings || currentScreen == AppScreen.Flight
    ) {
        when (currentScreen) {
            AppScreen.Settings -> {
                pilotViewModel.navigateTo(AppScreen.Training)
            }

            AppScreen.Flight -> when (flightPhase) {
                FlightPhase.PRE_FLIGHT -> {
                    pilotViewModel.navigateTo(AppScreen.Training)
                    flightViewModel.reset()
                }

                FlightPhase.POST_FLIGHT -> {
                    finishFlight(shouldSaveResult, true)
                }

                else -> {}
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    AppScreen.Flight -> TrainingHeader(
                        trainings = trainings,
                        selectedTraining = selectedTraining,
                        enabled = flightPhase == FlightPhase.POST_FLIGHT,
                        onChannelClick = { showChannelDialog = true },
                        onNameClick = {
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
                        onTrainingSelect = {
                            finishFlight(shouldSaveResult, false)
                            trainingViewModel.selectTraining(it)
                            pilotViewModel.navigateTo(AppScreen.Training)
                        }
                    )

                    AppScreen.Training -> {
                        TrainingHeader(
                            trainings = trainings,
                            selectedTraining = selectedTraining,
                            onChannelClick = { showChannelDialog = true },
                            onNameClick = {
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
                        isUsbFeatureEnabled = USB_ENABLED,
                        useLapButton = useLapButton,
                        useErrorFixButtons = useErrorFixButtons,
                        appTheme = appTheme,
                        darkThemeVariant = darkThemeVariant,
                        timerPrecision = timerPrecision,
                        startSignal = effectiveStartSignal,
                        onChannelGridChange = { settingsViewModel.setChannelGrid(it) },
                        onColorCountChange = { settingsViewModel.setColorCount(it) },
                        onMutedChange = { settingsViewModel.setMuted(it) },
                        onUsbKeyboardChange = { settingsViewModel.setUsbKeyboardEnabled(it) },
                        onUseLapButtonChange = { settingsViewModel.setUseLapButton(it) },
                        onUseErrorFixButtonsChange = { settingsViewModel.setUseErrorFixButtons(it) },
                        onAppThemeChange = { settingsViewModel.setAppTheme(it) },
                        onDarkThemeVariantChange = { settingsViewModel.setDarkThemeVariant(it) },
                        onTimerPrecisionChange = { settingsViewModel.setTimerPrecision(it) },
                        onStartSignalChange = { settingsViewModel.setStartSignal(it) },
                        onNavigateBack = { pilotViewModel.navigateTo(AppScreen.Training) },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )

                    else -> Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 8.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                            },
                            label = "flight_training_transition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.Flight -> FlightScreen(
                                    flightPhase = flightPhase,
                                    startSignal = effectiveStartSignal,
                                    laps = laps,
                                    currentLap = currentLap,
                                    currentLapTime = currentLapTime,
                                    elapsedMs = elapsedMs,
                                    isPreBlinking = isPreBlinking,
                                    timeLimitSeconds = selectedTraining.rules.timeLimitSeconds,
                                    maxLaps = selectedTraining.rules.lapsLimit,
                                    stopReason = stopReason,
                                    showRecordKinds = selectedTraining.rules.showRecordKinds,
                                    holeshotEnabled = selectedTraining.rules.holeshotEnabled,
                                    shouldSaveResult = shouldSaveResult,
                                    onShouldSaveResultChange = { flightViewModel.setShouldSaveResult(it) },
                                    rotatePilotsForNextFlight = rotatePilotsForNextFlight,
                                    onRotatePilotsForNextFlightChange = { flightViewModel.setRotatePilotsForNextFlight(it) },
                                    useLapButton = useLapButton,
                                    useErrorFixButtons = useErrorFixButtons,
                                    onLapClick = {
                                        flightViewModel.addLap()
                                    },
                                    onErrorClick = { flightViewModel.addErrorToLastLap() },
                                    onFixClick = { flightViewModel.addFixToLastLap() },
                                    timerPrecision = timerPrecision,
                                    pilot = selectedTraining.pilot,
                                    pilotChangeIndex = flightPilotChangeIndex,
                                    teamFlight = teamFlight,
                                    swapMode = (selectedTraining as? Training.Team)?.rules?.swapMode ?: Rules.Team.SwapMode.STRAIGHT,
                                    changeRemainingMs = changeRemainingMs,
                                    hasPagerWiggled = if (WIGGLE_ONCE_ENABLED) hasPagerWiggled else false,
                                    onPagerWiggleComplete = { trainingViewModel.markPagerWiggled() },
                                    onBackClick = { finishFlight(shouldSaveResult, true) },
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppScreen.Training -> {
                                    AnimatedContent(
                                        targetState = selectedTraining,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(350)) +
                                                    scaleIn(
                                                        initialScale = 0.9f,
                                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                                    ))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(250)) +
                                                            scaleOut(
                                                                targetScale = 1.08f,
                                                                animationSpec = tween(300)
                                                            )
                                                )
                                        },
                                        contentKey = { it.id },
                                        label = "training_switch",
                                        modifier = Modifier.fillMaxSize()
                                    ) { training ->
                                        StatsScreen(
                                            description = training.description(LocalContext.current),
                                            onEditRulesClick = { showRulesEditor = true },
                                            stats = training.stats,
                                            showRecordKinds = training.rules.showRecordKinds,
                                            timerPrecision = timerPrecision,
                                            pilot = training.pilot,
                                            onRotatePilots = { trainingViewModel.rotatePilotOrder() },
                                            hasPagerWiggled = if (WIGGLE_ONCE_ENABLED) hasPagerWiggled else false,
                                            onPagerWiggleComplete = { trainingViewModel.markPagerWiggled() },
                                            isDefault = training.isDefault(),
                                            onDeleteClick = {
                                                if (training.isEmpty()) {
                                                    trainingViewModel.deleteTraining(training)
                                                } else {
                                                    trainingToFinish = training
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                else -> {}
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
                            visible = effectiveUsbKeyboardEnabled && currentScreen != AppScreen.Flight,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            Text(
                                text = "Подключите USB-клавиатуру",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val isOnFlight = currentScreen == AppScreen.Flight

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
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
                            val isManualPreStart =
                                isOnFlight && flightPhase == FlightPhase.PRE_FLIGHT && effectiveStartSignal == StartSignal.MANUAL
                            val isPreFixedOrRandom =
                                isOnFlight && flightPhase == FlightPhase.PRE_FLIGHT && effectiveStartSignal != StartSignal.MANUAL
                            val isPostFlight = flightPhase == FlightPhase.POST_FLIGHT

                            val buttonText = when {
                                !isOnFlight || isPostFlight -> "Старт"
                                isManualPreStart -> "GO GO GO"
                                isPreFixedOrRandom -> "ОТМЕНА"
                                else -> "Стоп"
                            }
                            val holdDurationMs = when {
                                !isOnFlight || isPostFlight -> if (isMuted) 1800 else (3 * STAGE_DURATION_MS + 2 * STAGE_DELAY_MS).toInt()
                                isManualPreStart || isPreFixedOrRandom -> 0
                                else -> 1200
                            }


                            HoldButton(
                                onConfirm = {
                                    wasHoldConfirmed = true
                                    when {
                                        !isOnFlight -> pilotViewModel.navigateTo(AppScreen.Flight)
                                        isManualPreStart -> flightViewModel.manualStart(isMuted)
                                        isPreFixedOrRandom -> {
                                            pilotViewModel.navigateTo(AppScreen.Training)
                                            flightViewModel.reset()
                                        }

                                        flightPhase == FlightPhase.FLIGHT -> {
                                            flightViewModel.stopRace(isMuted)
                                        }

                                        isPostFlight -> {
                                            finishFlight(shouldSaveResult, false)
                                            flightViewModel.setRules(selectedTraining.rules)
                                            flightViewModel.prepareRace(effectiveStartSignal, isMuted)
                                        }

                                        else -> {
                                            pilotViewModel.navigateTo(AppScreen.Training)
                                            flightViewModel.reset()
                                        }
                                    }
                                },
                                text = buttonText,
                                holdDurationMs = holdDurationMs,
                                onPressStart = {
                                    wasHoldConfirmed = false
                                    if (!isMuted && (!isOnFlight || isPostFlight)) {
                                        soundJob = SoundManager.playStageSequence(scope)
                                    }
                                },
                                onPressEnd = {
                                    soundJob?.cancel()
                                    if (!wasHoldConfirmed) {
                                        SoundManager.stop()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (currentScreen == AppScreen.Flight && flightPhase == FlightPhase.POST_FLIGHT) {
                                        finishFlight(shouldSaveResult, false)
                                    }
                                    pilotViewModel.navigateTo(AppScreen.Settings)
                                },
                                enabled = currentScreen != AppScreen.Flight || isPostFlight,
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
            currentChannel = selectedTraining.pilot.channel,
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
            isEmpty = nameEditorIsNew || selectedTraining.isEmpty(),
            onConfirm = { name ->
                if (nameEditorIsNew) {
                    val defaultChannel = trainings.lastOrNull()?.pilot?.channel ?: Channel()
                    val newPilot = Pilot.Individual(name = name, channel = defaultChannel)
                    val newTraining = Training.Individual(pilot = newPilot)
                    trainingViewModel.addTraining(newTraining)
                    trainingViewModel.selectTraining(newTraining)
                } else {
                    trainingViewModel.updateCurrentPilotNames(name, "")
                }
                showIndividualNameDialog = false
            },
            onRequestConfirm = { name ->
                pendingIndividualName = name
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
            isEmpty = nameEditorIsNew || selectedTraining.isEmpty(),
            onConfirm = { n1, n2 ->
                if (nameEditorIsNew) {
                    val defaultChannel = trainings.lastOrNull()?.pilot?.channel ?: Channel()
                    val newPilot = Pilot.Team(name1 = n1, name2 = n2, channel = defaultChannel)
                    val newTraining = Training.Team(pilot = newPilot)
                    trainingViewModel.addTraining(newTraining)
                    trainingViewModel.selectTraining(newTraining)
                } else {
                    trainingViewModel.updateCurrentPilotNames(n1, n2)
                }
                showTeamNameDialog = false
            },
            onRequestConfirm = { n1, n2 ->
                pendingTeamName = n1 to n2
                showTeamNameDialog = false
            },
            onDismiss = { showTeamNameDialog = false }
        )
    }

    pendingIndividualName?.let { name ->
        ConfirmNameChangeDialog(
            message = "Имя изменено. Вы уверены что хотите продолжить текущую тренировку с новым пилотом?",
            onContinue = {
                trainingViewModel.updateCurrentPilotNames(name, "")
                pendingIndividualName = null
            },
            onCreateNew = {
                val wasPostFlight = currentScreen == AppScreen.Flight && flightPhase == FlightPhase.POST_FLIGHT
                if (wasPostFlight) {
                    finishFlight(shouldSaveResult, false)
                }
                val pilot = Pilot.Individual(name = name, channel = selectedTraining.pilot.channel)
                val newTraining = Training.Individual(pilot = pilot, date = LocalDate.now())
                    .copy(rules = selectedTraining.rules as Rules.Individual)
                trainingViewModel.addTraining(newTraining)
                if (wasPostFlight) {
                    pilotViewModel.navigateTo(AppScreen.Training)
                }
                pendingIndividualName = null
            },
            onDismiss = { pendingIndividualName = null }
        )
    }

    pendingTeamName?.let { (n1, n2) ->
        val currentTeam = selectedTraining.pilot as? Pilot.Team
        val changedNames = listOfNotNull(
            currentTeam?.name1?.takeIf { it != n1 },
            currentTeam?.name2?.takeIf { it != n2 }
        ).size
        val teamMessage = if (changedNames >= 2) {
            "Имена изменены. Вы уверены что хотите продолжить текущую тренировку с новой командой?"
        } else {
            "Имя изменено. Вы уверены что хотите продолжить текущую тренировку с новым пилотом?"
        }
        ConfirmNameChangeDialog(
            message = teamMessage,
            onContinue = {
                trainingViewModel.updateCurrentPilotNames(n1, n2)
                pendingTeamName = null
            },
            onCreateNew = {
                val wasPostFlight = currentScreen == AppScreen.Flight && flightPhase == FlightPhase.POST_FLIGHT
                if (wasPostFlight) {
                    finishFlight(shouldSaveResult, false)
                }
                val pilot = Pilot.Team(name1 = n1, name2 = n2, channel = selectedTraining.pilot.channel)
                val newTraining = Training.Team(pilot = pilot, date = LocalDate.now())
                    .copy(rules = selectedTraining.rules as Rules.Team)
                trainingViewModel.addTraining(newTraining)
                if (wasPostFlight) {
                    pilotViewModel.navigateTo(AppScreen.Training)
                }
                pendingTeamName = null
            },
            onDismiss = { pendingTeamName = null }
        )
    }

    if (showRulesEditor) {
        RulesEditorDialog(
            currentRules = selectedTraining.rules,
            isEmpty = selectedTraining.isEmpty(),
            onConfirm = { newRules ->
                trainingViewModel.updateTrainingRules(selectedTraining, newRules)
                showRulesEditor = false
            },
            onRequestConfirm = { newRules ->
                pendingRulesChange = newRules
                showRulesEditor = false
            },
            onDismiss = { showRulesEditor = false }
        )
    }

    pendingRulesChange?.let { newRules ->
        ConfirmRulesChangeDialog(
            onContinue = {
                trainingViewModel.updateTrainingRules(selectedTraining, newRules)
                pendingRulesChange = null
            },
            onCreateNew = {
                val pilot = selectedTraining.pilot
                val newTraining = when (pilot) {
                    is Pilot.Individual -> Training.Individual(
                        pilot = pilot.copy(),
                        date = LocalDate.now()
                    ).copy(rules = newRules as Rules.Individual)

                    is Pilot.Team -> Training.Team(
                        pilot = pilot.copy(),
                        date = LocalDate.now()
                    ).copy(rules = newRules as Rules.Team)
                }
                trainingViewModel.addTraining(newTraining)
                pendingRulesChange = null
            },
            onDismiss = { pendingRulesChange = null }
        )
    }

    trainingToFinish?.let { training ->
        ConfirmFinishTrainingDialog(
            onConfirm = {
                trainingViewModel.deleteTraining(training)
                trainingToFinish = null
            },
            onDismiss = { trainingToFinish = null }
        )
    }
}
