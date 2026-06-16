package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.storage.TrainingStorage

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = TrainingStorage(application.applicationContext)

    private val _trainings = MutableStateFlow<List<Training>>(emptyList())
    val trainings: StateFlow<List<Training>> = _trainings.asStateFlow()

    private val _selectedTraining: MutableStateFlow<Training>
    val selectedTraining: StateFlow<Training>

    init {
        val loaded = storage.loadAll()
        val initial = loaded.ifEmpty { listOf(Training.DEFAULT) }
        _trainings.value = initial
        _selectedTraining = MutableStateFlow(initial.first())
        selectedTraining = _selectedTraining.asStateFlow()
    }

    fun selectTraining(training: Training) {
        _selectedTraining.value = training
        viewModelScope.launch {
            storage.touch(training)
        }
        cleanupDefaults(training)
        _trainings.value = listOf(training) + _trainings.value.filter { it.id != training.id }
    }

    fun addTraining(training: Training) {
        _trainings.value = listOf(training) + _trainings.value.filter { it.id != training.id }
        cleanupDefaults(training)
        _selectedTraining.value = training
        viewModelScope.launch {
            storage.save(training)
        }
    }

    fun deleteTraining(training: Training) {
        val remaining = _trainings.value.filter { it.id != training.id }
        val newSelected = if (_selectedTraining.value.id == training.id) {
            remaining.lastOrNull() ?: Training.DEFAULT
        } else {
            _selectedTraining.value
        }
        _selectedTraining.value = newSelected
        cleanupDefaults(newSelected)
        _trainings.value = listOf(newSelected) + _trainings.value.filter {
            it.id != newSelected.id && it.id != training.id
        }
        viewModelScope.launch {
            storage.delete(training)
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
        saveAll(updated)
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
        cleanupDefaults(updatedTraining)
        save(updatedTraining)
    }

    private fun cleanupDefaults(keep: Training) {
        _trainings.value = _trainings.value.filter { !it.isDefault() || it.id == keep.id }
    }

    private fun save(training: Training) {
        viewModelScope.launch {
            storage.save(training)
        }
    }

    private fun saveAll(trainings: List<Training>) {
        viewModelScope.launch {
            trainings.forEach { storage.save(it) }
        }
    }
}
