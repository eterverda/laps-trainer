package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.data.SettingsDataStore
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = SettingsDataStore(application.applicationContext)

    val channelGrid: StateFlow<ChannelGrid> = dataStore.channelGrid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelGrid.HDZERO)

    val colorCount: StateFlow<ColorCount> = dataStore.colorCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ColorCount.FOUR)

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
}
