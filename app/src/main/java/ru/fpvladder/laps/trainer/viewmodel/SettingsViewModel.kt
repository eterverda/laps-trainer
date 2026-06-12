package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.data.SettingsDataStore
import ru.fpvladder.laps.trainer.model.AppTheme
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.TimerPrecision

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application.applicationContext)

    val channelGrid: StateFlow<ChannelGrid> = dataStore.channelGrid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelGrid.HDZERO)

    val colorCount: StateFlow<ColorCount> = dataStore.colorCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColorCount.FOUR)

    val isMuted: StateFlow<Boolean> = dataStore.isMuted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isUsbKeyboardEnabled: StateFlow<Boolean> = dataStore.isUsbKeyboardEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appTheme: StateFlow<AppTheme> = dataStore.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val timerPrecision: StateFlow<TimerPrecision> = dataStore.timerPrecision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimerPrecision.DECISECONDS)

    val startSignal: StateFlow<StartSignal> = dataStore.startSignal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StartSignal.RANDOM)

    val effectiveStartSignal: StateFlow<StartSignal> = combine(isMuted, startSignal) { muted, signal ->
        if (muted) StartSignal.MANUAL else signal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StartSignal.RANDOM)

    val useLapButton: StateFlow<Boolean> = dataStore.useLapButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useErrorFixButtons: StateFlow<Boolean> = dataStore.useErrorFixButtons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setChannelGrid(grid: ChannelGrid) {
        viewModelScope.launch {
            dataStore.setChannelGrid(grid)
        }
    }

    fun setColorCount(count: ColorCount) {
        viewModelScope.launch {
            dataStore.setColorCount(count)
        }
    }

    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            dataStore.setMuted(muted)
        }
    }

    fun setUsbKeyboardEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setUsbKeyboardEnabled(enabled)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.setAppTheme(theme)
        }
    }

    fun setTimerPrecision(precision: TimerPrecision) {
        viewModelScope.launch {
            dataStore.setTimerPrecision(precision)
        }
    }

    fun setStartSignal(signal: StartSignal) {
        viewModelScope.launch {
            dataStore.setStartSignal(signal)
        }
    }

    fun setUseLapButton(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setUseLapButton(enabled)
        }
    }

    fun setUseErrorFixButtons(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setUseErrorFixButtons(enabled)
        }
    }
}
