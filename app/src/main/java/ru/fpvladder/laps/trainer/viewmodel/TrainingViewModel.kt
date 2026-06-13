package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Training

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val _trainings = MutableStateFlow<List<Training>>(emptyList())
    val trainings: StateFlow<List<Training>> = _trainings.asStateFlow()

    private val _hasPagerWiggled = MutableStateFlow(false)
    val hasPagerWiggled: StateFlow<Boolean> = _hasPagerWiggled.asStateFlow()

    private val _selectedTraining: MutableStateFlow<Training>
    val selectedTraining: StateFlow<Training>

    init {
        val individual = Training.Individual()
        val team = Training.Team(pilot = Pilot.Team(name1 = "Командор", name2 = "Дринкинс"))
        _trainings.value = listOf(individual, team)
        _selectedTraining = MutableStateFlow(individual)
        selectedTraining = _selectedTraining.asStateFlow()
    }

    fun selectTraining(training: Training) {
        _selectedTraining.value = training
    }

    fun markPagerWiggled() {
        _hasPagerWiggled.value = true
    }

    fun addTraining(training: Training) {
        val updated = _trainings.value + training
        _trainings.value = updated
    }

    fun deleteTraining(training: Training) {
        val updated = _trainings.value.filter { it.id != training.id }
        _trainings.value = updated
        if (_selectedTraining.value.id == training.id) {
            _selectedTraining.value = updated.lastOrNull() ?: _selectedTraining.value
        }
    }

    fun updateTrainingRules(training: Training, newRules: Rules) {
        val updatedTraining = when (training) {
            is Training.Individual -> training.copy(rules = newRules as Rules.Individual)
            is Training.Team -> training.copy(rules = newRules as Rules.Team)
        }
        updateTrainingInList(updatedTraining)
    }

    fun addFlight(training: Training, flight: Flight) {
        val current = _trainings.value.find { it.id == training.id } ?: training
        val updatedTraining = when (current) {
            is Training.Individual -> current.copy(
                flights = current.flights + (flight as Flight.Individual)
            )
            is Training.Team -> current.copy(
                flights = current.flights + (flight as Flight.Team)
            )
        }
        updateTrainingInList(updatedTraining)
    }

    fun applyChannelToAll(channel: Channel) {
        val previous = _trainings.value
        if (previous.isEmpty()) return
        val updated = previous.map { training ->
            when (training) {
                is Training.Individual -> training.copy(
                    pilot = training.pilot.copy(channel = channel)
                )
                is Training.Team -> training.copy(
                    pilot = training.pilot.copy(channel = channel)
                )
            }
        }
        _trainings.value = updated
        val selectedIndex = previous.indexOfFirst { it.id == _selectedTraining.value.id }
        _selectedTraining.value = if (selectedIndex >= 0) updated[selectedIndex] else updated.last()
    }

    fun updateCurrentPilotChannel(channel: Channel) {
        val training = _selectedTraining.value
        val updatedTraining = when (training) {
            is Training.Individual -> training.copy(
                pilot = training.pilot.copy(channel = channel)
            )
            is Training.Team -> training.copy(
                pilot = training.pilot.copy(channel = channel)
            )
        }
        updateTrainingInList(updatedTraining)
    }

    fun rotatePilotOrder() {
        val training = _selectedTraining.value
        if (training is Training.Team) {
            val updated = training.copy(
                rules = training.rules.copy(swapMode = training.rules.swapMode.rotate())
            )
            updateTrainingInList(updated)
        }
    }

    fun updateCurrentPilotNames(name1: String, name2: String) {
        val training = _selectedTraining.value
        val updatedTraining = when (training) {
            is Training.Individual -> training.copy(
                pilot = training.pilot.copy(name = name1)
            )
            is Training.Team -> training.copy(
                pilot = training.pilot.copy(name1 = name1, name2 = name2)
            )
        }
        updateTrainingInList(updatedTraining)
    }

    private fun updateTrainingInList(updatedTraining: Training) {
        val updatedList = _trainings.value.map {
            if (it.id == updatedTraining.id) updatedTraining else it
        }
        _trainings.value = updatedList
        if (_selectedTraining.value.id == updatedTraining.id) {
            _selectedTraining.value = updatedTraining
        }
    }
}
