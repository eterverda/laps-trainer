package ru.fpvladder.laps.trainer.viewmodel

import android.os.SystemClock
import android.util.Log
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
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight
import kotlin.random.Random

class FlightViewModel : ViewModel() {

    private val _isPostFlight = MutableStateFlow(false)
    val isPostFlight: StateFlow<Boolean> = _isPostFlight.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _isPreBlinking = MutableStateFlow(false)
    val isPreBlinking: StateFlow<Boolean> = _isPreBlinking.asStateFlow()

    private val _preStartTime = MutableStateFlow(0L)
    val preStartTime: StateFlow<Long> = _preStartTime.asStateFlow()

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

    private val _swapPilotsForNextFlight = MutableStateFlow(false)
    val swapPilotsForNextFlight: StateFlow<Boolean> = _swapPilotsForNextFlight.asStateFlow()

    private val _preStartCountdownMs = MutableStateFlow(0L)
    val preStartCountdownMs: StateFlow<Long> = _preStartCountdownMs.asStateFlow()

    private var timerJob: Job? = null
    private var preJob: Job? = null
    private var nextLapNumber = 0
    private var rules: Rules = Rules.Individual()
    private var raceIsMuted: Boolean = false
    private var lastLapElapsedMs = 0L

    private val _pilotSwapIndex = MutableStateFlow<Int?>(null)
    val pilotSwapIndex: StateFlow<Int?> = _pilotSwapIndex.asStateFlow()

    private val _teamFlight = MutableStateFlow<Flight.Team?>(null)
    val teamFlight: StateFlow<Flight.Team?> = _teamFlight.asStateFlow()

    private var pendingPilotSwap = false
    private var swapPointLap = 0
    private var swapPointTimeMs = 0L

    fun setRules(newRules: Rules) {
        rules = newRules
        if (newRules is Rules.Team) {
            swapPointLap = if (newRules.swapMode == Rules.Team.SwapMode.LAPS) (newRules.maxLaps / 2).coerceAtLeast(0) else 0
            swapPointTimeMs = if (newRules.swapMode == Rules.Team.SwapMode.TIME) (newRules.timeLimitSeconds * 1000L / 2).coerceAtLeast(0L) else 0L
        } else {
            swapPointLap = 0
            swapPointTimeMs = 0L
        }
    }

    fun prepareRace(startSignal: StartSignal, isMuted: Boolean) {
        reset()
        _preStartTime.value = SystemClock.elapsedRealtime()
        preJob = viewModelScope.launch {
            when (startSignal) {
                StartSignal.FIXED -> {
                    val totalDelay = 1500L + if (isMuted) 0 else BUZZER_DURATION_MS
                    launch {
                        val start = SystemClock.elapsedRealtime()
                        while (true) {
                            val passed = SystemClock.elapsedRealtime() - start
                            val remaining = (totalDelay - passed).coerceAtLeast(0)
                            _preStartCountdownMs.value = remaining
                            if (remaining <= 0) break
                            delay(16)
                        }
                    }
                    delay(1500)
                    if (!isMuted) SoundManager.playBuzzer()
                    if (!isMuted) delay(BUZZER_DURATION_MS)
                    startTimer(isMuted)
                }

                StartSignal.RANDOM -> {
                    val blinkJob = launch { blinkLoop() }
                    val wait = Random.nextLong(1000, 3000)
                    delay(wait)
                    blinkJob.cancel()
                    if (!isMuted) SoundManager.playBuzzer()
                    if (!isMuted) delay(BUZZER_DURATION_MS)
                    startTimer(isMuted)
                }

                StartSignal.MANUAL -> {
                    blinkLoop()
                }
            }
        }
    }

    fun manualStart(isMuted: Boolean) {
        preJob?.cancel()
        _isPreBlinking.value = false
        viewModelScope.launch {
            if (!isMuted) SoundManager.playBuzzer()
            delay(BUZZER_DURATION_MS)
            startTimer(isMuted)
        }
    }

    fun stopRace(isMuted: Boolean) {
        performStop(skipBuzzer = false, isMuted = isMuted, reason = StopReason.MANUAL)
    }

    private fun performStop(
        skipBuzzer: Boolean,
        isMuted: Boolean,
        delayForBuzzer: Boolean = true,
        reason: StopReason
    ) {
        _stopReason.value = reason
        _isStopping.value = true
        viewModelScope.launch {
            if (!skipBuzzer && !isMuted) {
                SoundManager.playBuzzer()
                if (delayForBuzzer) delay(BUZZER_DURATION_MS)
            }
            timerJob?.cancel()

            _currentLap.value = null

            _isPostFlight.value = true
            _isStopping.value = false
            val hasSuccessLap = _laps.value.any { it.status == Lap.Status.SUCCESS }
            _shouldSaveResult.value = hasSuccessLap
            _swapPilotsForNextFlight.value = hasSuccessLap && rules is Rules.Team
            if (rules is Rules.Team) {
                _teamFlight.value = computeTeamFlight(
                    _laps.value,
                    reason,
                    _pilotSwapIndex.value
                )
            }
        }
    }

