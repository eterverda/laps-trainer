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
import ru.fpvladder.laps.trainer.model.SwapMode
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.Training

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val _trainings = MutableStateFlow<List<Training>>(emptyList())
    val trainings: StateFlow<List<Training>> = _trainings.asStateFlow()

    private val _archivedTrainings = MutableStateFlow<List<Training>>(emptyList())
    val archivedTrainings: StateFlow<List<Training>> = _archivedTrainings.asStateFlow()

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

    fun archiveTraining(training: Training) {
        val updated = _trainings.value.filter { it.id != training.id }
        _archivedTrainings.value = _archivedTrainings.value + training.withArchived(true)
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

    fun updateTrainingStats(training: Training, stats: Stats) {
        val updatedTraining = when (training) {
            is Training.Individual -> training.copy(stats = stats as Stats.Individual)
            is Training.Team -> training.copy(stats = stats as Stats.Team)
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
        val updated = _trainings.value.map { training ->
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
        val selectedId = _selectedTraining.value.id
        _selectedTraining.value = updated.find { it.id == selectedId } ?: updated.last()
    }

    fun updatePilot(pilot: Pilot) {
        val training = _selectedTraining.value
        val rules = training.rules
        val updatedTraining = when (training) {
            is Training.Individual -> {
                when (pilot) {
                    is Pilot.Individual -> training.copy(pilot = pilot)
                    is Pilot.Team -> Training.Team(
                        id = training.id,
                        pilot = pilot,
                        rules = Rules.Team(
                            maxLaps = rules.maxLaps,
                            timeLimitSeconds = rules.timeLimitSeconds,
                            holeshotEnabled = rules.holeshotEnabled,
                            swapMode = SwapMode.TIME
                        ),
                        stats = Stats.Team(),
                        createdAt = training.createdAt,
                        isArchived = training.isArchived
                    )
                }
            }
            is Training.Team -> {
                when (pilot) {
                    is Pilot.Individual -> Training.Individual(
                        id = training.id,
                        pilot = pilot,
                        rules = Rules.Individual(
                            maxLaps = rules.maxLaps,
                            timeLimitSeconds = rules.timeLimitSeconds,
                            holeshotEnabled = rules.holeshotEnabled
                        ),
                        stats = Stats.Individual(),
                        createdAt = training.createdAt,
                        isArchived = training.isArchived
                    )
                    is Pilot.Team -> training.copy(pilot = pilot)
                }
            }
        }
        updateTrainingInList(updatedTraining)
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

    fun swapPilotOrder() {
        val training = _selectedTraining.value
        if (training is Training.Team) {
            val updated = training.copy(
                rules = training.rules.copy(pilotOrderSwapped = !training.rules.pilotOrderSwapped)
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
