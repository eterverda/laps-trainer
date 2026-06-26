package ru.fpvladder.laps.trainer.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.audio.BUZZER_DURATION_MS
import ru.fpvladder.laps.trainer.audio.SoundManager
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.NO_PILOT_CHANGE
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.settings.DefaultRules
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight
import kotlin.random.Random

class FlightViewModel : ViewModel() {

    private val _flightPhase = MutableStateFlow(FlightPhase.IDLE)
    val flightPhase: StateFlow<FlightPhase> = _flightPhase.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _isPreBlinking = MutableStateFlow(false)
    val isPreBlinking: StateFlow<Boolean> = _isPreBlinking.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    private val _currentLap = MutableStateFlow<Lap?>(null)
    val currentLap: StateFlow<Lap?> = _currentLap.asStateFlow()

    private val _currentLapTime = MutableStateFlow(0L)
    val currentLapTime: StateFlow<Long> = _currentLapTime.asStateFlow()

    private val _stopReason = MutableStateFlow<StopReason?>(null)
    val stopReason: StateFlow<StopReason?> = _stopReason.asStateFlow()

    private val _shouldSaveResult = MutableStateFlow(false)
    val shouldSaveResult: StateFlow<Boolean> = _shouldSaveResult.asStateFlow()

    private val _rotatePilotsForNextFlight = MutableStateFlow(false)
    val rotatePilotsForNextFlight: StateFlow<Boolean> = _rotatePilotsForNextFlight.asStateFlow()

    private var timerJob: Job? = null
    private var preJob: Job? = null
    private var nextLapNumber = 0
    private var rules: Rules = DefaultRules.INDIVIDUAL
    private var lastLapElapsedMs = 0L

    private val _pilotChangeIndex = MutableStateFlow(NO_PILOT_CHANGE)
    val pilotChangeIndex: StateFlow<Int> = _pilotChangeIndex.asStateFlow()

    private val _teamFlight = MutableStateFlow<Flight.Team?>(null)
    val teamFlight: StateFlow<Flight.Team?> = _teamFlight.asStateFlow()

    private var pendingPilotChange = false
    private var changePointLap = 0
    private var changePointTimeMs = 0L

    fun setRules(newRules: Rules) {
        rules = newRules
        if (newRules is Rules.Team) {
            changePointLap = if (newRules.changeMode == Rules.Team.ChangeMode.LAPS) (newRules.lapsLimit / 2).coerceAtLeast(0) else 0
            changePointTimeMs = if (newRules.changeMode == Rules.Team.ChangeMode.TIME) (newRules.timeLimitSeconds * 1000L / 2).coerceAtLeast(0L) else 0L
        } else {
            changePointLap = 0
            changePointTimeMs = 0L
        }
    }

    fun prepareRace(startSignal: StartSignal, muted: StateFlow<Boolean>) {
        if (_flightPhase.value != FlightPhase.IDLE) return
        reset()
        _flightPhase.value = FlightPhase.PRE_FLIGHT
        preJob = viewModelScope.launch {
            if (startSignal == StartSignal.MANUAL) {
                blinkLoop()
            } else {
                val wait = when (startSignal) {
                    StartSignal.FIXED -> 1500L
                    else -> Random.nextLong(1000, 3000)
                }
                val totalDelay = wait + if (muted.value) 0 else BUZZER_DURATION_MS
                launch {
                    val start = SystemClock.elapsedRealtime()
                    while (true) {
                        val passed = SystemClock.elapsedRealtime() - start
                        val remaining = (totalDelay - passed).coerceAtLeast(0)
                        _elapsedMs.value = -remaining
                        if (remaining <= 0) break
                        delay(16)
                    }
                }
                delay(wait)
                if (!muted.value) {
                    SoundManager.playBuzzer()
                    delay(BUZZER_DURATION_MS)
                }
                _flightPhase.value = FlightPhase.FLIGHT
                startTimer(muted)
            }
        }
    }

    fun manualStart(muted: StateFlow<Boolean>) {
        if (_flightPhase.value != FlightPhase.PRE_FLIGHT) return
        preJob?.cancel()
        _isPreBlinking.value = false
        _flightPhase.value = FlightPhase.FLIGHT
        viewModelScope.launch {
            if (!muted.value) {
                SoundManager.playBuzzer()
            }
            delay(BUZZER_DURATION_MS)
            startTimer(muted)
        }
    }

    fun stopRace(muted: Boolean) {
        performStop(skipBuzzer = false, muted = muted, delayForBuzzer = true, reason = StopReason.MANUAL)
    }

    private fun performStop(
        skipBuzzer: Boolean,
        muted: Boolean,
        delayForBuzzer: Boolean = true,
        reason: StopReason
    ) {
        _stopReason.value = reason
        timerJob?.cancel()
        _currentLap.value = null

        val hasSuccessLap = _laps.value.any { it.success && it.number > 0 }
        _shouldSaveResult.value = hasSuccessLap
        _rotatePilotsForNextFlight.value = hasSuccessLap && rules is Rules.Team
        if (rules is Rules.Team) {
            _teamFlight.value = computeTeamFlight(
                _laps.value,
                reason,
                _pilotChangeIndex.value,
                (rules as Rules.Team).swapMode
            )
        }

        _flightPhase.value = FlightPhase.POST_FLIGHT

        viewModelScope.launch {
            if (!skipBuzzer && !muted) {
                SoundManager.playBuzzer()
                if (delayForBuzzer) delay(BUZZER_DURATION_MS)
            }
        }
    }