    fun addLap() {
        val current = _currentLap.value ?: return
        Log.d("FlightVM", "addLap: currentLabel=${current.label}, nextLapNumber=$nextLapNumber, swapPointLap=$swapPointLap, pendingPilotSwap=$pendingPilotSwap, lapsSize=${_laps.value.size}")
        val completed = current.copy(interval = TimeInterval(current.startMs, _elapsedMs.value))
        _laps.value = _laps.value + completed

        if (current.status != Lap.Status.FAIL) {
            nextLapNumber++
        }
        lastLapElapsedMs = _elapsedMs.value
        _currentLapTime.value = 0L

        val teamRules = rules as? Rules.Team
        if (teamRules != null) {
            if (teamRules.swapMode == Rules.Team.SwapMode.LAPS && !pendingPilotSwap && nextLapNumber - 1 == swapPointLap) {
                pendingPilotSwap = true
                Log.d("FlightVM", "Triggering pilot swap at lap ${nextLapNumber - 1}")
                if (!raceIsMuted) SoundManager.playBuzzer()
            }

            if (pendingPilotSwap) {
                if (_pilotSwapIndex.value == null) {
                    _pilotSwapIndex.value = (_laps.value.size - 1).coerceAtLeast(0)
                }
                pendingPilotSwap = false
                val newLabel = if (nextLapNumber == 0) "HS" else "$nextLapNumber)"
                _currentLap.value = Lap(
                    label = newLabel,
                    interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
                    status = if (nextLapNumber == 0) Lap.Status.HS else Lap.Status.SUCCESS
                )
                return
            }
        }

        if (current.status == Lap.Status.SUCCESS && nextLapNumber - 1 >= rules.maxLaps) {
            performStop(
                skipBuzzer = false,
                isMuted = raceIsMuted,
                delayForBuzzer = false,
                reason = StopReason.MAX_LAPS
            )
            return
        }

        val newLabel = if (nextLapNumber == 0) "HS" else "$nextLapNumber)"
        _currentLap.value = Lap(
            label = newLabel,
            interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
            status = if (nextLapNumber == 0) Lap.Status.HS else Lap.Status.SUCCESS
        )
        Log.d("FlightVM", "addLap finished: newLabel=$newLabel, pilotSwapIndex=${_pilotSwapIndex.value}")
    }

    fun addErrorToLastLap() {
        val current = _currentLap.value ?: return
        if (current.status == Lap.Status.FAIL) return
        _currentLap.value = current.copy(status = Lap.Status.FAIL)
    }

    fun addFixToLastLap() {
        val current = _currentLap.value ?: return
        if (current.status != Lap.Status.FAIL) return
        val restoredStatus = if (current.label == "HS") Lap.Status.HS else Lap.Status.SUCCESS
        _currentLap.value = current.copy(status = restoredStatus)
    }

    fun setShouldSaveResult(value: Boolean) {
        _shouldSaveResult.value = value
    }

    fun setSwapPilotsForNextFlight(value: Boolean) {
        _swapPilotsForNextFlight.value = value
    }

    fun reset() {
        preJob?.cancel()
        timerJob?.cancel()
        _isPreBlinking.value = false
        _isStopping.value = false
        _elapsedMs.value = 0L
        _currentLapTime.value = 0L
        _preStartTime.value = 0L
        _isPostFlight.value = false
        _isStarted.value = false
        _laps.value = emptyList()
        _currentLap.value = null
        _stopReason.value = null
        _preStartCountdownMs.value = 0L
        nextLapNumber = 0
        lastLapElapsedMs = 0L
        _pilotSwapIndex.value = null
        pendingPilotSwap = false
        _shouldSaveResult.value = false
        _swapPilotsForNextFlight.value = false
        _teamFlight.value = null
    }

    fun buildFlight(): Flight {
        val stopReason = _stopReason.value ?: StopReason.MANUAL
        return when (rules) {
            is Rules.Individual -> computeIndividualFlight(_laps.value, stopReason)
            is Rules.Team -> {
                _teamFlight.value
                    ?: computeTeamFlight(_laps.value, stopReason, _pilotSwapIndex.value)
            }
        }
    }

    private fun startTimer(isMuted: Boolean) {
        if (_isStarted.value) return
        raceIsMuted = isMuted
        _isStarted.value = true
        nextLapNumber = if (rules.holeshotEnabled) 0 else 1
        _laps.value = emptyList()
        _currentLap.value = Lap(
            label = if (rules.holeshotEnabled) "HS" else "1)",
            interval = TimeInterval(lastLapElapsedMs, lastLapElapsedMs),
            status = if (rules.holeshotEnabled) Lap.Status.HS else Lap.Status.SUCCESS
        )
        val startTime = SystemClock.elapsedRealtime()
        val limitMs = rules.timeLimitSeconds * 1000L
        val buzzerStartMs = limitMs - BUZZER_DURATION_MS
        timerJob = viewModelScope.launch {
            var buzzerPlayed = false
            var swapBuzzerPlayed = false
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                _elapsedMs.value = elapsed
                _currentLapTime.value = elapsed - lastLapElapsedMs

                val localRules = rules
                if (localRules is Rules.Team && localRules.swapMode == Rules.Team.SwapMode.TIME && !swapBuzzerPlayed && elapsed >= swapPointTimeMs) {
                    swapBuzzerPlayed = true
                    if (!isMuted) SoundManager.playBuzzer()
                    pendingPilotSwap = true
                    _pilotSwapIndex.value = _laps.value.size
                }

                if (rules.timeLimitSeconds != Int.MAX_VALUE && !buzzerPlayed && elapsed >= buzzerStartMs) {
                    buzzerPlayed = true
                    if (!isMuted) SoundManager.playBuzzer()
                }
                if (rules.timeLimitSeconds != Int.MAX_VALUE && elapsed >= limitMs) {
                    performStop(skipBuzzer = true, isMuted = isMuted, reason = StopReason.TIME_LIMIT)
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
