package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.settings.SettingsDataStore
import ru.fpvladder.laps.trainer.settings.AppThemeMode
import ru.fpvladder.laps.trainer.settings.DarkThemeVariant
import ru.fpvladder.laps.trainer.settings.ChannelGrid
import ru.fpvladder.laps.trainer.settings.ColorCount
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.settings.USB_ENABLED

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application.applicationContext)

    init {
        viewModelScope.launch {
            dataStore.migrateThemeSettings()
        }
    }

    val channelGrid: StateFlow<ChannelGrid> = dataStore.channelGrid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelGrid.HDZERO)

    val colorCount: StateFlow<ColorCount> = dataStore.colorCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColorCount.FOUR)

    val isMuted: StateFlow<Boolean> = dataStore.isMuted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isUsbKeyboardEnabled: StateFlow<Boolean> = dataStore.isUsbKeyboardEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val effectiveUsbKeyboardEnabled: StateFlow<Boolean> = isUsbKeyboardEnabled
        .map { it && USB_ENABLED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), USB_ENABLED)

    val appTheme: StateFlow<AppThemeMode> = dataStore.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val darkThemeVariant: StateFlow<DarkThemeVariant> = dataStore.darkThemeVariant
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkThemeVariant.CATPUCCIN_MOCHA)

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

    fun setAppTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            dataStore.setAppTheme(mode)
        }
    }

    fun setDarkThemeVariant(variant: DarkThemeVariant) {
        viewModelScope.launch {
            dataStore.setDarkThemeVariant(variant)
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
