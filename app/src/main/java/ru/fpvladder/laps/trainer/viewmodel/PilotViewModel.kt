package ru.fpvladder.laps.trainer.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen { Race, Training, Settings }

class PilotViewModel : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.Training)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }
}
