package ru.fpvladder.laps.lite.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.fpvladder.laps.lite.model.ChannelColor
import ru.fpvladder.laps.lite.model.Pilot

enum class AppScreen { Main, Race }

class PilotViewModel : ViewModel() {

    private val _pilot = MutableStateFlow(Pilot())
    val pilot: StateFlow<Pilot> = _pilot.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.Main)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun updateName(name: String) {
        _pilot.value = _pilot.value.copy(name = name.take(24))
    }

    fun updateChannel(letter: String, number: Int, color: ChannelColor) {
        _pilot.value = _pilot.value.copy(
            channelLetter = letter,
            channelNumber = number,
            channelColor = color
        )
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }
}
