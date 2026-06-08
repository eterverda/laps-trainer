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

    private var timerJob: Job? = null
    private var preJob: Job? = null

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

    fun reset() {
        preJob?.cancel()
        timerJob?.cancel()
        _isPreBlinking.value = false
        _isStopping.value = false
        _elapsedMs.value = 0L
        _preStartTime.value = 0L
        _phase.value = FlightPhase.PRE
    }

    private fun startTimer() {
        if (_phase.value == FlightPhase.MAIN) return
        _phase.value = FlightPhase.MAIN
        val startTime = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (true) {
                _elapsedMs.value = SystemClock.elapsedRealtime() - startTime
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
