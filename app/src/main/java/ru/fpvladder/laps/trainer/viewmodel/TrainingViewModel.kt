package ru.fpvladder.laps.trainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.fpvladder.laps.trainer.data.TrainingStorage
import ru.fpvladder.laps.trainer.model.Training

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = TrainingStorage(application.applicationContext)

    private val _trainings = MutableStateFlow<List<Training>>(emptyList())
    val trainings: StateFlow<List<Training>> = _trainings.asStateFlow()

    init {
        _trainings.value = storage.load()
    }

    private val _selectedTraining = MutableStateFlow<Training?>(null)
    val selectedTraining: StateFlow<Training?> = _selectedTraining.asStateFlow()

    fun selectTraining(training: Training) {
        _selectedTraining.value = training
    }

    fun clearSelectedTraining() {
        _selectedTraining.value = null
    }

    fun addTraining(training: Training) {
        val updated = _trainings.value + training
        _trainings.value = updated
        storage.save(updated)
    }

    fun deleteTraining(training: Training) {
        val updated = _trainings.value.filter { it.id != training.id }
        _trainings.value = updated
        storage.save(updated)
    }
}
