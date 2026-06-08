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
import ru.fpvladder.laps.trainer.model.LapEntry
import ru.fpvladder.laps.trainer.model.LapIcon
import ru.fpvladder.laps.trainer.model.LapStatus
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimerPrecision
import kotlin.random.Random

class FlightViewModel : ViewModel() {

    private val _phase = MutableStateFlow(FlightPhase.PRE)
    val phase: StateFlow<FlightPhase> = _phase.asStateFlow()

    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _isPreBlinking = MutableStateFlow(false)
    val isPreBlinking: StateFlow<Boolean> = _isPreBlinking.asStateFlow()

    private val _preStartTime = MutableStateFlow(0L)
    val preStartTime: StateFlow<Long> = _preStartTime.asStateFlow()

    private val _laps = MutableStateFlow<List<LapEntry>>(emptyList())
    val laps: StateFlow<List<LapEntry>> = _laps.asStateFlow()

    private val _currentLapTime = MutableStateFlow(0L)
    val currentLapTime: StateFlow<Long> = _currentLapTime.asStateFlow()

    private val _timerPrecision = MutableStateFlow(TimerPrecision.MILLISECONDS)
    val timerPrecision: StateFlow<TimerPrecision> = _timerPrecision.asStateFlow()

    private val _stopReason = MutableStateFlow<StopReason?>(null)
    val stopReason: StateFlow<StopReason?> = _stopReason.asStateFlow()

    private val _preStartCountdownMs = MutableStateFlow(0L)
    val preStartCountdownMs: StateFlow<Long> = _preStartCountdownMs.asStateFlow()

    private var timerJob: Job? = null
    private var preJob: Job? = null
    private var nextLapNumber = 0
    private var rules: Rules = Rules.Individual()
    private var raceIsMuted: Boolean = false
    private var lastLapElapsedMs = 0L

    fun setRules(newRules: Rules) {
        rules = newRules
    }

    fun setTimerPrecision(precision: TimerPrecision) {
        _timerPrecision.value = precision
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

            val current = _laps.value.toMutableList()
            if (current.isNotEmpty()) {
                val lastIndex = current.size - 1
                val last = current[lastIndex]
                if (!last.icons.contains(LapIcon.LAP)) {
                    current.removeAt(lastIndex)
                }
            }
            _laps.value = current

            _phase.value = FlightPhase.POST
            _isStopping.value = false
        }
    }

    fun addLap() {
        val lapTime = _elapsedMs.value - lastLapElapsedMs
        val current = _laps.value.toMutableList()
        var status = LapStatus.RUNNING
        if (current.isNotEmpty()) {
            val lastIndex = current.size - 1
            val last = current[lastIndex]
            val newIcons = last.icons.toMutableList()
            newIcons.add(LapIcon.LAP)
            status = if (newIcons.size >= 2 && newIcons[newIcons.size - 2] == LapIcon.ERROR) {
                LapStatus.FAIL
            } else {
                LapStatus.SUCCESS
            }
            val label = if (status == LapStatus.FAIL) "" else last.lapLabel
            if (status == LapStatus.SUCCESS) {
                nextLapNumber++
            }
            current[lastIndex] = last.copy(timeMs = lapTime, icons = newIcons, status = status, lapLabel = label)
        }
        lastLapElapsedMs = _elapsedMs.value
        _currentLapTime.value = 0L

        if (status == LapStatus.SUCCESS && nextLapNumber - 1 >= rules.maxLaps) {
            _laps.value = current
            performStop(
                skipBuzzer = false,
                isMuted = raceIsMuted,
                delayForBuzzer = false,
                reason = StopReason.MAX_LAPS
            )
            return
        }

        val newLabel = if (nextLapNumber == 0) "HS" else "$nextLapNumber)"
        val entry = LapEntry(
            lapLabel = newLabel,
            timeMs = 0L,
            icons = emptyList(),
            status = LapStatus.RUNNING
        )
        _laps.value = current + entry
    }

    fun addErrorToLastLap() {
        val current = _laps.value.toMutableList()
        if (current.isEmpty()) return
        val last = current.last()
        if (last.icons.contains(LapIcon.ERROR)) return
        val newIcons = last.icons.toMutableList()
        newIcons.add(LapIcon.ERROR)
        current[current.size - 1] = last.copy(icons = newIcons)
        _laps.value = current
    }

    fun addFixToLastLap() {
        val current = _laps.value.toMutableList()
        if (current.isEmpty()) return
        val last = current.last()
        if (last.icons.contains(LapIcon.FIX)) return
        val newIcons = last.icons.toMutableList()
        newIcons.add(LapIcon.FIX)
        current[current.size - 1] = last.copy(icons = newIcons)
        _laps.value = current
    }

    fun reset() {
        preJob?.cancel()
        timerJob?.cancel()
        _isPreBlinking.value = false
        _isStopping.value = false
        _elapsedMs.value = 0L
        _currentLapTime.value = 0L
        _preStartTime.value = 0L
        _phase.value = FlightPhase.PRE
        _laps.value = emptyList()
        _stopReason.value = null
        _preStartCountdownMs.value = 0L
        nextLapNumber = 0
        lastLapElapsedMs = 0L
    }

    private fun startTimer(isMuted: Boolean) {
        if (_phase.value == FlightPhase.MAIN) return
        raceIsMuted = isMuted
        _phase.value = FlightPhase.MAIN
        nextLapNumber = if (rules.holeshotEnabled) 0 else 1
        _laps.value = listOf(
            LapEntry(
                lapLabel = if (rules.holeshotEnabled) "HS" else "1)",
                timeMs = 0L,
                icons = emptyList(),
                status = LapStatus.RUNNING
            )
        )
        val startTime = SystemClock.elapsedRealtime()
        val limitMs = rules.timeLimitSeconds * 1000L
        val buzzerStartMs = limitMs - BUZZER_DURATION_MS
        timerJob = viewModelScope.launch {
            var buzzerPlayed = false
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                _elapsedMs.value = elapsed
                _currentLapTime.value = elapsed - lastLapElapsedMs

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
