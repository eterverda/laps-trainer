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
import ru.fpvladder.laps.trainer.model.StartSignal
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

    private var timerJob: Job? = null
    private var preJob: Job? = null
    private var nextLapNumber = 0
    private var holeshotEnabled = false
    private var lastLapElapsedMs = 0L

    fun setHoleshotEnabled(enabled: Boolean) {
        holeshotEnabled = enabled
    }

    fun prepareRace(startSignal: StartSignal, isMuted: Boolean) {
        reset()
        _preStartTime.value = SystemClock.elapsedRealtime()
        preJob = viewModelScope.launch {
            when (startSignal) {
                StartSignal.FIXED -> {
                    delay(1500)
                    if (!isMuted) SoundManager.playBuzzer()
                    if (!isMuted) delay(BUZZER_DURATION_MS)
                    startTimer()
                }

                StartSignal.RANDOM -> {
                    val blinkJob = launch { blinkLoop() }
                    val wait = Random.nextLong(1000, 3000)
                    delay(wait)
                    blinkJob.cancel()
                    if (!isMuted) SoundManager.playBuzzer()
                    if (!isMuted) delay(BUZZER_DURATION_MS)
                    startTimer()
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
            startTimer()
        }
    }

    fun stopRace(isMuted: Boolean) {
        _isStopping.value = true
        viewModelScope.launch {
            if (!isMuted) SoundManager.playBuzzer()
            if (!isMuted) delay(BUZZER_DURATION_MS)
            timerJob?.cancel()
            _phase.value = FlightPhase.POST
            _isStopping.value = false
        }
    }

    fun addLap() {
        val lapTime = _elapsedMs.value - lastLapElapsedMs
        val current = _laps.value.toMutableList()
        if (current.isNotEmpty()) {
            val lastIndex = current.size - 1
            val last = current[lastIndex]
            val newIcons = last.icons.toMutableList()
            newIcons.add(LapIcon.LAP)
            val status = if (newIcons.size >= 2 && newIcons[newIcons.size - 2] == LapIcon.ERROR) {
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
        nextLapNumber = 0
        lastLapElapsedMs = 0L
    }

    private fun startTimer() {
        if (_phase.value == FlightPhase.MAIN) return
        _phase.value = FlightPhase.MAIN
        nextLapNumber = if (holeshotEnabled) 0 else 1
        _laps.value = listOf(
            LapEntry(
                lapLabel = if (holeshotEnabled) "HS" else "1)",
                timeMs = 0L,
                icons = emptyList(),
                status = LapStatus.RUNNING
            )
        )
        val startTime = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (true) {
                _elapsedMs.value = SystemClock.elapsedRealtime() - startTime
                _currentLapTime.value = _elapsedMs.value - lastLapElapsedMs
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
