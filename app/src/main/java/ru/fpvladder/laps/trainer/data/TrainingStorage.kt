package ru.fpvladder.laps.trainer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.TrainingType
import java.io.File

class TrainingStorage(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, FILENAME)

    fun load(): List<Training> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val array = JSONArray(json)
            List(array.length()) { index ->
                fromJson(array.getJSONObject(index))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(trainings: List<Training>) {
        try {
            val array = JSONArray()
            trainings.forEach { array.put(toJson(it)) }
            file.writeText(array.toString())
        } catch (_: Exception) {
        }
    }

    private fun toJson(training: Training): JSONObject {
        return JSONObject().apply {
            put("id", training.id)
            put("type", training.type.name)
            put("pilotName", training.pilotName ?: JSONObject.NULL)
            put("channelLetter", training.channelLetter ?: JSONObject.NULL)
            put("channelNumber", training.channelNumber ?: JSONObject.NULL)
            put("channelColor", training.channelColor?.name ?: JSONObject.NULL)
            put("createdAt", training.createdAt)
        }
    }

    private fun fromJson(obj: JSONObject): Training {
        return Training(
            id = obj.getString("id"),
            type = TrainingType.valueOf(obj.getString("type")),
            pilotName = obj.takeUnless { it.isNull("pilotName") }?.getString("pilotName"),
            channelLetter = obj.takeUnless { it.isNull("channelLetter") }?.getString("channelLetter"),
            channelNumber = obj.takeUnless { it.isNull("channelNumber") }?.getInt("channelNumber"),
            channelColor = obj.takeUnless { it.isNull("channelColor") }
                ?.getString("channelColor")
                ?.let { ChannelColor.valueOf(it) },
            createdAt = obj.getLong("createdAt")
        )
    }

    companion object {
        private const val FILENAME = "trainings.json"
    }
}