    fun addLap(muted: Boolean) {
        if (_flightPhase.value != FlightPhase.FLIGHT) return
        if (!muted) {
            SoundManager.playGate()
        }

        val current = _currentLap.value ?: return
        val completed = current.copy(interval = TimeInterval(current.startMs, _elapsedMs.value))
        _laps.value += completed

        if (current.success) {
            nextLapNumber++
        }
        lastLapElapsedMs = _elapsedMs.value
        _currentLapTime.value = 0L

        val teamRules = rules as? Rules.Team
        if (teamRules != null) {
            if (teamRules.changeMode == Rules.Team.ChangeMode.LAPS && !pendingPilotChange && nextLapNumber - 1 == changePointLap) {
                pendingPilotChange = true
                if (!muted) SoundManager.playBuzzer()
            }

            if (pendingPilotChange) {
                if (_pilotChangeIndex.value == NO_PILOT_CHANGE) {
                    _pilotChangeIndex.value = (_laps.value.size - 1).coerceAtLeast(0)
                }
                pendingPilotChange = false
                _currentLap.value = Lap(
                    number = nextLapNumber,
                    interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
                    success = true,
                )
                return
            }
        }

        if (current.success && current.number > 0 && nextLapNumber - 1 >= rules.lapsLimit) {
            performStop(
                skipBuzzer = false,
                muted = muted,
                delayForBuzzer = false,
                reason = StopReason.LAPS_LIMIT
            )
            return
        }

        _currentLap.value = Lap(
            number = nextLapNumber,
            interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
            success = true,
        )
    }

    fun addErrorToLastLap() {
        if (_flightPhase.value != FlightPhase.FLIGHT) return
        val current = _currentLap.value ?: return
        if (!current.success) return
        _currentLap.value = current.copy(success = false)
    }

    fun addFixToLastLap() {
        if (_flightPhase.value != FlightPhase.FLIGHT) return
        val current = _currentLap.value ?: return
        if (current.success) return
        _currentLap.value = current.copy(success = true)
    }

    fun setShouldSaveResult(value: Boolean) {
        _shouldSaveResult.value = value
    }

    fun setRotatePilotsForNextFlight(value: Boolean) {
        _rotatePilotsForNextFlight.value = value
    }

    fun reset() {
        preJob?.cancel()
        timerJob?.cancel()
        _isPreBlinking.value = false
        _elapsedMs.value = 0L
        _currentLapTime.value = 0L
        _laps.value = emptyList()
        _currentLap.value = null
        _stopReason.value = null
        nextLapNumber = 0
        lastLapElapsedMs = 0L
        _pilotChangeIndex.value = NO_PILOT_CHANGE
        pendingPilotChange = false
        _shouldSaveResult.value = false
        _rotatePilotsForNextFlight.value = false
        _teamFlight.value = null
        _flightPhase.value = FlightPhase.IDLE
    }

    fun buildFlight(): Flight {
        val stopReason = _stopReason.value ?: StopReason.MANUAL
        return when (rules) {
            is Rules.Individual -> computeIndividualFlight(_laps.value, stopReason)
            is Rules.Team -> {
                _teamFlight.value
                    ?: computeTeamFlight(
                        _laps.value,
                        stopReason,
                        _pilotChangeIndex.value,
                        (rules as Rules.Team).swapMode
                    )
            }
        }
    }

    private fun startTimer(muted: StateFlow<Boolean>) {
        if (_flightPhase.value != FlightPhase.FLIGHT) return
        nextLapNumber = if (rules.holeshotEnabled) 0 else 1
        _laps.value = emptyList()
        _currentLap.value = Lap(
            number = if (rules.holeshotEnabled) 0 else 1,
            interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
            success = true,
        )
        val startTime = SystemClock.elapsedRealtime()
        val limitMs = rules.timeLimitSeconds * 1000L
        val buzzerStartMs = limitMs - BUZZER_DURATION_MS
        timerJob = viewModelScope.launch {
            var buzzerPlayed = false
            var changeBuzzerPlayed = false
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                _elapsedMs.value = elapsed
                _currentLapTime.value = elapsed - lastLapElapsedMs

                val localRules = rules
                if (localRules is Rules.Team && localRules.changeMode == Rules.Team.ChangeMode.TIME && !changeBuzzerPlayed && elapsed >= changePointTimeMs) {
                    changeBuzzerPlayed = true
                    if (!muted.value) SoundManager.playBuzzer()
                    pendingPilotChange = true
                    _pilotChangeIndex.value = _laps.value.size
                }

                if (rules.timeLimitSeconds != Int.MAX_VALUE && !buzzerPlayed && elapsed >= buzzerStartMs) {
                    buzzerPlayed = true
                    if (!muted.value) {
                        SoundManager.playBuzzer()
                    }
                }
                if (rules.timeLimitSeconds != Int.MAX_VALUE && elapsed >= limitMs) {
                    performStop(skipBuzzer = true, muted = muted.value, reason = StopReason.TIME_LIMIT)
                    break
                }
                delay(16)
            }
        }
    }

    private suspend fun blinkLoop() {
        _isPreBlinking.value = true
        try {
            while (true) {
                delay(200)
                _isPreBlinking.value = false
                delay(200)
                _isPreBlinking.value = true
            }
        } finally {
            _isPreBlinking.value = false
        }
    }
}
