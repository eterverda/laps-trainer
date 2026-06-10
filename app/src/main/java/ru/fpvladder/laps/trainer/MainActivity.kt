package ru.fpvladder.laps.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
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
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.computeFlightCounters
import ru.fpvladder.laps.trainer.model.computeFlightRecords
import ru.fpvladder.laps.trainer.model.mergeFlightRecords
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.description
import ru.fpvladder.laps.trainer.audio.SoundManager
import ru.fpvladder.laps.trainer.audio.STAGE_DURATION_MS
import ru.fpvladder.laps.trainer.audio.STAGE_DELAY_MS

import ru.fpvladder.laps.trainer.viewmodel.AppScreen
import ru.fpvladder.laps.trainer.viewmodel.FlightPhase
import ru.fpvladder.laps.trainer.viewmodel.FlightViewModel
import ru.fpvladder.laps.trainer.viewmodel.KeyboardViewModel
import ru.fpvladder.laps.trainer.viewmodel.PilotViewModel
import ru.fpvladder.laps.trainer.ui.screens.formatCountdown
import ru.fpvladder.laps.trainer.ui.screens.formatTime
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
            LapsTrainerTheme(appTheme = appTheme) {
                AppRoot(
                    keyboardViewModel = keyboardViewModel,
                    pilotViewModel = pilotViewModel,
                    trainingViewModel = trainingViewModel,
                    settingsViewModel = settingsViewModel,
                    flightViewModel = flightViewModel
                )
            }
        }
    }
}

