package ru.fpvladder.laps.trainer.storage

import android.content.Context
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.serialization.YamlFormat
import java.io.File

class TrainingStorage(
    private val dir: File,
    private val yaml: Yaml = YamlFormat,
) {

    constructor(context: Context, yaml: Yaml = YamlFormat) : this(
        File(context.filesDir, "training/active"),
        yaml
    )

    fun save(training: Training) {
        if (training.isDefault()) return
        dir.mkdirs()
        File(dir, "${training.id}.yaml").writeText(yaml.encodeToString(training))
    }

    fun loadAll(): List<Training> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.extension == "yaml" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching {
                    yaml.decodeFromString(Training.serializer(), file.readText())
                }.getOrNull()
            }
            ?: emptyList()
    }

    fun delete(training: Training) {
        File(dir, "${training.id}.yaml").delete()
    }

    fun touch(training: Training) {
        if (training.isDefault()) return
        File(dir, "${training.id}.yaml").takeIf { it.exists() }?.setLastModified(System.currentTimeMillis())
    }
}
