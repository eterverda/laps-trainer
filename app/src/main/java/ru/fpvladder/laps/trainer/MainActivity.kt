package ru.fpvladder.laps.trainer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ru.fpvladder.laps.trainer.ui.components.ChannelDialog
import ru.fpvladder.laps.trainer.ui.components.ConfirmFinishTrainingDialog
import ru.fpvladder.laps.trainer.ui.components.ConfirmRulesChangeDialog
import ru.fpvladder.laps.trainer.ui.components.HoldButton
import ru.fpvladder.laps.trainer.ui.components.KeyboardSetupDialog

import ru.fpvladder.laps.trainer.ui.components.PilotNameDialog
import ru.fpvladder.laps.trainer.ui.components.PilotNameDialogResult
import ru.fpvladder.laps.trainer.ui.components.PilotNameDialogState
import ru.fpvladder.laps.trainer.ui.components.RulesEditorDialog
import ru.fpvladder.laps.trainer.ui.components.TrainingHeader
import ru.fpvladder.laps.trainer.ui.screens.FlightScreen
import ru.fpvladder.laps.trainer.ui.screens.SettingsScreen
import ru.fpvladder.laps.trainer.ui.screens.StatsScreen
import ru.fpvladder.laps.trainer.ui.theme.LapsTrainerTheme
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.settings.AppThemeMode
import ru.fpvladder.laps.trainer.settings.DefaultRules
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.settings.USB_ENABLED
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.ui.helpers.description
import ru.fpvladder.laps.trainer.audio.SoundManager
import ru.fpvladder.laps.trainer.audio.STAGE_DURATION_MS
import ru.fpvladder.laps.trainer.audio.STAGE_DELAY_MS
import ru.fpvladder.laps.trainer.usb.UsbHidAction
import ru.fpvladder.laps.trainer.usb.UsbHidEvent
import ru.fpvladder.laps.trainer.usb.UsbHidConfig
import ru.fpvladder.laps.trainer.usb.UsbHidManager
import ru.fpvladder.laps.trainer.usb.UsbHidState