@Composable
fun FlightTimer(
    phase: FlightPhase,
    startSignal: StartSignal,
    elapsedMs: Long,
    preStartCountdownMs: Long,
    isPreBlinking: Boolean,
    timerPrecision: TimerPrecision,
    timeLimitSeconds: Int
) {
    val isBlinking = phase == FlightPhase.PRE && isPreBlinking
    val alpha by animateFloatAsState(
        targetValue = if (isBlinking) 0f else 1f,
        animationSpec = tween(200),
        label = "timer_blink"
    )

    val timeText = if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) {
        formatCountdown(preStartCountdownMs, timerPrecision) + " "
    } else {
        formatTime(
            if (phase == FlightPhase.PRE) 0L else elapsedMs,
            timerPrecision,
            timeLimitSeconds
        )
    }

    val annotatedText = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(timeText)
        }
        if (timeLimitSeconds != Int.MAX_VALUE) {
            val remainingMs = (timeLimitSeconds * 1000L - elapsedMs).coerceAtLeast(0)

            withStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            ) {
                append("\n")
                append(formatCountdown(remainingMs, timerPrecision))
            }
        }
    }

    Text(
        text = annotatedText,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .alpha(if (phase == FlightPhase.PRE && startSignal == StartSignal.FIXED) 1f else alpha),
        textAlign = TextAlign.Center
    )
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
    val channelGrid by settingsViewModel.channelGrid.collectAsState()
    val colorCount by settingsViewModel.colorCount.collectAsState()
    val isMuted by settingsViewModel.isMuted.collectAsState()
    val isUsbKeyboardEnabled by settingsViewModel.isUsbKeyboardEnabled.collectAsState()
    val appTheme by settingsViewModel.appTheme.collectAsState()
    val timerPrecision by settingsViewModel.timerPrecision.collectAsState()
    val startSignal by settingsViewModel.startSignal.collectAsState()
    val useErrorFixButtons by settingsViewModel.useErrorFixButtons.collectAsState()
    val flightPhase by flightViewModel.phase.collectAsState()
    val isStopping by flightViewModel.isStopping.collectAsState()
    val elapsedMs by flightViewModel.elapsedMs.collectAsState()
    val preStartTime by flightViewModel.preStartTime.collectAsState()
    val isPreBlinking by flightViewModel.isPreBlinking.collectAsState()
    val flightTimerPrecision by flightViewModel.timerPrecision.collectAsState()
    val scope = rememberCoroutineScope()
    var soundJob by remember { mutableStateOf<Job?>(null) }

    val laps by flightViewModel.laps.collectAsState()
    val currentLap by flightViewModel.currentLap.collectAsState()
    val currentLapTime by flightViewModel.currentLapTime.collectAsState()
    val stopReason by flightViewModel.stopReason.collectAsState()
    val preStartCountdownMs by flightViewModel.preStartCountdownMs.collectAsState()

    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.Flight) {
            flightViewModel.setRules(selectedTraining.rules)
            flightViewModel.setTimerPrecision(timerPrecision)
            flightViewModel.prepareRace(startSignal, isMuted)
        } else {
            flightViewModel.reset()
        }
    }

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

    BackHandler(
        enabled = currentScreen == AppScreen.Settings || currentScreen == AppScreen.Flight
    ) {
        when {
            currentScreen == AppScreen.Settings -> {
                pilotViewModel.navigateTo(AppScreen.Training)
            }

            currentScreen == AppScreen.Flight && flightPhase == FlightPhase.PRE -> {
                pilotViewModel.navigateTo(AppScreen.Training)
                flightViewModel.reset()
            }

            currentScreen == AppScreen.Flight && flightPhase == FlightPhase.POST -> {
                pilotViewModel.navigateTo(AppScreen.Training)
                flightViewModel.reset()
            }
            // MAIN — back игнорируется (enabled=true, но ничего не делаем)
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

                AnimatedVisibility(
                    visible = currentScreen == AppScreen.Flight && flightPhase != FlightPhase.POST,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FlightTimer(
                            phase = flightPhase,
                            startSignal = startSignal,
                            elapsedMs = elapsedMs,
                            preStartCountdownMs = preStartCountdownMs,
                            isPreBlinking = isPreBlinking,
                            timerPrecision = flightTimerPrecision,
                            timeLimitSeconds = selectedTraining.rules.timeLimitSeconds
                        )
                    }
                }

                when (currentScreen) {
                    AppScreen.Settings -> SettingsScreen(
                        channelGrid = channelGrid,
                        colorCount = colorCount,
                        isMuted = isMuted,
                        isUsbKeyboardEnabled = isUsbKeyboardEnabled,
                        useErrorFixButtons = useErrorFixButtons,
                        appTheme = appTheme,
                        timerPrecision = timerPrecision,
                        startSignal = startSignal,
                        onChannelGridChange = { settingsViewModel.setChannelGrid(it) },
                        onColorCountChange = { settingsViewModel.setColorCount(it) },
                        onMutedChange = { settingsViewModel.setMuted(it) },
                        onUsbKeyboardChange = { settingsViewModel.setUsbKeyboardEnabled(it) },
                        onUseErrorFixButtonsChange = { settingsViewModel.setUseErrorFixButtons(it) },
                        onAppThemeChange = { settingsViewModel.setAppTheme(it) },
                        onTimerPrecisionChange = { settingsViewModel.setTimerPrecision(it) },
                        onStartSignalChange = { settingsViewModel.setStartSignal(it) },
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
                                        phase = flightPhase,
                                        startSignal = startSignal,
                                        laps = laps,
                                        currentLap = currentLap,
                                        currentLapTime = currentLapTime,
                                        elapsedMs = elapsedMs,
                                        timeLimitSeconds = selectedTraining.rules.timeLimitSeconds,
                                        maxLaps = selectedTraining.rules.maxLaps,
                                        stopReason = stopReason,
                                        enabledBestLapKinds = when (val r = selectedTraining.rules) {
                                            is Rules.Individual -> r.enabledBestLapKinds
                                            is Rules.Team -> r.enabledBestLapKinds
                                            else -> emptySet()
                                        },
                                        onSave = {
                                            val currentStats = selectedTraining.stats
                                            if (currentStats is Stats.Individual) {
                                                val training = selectedTraining as Training.Individual
                                                val records = computeFlightRecords(
                                                    laps,
                                                    flightTimerPrecision,
                                                    training.rules.enabledBestLapKinds
                                                )
                                                val flightCounters = computeFlightCounters(laps)
                                                val updatedStats = currentStats.mergeFlightRecords(
                                                    records,
                                                    flightCounters,
                                                )
                                                trainingViewModel.updateTrainingStats(selectedTraining, updatedStats)
                                            }
                                            pilotViewModel.navigateTo(AppScreen.Training)
                                            flightViewModel.reset()
                                        },
                                        onDiscard = {
                                            pilotViewModel.navigateTo(AppScreen.Training)
                                            flightViewModel.reset()
                                        },
                                        onLapClick = {
                                            flightViewModel.addLap()
                                        },
                                        timerPrecision = flightTimerPrecision,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    AppScreen.Training -> {
                                        val isTeam = selectedTraining is Training.Team
                                        StatsContent(
                                            description = selectedTraining.description(LocalContext.current),
                                            onEditRulesClick = { showRulesEditor = true },
                                            stats = selectedTraining.stats,
                                            timerPrecision = flightTimerPrecision,
                                            isTeam = isTeam,
                                            pilot1Name = (selectedTraining as? Training.Team)?.pilot?.name1
                                                ?: "",
                                            pilot2Name = (selectedTraining as? Training.Team)?.pilot?.name2
                                                ?: "",
                                            pilotOrderSwapped = (selectedTraining as? Training.Team)?.rules?.pilotOrderSwapped
                                                ?: false,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        val isOnFlight = currentScreen == AppScreen.Flight

                        AnimatedVisibility(
                            visible = isOnFlight && flightPhase == FlightPhase.MAIN && useErrorFixButtons,
                            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    16.dp,
                                    Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { flightViewModel.addErrorToLastLap() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_cross),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ОШИБКА", fontSize = 14.sp)
                                }
                                Button(
                                    onClick = { flightViewModel.addFixToLastLap() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_square),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ИСПРАВИЛ", fontSize = 14.sp)
                                }
                            }
                        }

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
                                isOnFlight && flightPhase == FlightPhase.PRE && startSignal == StartSignal.MANUAL
                            val isPreFixedOrRandom =
                                isOnFlight && flightPhase == FlightPhase.PRE && startSignal != StartSignal.MANUAL

                            val buttonText = when {
                                !isOnFlight -> "Старт"
                                isManualPreStart -> "GO GO GO"
                                isPreFixedOrRandom -> "ОТМЕНА"
                                flightPhase == FlightPhase.POST -> "Старт"
                                else -> "Стоп"
                            }
                            val holdDurationMs = when {
                                !isOnFlight -> if (isMuted) 2000 else (3 * STAGE_DURATION_MS + 2 * STAGE_DELAY_MS).toInt()
                                isManualPreStart || isPreFixedOrRandom -> 0
                                flightPhase == FlightPhase.POST -> if (isMuted) 2000 else (3 * STAGE_DURATION_MS + 2 * STAGE_DELAY_MS).toInt()
                                flightPhase == FlightPhase.MAIN -> 1200
                                else -> 2000
                            }


                            HoldButton(
                                onConfirm = {
                                    when {
                                        !isOnFlight -> pilotViewModel.navigateTo(AppScreen.Flight)
                                        isManualPreStart -> flightViewModel.manualStart(isMuted)
                                        isPreFixedOrRandom -> {
                                            pilotViewModel.navigateTo(AppScreen.Training)
                                            flightViewModel.reset()
                                        }

                                        flightPhase == FlightPhase.MAIN -> {
                                            flightViewModel.stopRace(isMuted)
                                        }

                                        flightPhase == FlightPhase.POST -> {
                                            flightViewModel.reset()
                                            flightViewModel.setRules(selectedTraining.rules)
                                            flightViewModel.prepareRace(startSignal, isMuted)
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
                                    if (!isMuted && (!isOnFlight || flightPhase == FlightPhase.POST)) {
                                        soundJob = SoundManager.playStageSequence(scope)
                                    }
                                },
                                onPressEnd = {
                                    if (!isStopping) {
                                        soundJob?.cancel()
                                        SoundManager.stop()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .height(54.dp)
                            )
                            IconButton(
                                onClick = { pilotViewModel.navigateTo(AppScreen.Settings) },
                                enabled = currentScreen != AppScreen.Flight || flightPhase == FlightPhase.POST,
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
