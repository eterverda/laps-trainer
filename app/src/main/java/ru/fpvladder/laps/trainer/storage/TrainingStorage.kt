package ru.fpvladder.laps.trainer.storage

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.serialization.YamlFormat
import java.io.File

class TrainingStorage(
    private val file: File,
    private val yaml: Yaml = YamlFormat,
) {

    fun save(training: Training) {
        file.parentFile?.mkdirs()
        file.writeText(yaml.encodeToString(training))
    }

    fun load(): Training {
        return yaml.decodeFromString(Training.serializer(), file.readText())
    }

    fun exists(): Boolean = file.exists()
}