import ru.fpvladder.laps.trainer.viewmodel.AppScreen
import ru.fpvladder.laps.trainer.viewmodel.FlightViewModel
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase
import ru.fpvladder.laps.trainer.viewmodel.PilotViewModel
import ru.fpvladder.laps.trainer.viewmodel.SettingsViewModel
import ru.fpvladder.laps.trainer.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {

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

            val isDark = when (appTheme) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

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
    pilotViewModel: PilotViewModel,
    trainingViewModel: TrainingViewModel,
    settingsViewModel: SettingsViewModel,
    flightViewModel: FlightViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by pilotViewModel.currentScreen.collectAsState()
    val trainings by trainingViewModel.trainings.collectAsState()
    val selectedTraining by trainingViewModel.selectedTraining.collectAsState()
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
    val usePitstopButton by settingsViewModel.usePitstopButton.collectAsState()
    val flightPhase by flightViewModel.flightPhase.collectAsState()
    val elapsedMs by flightViewModel.elapsedMs.collectAsState()

    val context = LocalContext.current
    val keyboardManager = remember { UsbHidManager.getInstance(context) }
    val keyboardState by keyboardManager.state.collectAsState()
    val keyboardConfigs by keyboardManager.configs.collectAsState()
    val connectedUsbDevices = (keyboardState as? UsbHidState.Connected)?.devices ?: emptySet()
    val startButtonPressed = remember { MutableStateFlow(false) }
    val lapButtonPressed = remember { MutableStateFlow(false) }
    val errorButtonPressed = remember { MutableStateFlow(false) }
    val fixButtonPressed = remember { MutableStateFlow(false) }
    val pitstopButtonPressed = remember { MutableStateFlow(false) }
    var editingKeyboardConfig by remember { mutableStateOf<UsbHidConfig?>(null) }

    LaunchedEffect(isUsbKeyboardEnabled) {
        keyboardManager.setUserEnabled(isUsbKeyboardEnabled)
    }

    LaunchedEffect(currentScreen, flightPhase) {
        val discoveryAllowed = when (currentScreen) {
            AppScreen.Settings,
            AppScreen.Training -> true
            AppScreen.Flight -> flightPhase != FlightPhase.PRE_FLIGHT && flightPhase != FlightPhase.FLIGHT
        }
        keyboardManager.setDiscoveryAllowed(discoveryAllowed)
    }

    LaunchedEffect(Unit) {
        keyboardManager.keyEvents.collect { event ->
            if (keyboardManager.state.value is UsbHidState.Setup) return@collect
            val isDown = event.state == UsbHidEvent.STATE_DOWN
            when (event.action) {
                UsbHidAction.START -> startButtonPressed.value = isDown
                UsbHidAction.LAP -> {
                    if (pilotViewModel.currentScreen.value == AppScreen.Flight && settingsViewModel.useLapButton.value) {
                        lapButtonPressed.value = isDown
                    } else if (isDown) {
                        flightViewModel.addLap(settingsViewModel.isMuted.value)
                    }
                }
                UsbHidAction.ERROR -> {
                    if (pilotViewModel.currentScreen.value == AppScreen.Flight && settingsViewModel.useErrorFixButtons.value) {
                        errorButtonPressed.value = isDown
                    } else if (isDown) {
                        flightViewModel.addErrorToLastLap()
                    }
                }
                UsbHidAction.FIX -> {
                    if (pilotViewModel.currentScreen.value == AppScreen.Flight && settingsViewModel.useErrorFixButtons.value) {
                        fixButtonPressed.value = isDown
                    } else if (isDown) {
                        flightViewModel.addFixToLastLap()
                    }
                }
                UsbHidAction.PITSTOP -> {
                    val pitstopButtonVisible = settingsViewModel.usePitstopButton.value &&
                            trainingViewModel.selectedTraining.value is Training.Team
                    if (pilotViewModel.currentScreen.value == AppScreen.Flight && pitstopButtonVisible) {
                        pitstopButtonPressed.value = isDown
                    } else if (isDown) {
                        flightViewModel.addPitstopToLastLap()
                    }
                }
                UsbHidAction.UNDO -> {
                    if (isDown) {
                        flightViewModel.undoLastAction()
                    }
                }
                null -> {}
            }
        }
    }
    val isPreBlinking by flightViewModel.isPreBlinking.collectAsState()

    val scope = rememberCoroutineScope()
    var soundJob by remember { mutableStateOf<Job?>(null) }
    var wasHoldConfirmed by remember { mutableStateOf(false) }

    val laps by flightViewModel.laps.collectAsState()
    val lapMarks by flightViewModel.lapMarks.collectAsState()
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
                flightViewModel.prepareRace(effectiveStartSignal, settingsViewModel.isMuted)
            }
        } else {
            flightViewModel.reset()
        }
    }

    var showChannelDialog by remember { mutableStateOf(false) }
    var pilotNameDialogState by remember { mutableStateOf<PilotNameDialogState?>(null) }
    var showRulesEditor by remember { mutableStateOf(false) }
    var pendingRulesChange by remember { mutableStateOf<Rules?>(null) }
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

    val createNewTrainingFromPilot: (Pilot) -> Unit = { pilot ->
        val wasPostFlight = currentScreen == AppScreen.Flight && flightPhase == FlightPhase.POST_FLIGHT
        if (wasPostFlight) {
            finishFlight(shouldSaveResult, false)
        }
        val channel = selectedTraining.pilot.channel
        val newTraining = when (pilot) {
            is Pilot.Individual -> Training.Individual(
                pilot = pilot.copy(channel = channel),
                rules = DefaultRules.INDIVIDUAL
            )
            is Pilot.Team -> Training.Team(
                pilot = pilot.copy(channel = channel),
                rules = DefaultRules.TEAM
            )
        }
        trainingViewModel.addTraining(newTraining)
        trainingViewModel.selectTraining(newTraining)
        if (wasPostFlight) {
            pilotViewModel.navigateTo(AppScreen.Training)
        }
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
                            pilotNameDialogState = PilotNameDialogState.Rename(selectedTraining.pilot)
                        },
                        onAddIndividualClick = {
                            pilotNameDialogState = PilotNameDialogState.Create(Pilot.Individual.ANONYMOUS)
                        },
                        onAddTeamClick = {
                            pilotNameDialogState = PilotNameDialogState.Create(Pilot.Team.ANONYMOUS)
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
                                pilotNameDialogState = PilotNameDialogState.Rename(selectedTraining.pilot)
                            },
                            onAddIndividualClick = {
                                pilotNameDialogState = PilotNameDialogState.Create(Pilot.Individual.ANONYMOUS)
                            },
                            onAddTeamClick = {
                                pilotNameDialogState = PilotNameDialogState.Create(Pilot.Team.ANONYMOUS)
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
                        connectedUsbDevices = connectedUsbDevices,
                        keyboardConfigs = keyboardConfigs,
                        onConfigureKeyboard = { editingKeyboardConfig = it },
                        useLapButton = useLapButton,
                        useErrorFixButtons = useErrorFixButtons,
                        usePitstopButton = usePitstopButton,
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
                        onUsePitstopButtonChange = { settingsViewModel.setUsePitstopButton(it) },
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
                                    usePitstopButton = usePitstopButton && selectedTraining is Training.Team,
                                    lapButtonPressed = lapButtonPressed,
                                    errorButtonPressed = errorButtonPressed,
                                    fixButtonPressed = fixButtonPressed,
                                    pitstopButtonPressed = pitstopButtonPressed,
                                    onLapClick = {
                                        flightViewModel.addLap(isMuted)
                                    },
                                    onErrorClick = { flightViewModel.addErrorToLastLap() },
                                    onFixClick = { flightViewModel.addFixToLastLap() },
                                    onPitstopClick = { flightViewModel.addPitstopToLastLap() },
                                    timerPrecision = timerPrecision,
                                    lapMarks = lapMarks,
                                    pilot = selectedTraining.pilot,
                                    pilotChangeIndex = flightPilotChangeIndex,
                                    teamFlight = teamFlight,
                                    swapMode = (selectedTraining as? Training.Team)?.rules?.swapMode ?: Rules.Team.SwapMode.STRAIGHT,
                                    changeRemainingMs = changeRemainingMs,
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
                            visible = effectiveUsbKeyboardEnabled && currentScreen != AppScreen.Flight && connectedUsbDevices.isEmpty(),
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddLink,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp).rotate(180f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Подключите USB-клавиатуру",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val isOnFlight = currentScreen == AppScreen.Flight

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
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
                                !isOnFlight || isPostFlight -> if (isMuted) 1800 else (3 * STAGE_DURATION_MS + 2 * STAGE_DELAY_MS - 405).toInt()
                                isManualPreStart || isPreFixedOrRandom -> 0
                                else -> 900
                            }


                            HoldButton(
                                onConfirm = {
                                    wasHoldConfirmed = true
                                    when {
                                        !isOnFlight -> pilotViewModel.navigateTo(AppScreen.Flight)
                                        isManualPreStart -> flightViewModel.manualStart(settingsViewModel.isMuted)
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
                                            flightViewModel.prepareRace(effectiveStartSignal, settingsViewModel.isMuted)
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
                                controllerPressed = startButtonPressed,
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

    pilotNameDialogState?.let { state ->
        PilotNameDialog(
            state = state,
            onResult = { result ->
                when (result) {
                    is PilotNameDialogResult.Rename -> {
                        when (val p = result.pilot) {
                            is Pilot.Individual -> trainingViewModel.updateCurrentPilotNames(p.name, "")
                            is Pilot.Team -> trainingViewModel.updateCurrentPilotNames(p.name1, p.name2)
                        }
                    }
                    is PilotNameDialogResult.Create -> {
                        createNewTrainingFromPilot(result.pilot)
                    }
                }
                pilotNameDialogState = null
            },
            onDismiss = { pilotNameDialogState = null }
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
                        pilot = pilot,
                        rules = newRules as Rules.Individual
                    )

                    is Pilot.Team -> Training.Team(
                        pilot = pilot,
                        rules = newRules as Rules.Team
                    )
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

    val setupState = keyboardState as? UsbHidState.Setup
    setupState?.let { state ->
        val config = keyboardManager.configs.value.find { it.identity == state.info.identity }
            ?: UsbHidConfig(
                info = state.info,
                bindings = emptySet(),
            )
        KeyboardSetupDialog(
            config = config,
            keyEvents = keyboardManager.keyEvents,
            onSave = { newConfig ->
                keyboardManager.saveConfig(newConfig)
                keyboardManager.onSetupConfirmed(state.deviceName)
            },
            onCancel = { keyboardManager.onSetupCancelled(state.deviceName) },
        )
    }

    editingKeyboardConfig?.let { config ->
        val isConnected = connectedUsbDevices.any { it.identity == config.identity }
        KeyboardSetupDialog(
            config = config,
            keyEvents = keyboardManager.keyEvents,
            showDeletePage = !isConnected,
            allowEmptySave = true,
            onSave = { newConfig ->
                keyboardManager.saveConfig(newConfig)
                editingKeyboardConfig = null
            },
            onDelete = {
                keyboardManager.removeConfig(config.identity)
                editingKeyboardConfig = null
            },
            onCancel = { editingKeyboardConfig = null },
        )
    }
}
