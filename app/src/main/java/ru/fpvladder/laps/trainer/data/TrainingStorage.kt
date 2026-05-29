package ru.fpvladder.laps.trainer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.SwapMode
import ru.fpvladder.laps.trainer.model.Training
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
            put("createdAt", training.createdAt)
            put("isArchived", training.isArchived)
            when (training) {
                is Training.Individual -> {
                    put("type", "INDIVIDUAL")
                    put("pilot", pilotIndividualToJson(training.pilot))
                    put("rules", individualRulesToJson(training.rules))
                }
                is Training.Team -> {
                    put("type", "TEAM")
                    put("pilot", pilotTeamToJson(training.pilot))
                    put("rules", teamRulesToJson(training.rules))
                }
            }
            put("stats", statsToJson(training.stats))
        }
    }

    private fun fromJson(obj: JSONObject): Training {
        val type = obj.getString("type")
        val id = obj.getString("id")
        val createdAt = obj.getLong("createdAt")
        val isArchived = obj.optBoolean("isArchived", false)
        return when (type) {
            "INDIVIDUAL" -> Training.Individual(
                id = id,
                pilot = pilotIndividualFromJson(obj.getJSONObject("pilot")),
                rules = individualRulesFromJson(obj.getJSONObject("rules")),
                stats = statsFromJson(obj.optJSONObject("stats")),
                createdAt = createdAt,
                isArchived = isArchived
            )
            "TEAM" -> Training.Team(
                id = id,
                pilot = pilotTeamFromJson(obj.getJSONObject("pilot")),
                rules = teamRulesFromJson(obj.getJSONObject("rules")),
                stats = statsFromJson(obj.optJSONObject("stats")),
                createdAt = createdAt,
                isArchived = isArchived
            )
            else -> throw IllegalArgumentException("Unknown training type: $type")
        }
    }

    private fun pilotIndividualToJson(pilot: Pilot.Individual): JSONObject {
        return JSONObject().apply {
            put("name", pilot.name)
            put("channel", channelToJson(pilot.channel))
        }
    }

    private fun pilotIndividualFromJson(obj: JSONObject): Pilot.Individual {
        return Pilot.Individual(
            name = obj.optString("name", ""),
            channel = channelFromJson(obj.optJSONObject("channel"))
        )
    }

    private fun pilotTeamToJson(pilot: Pilot.Team): JSONObject {
        return JSONObject().apply {
            put("name1", pilot.name1)
            put("name2", pilot.name2)
            put("channel", channelToJson(pilot.channel))
        }
    }

    private fun pilotTeamFromJson(obj: JSONObject): Pilot.Team {
        return Pilot.Team(
            name1 = obj.optString("name1", ""),
            name2 = obj.optString("name2", ""),
            channel = channelFromJson(obj.optJSONObject("channel"))
        )
    }

    private fun channelToJson(channel: Channel): JSONObject {
        return JSONObject().apply {
            put("letter", channel.letter)
            put("number", channel.number)
            put("color", channel.color.name)
        }
    }

    private fun channelFromJson(obj: JSONObject?): Channel {
        if (obj == null) return Channel()
        return Channel(
            letter = obj.optString("letter", "R"),
            number = obj.optInt("number", 1),
            color = obj.optString("color", "RED").let { ChannelColor.valueOf(it) }
        )
    }

    private fun individualRulesToJson(rules: Rules.Individual): JSONObject {
        return rulesToJson(rules)
    }

    private fun teamRulesToJson(rules: Rules.Team): JSONObject {
        return rulesToJson(rules)
    }

    private fun rulesToJson(rules: Rules): JSONObject {
        return JSONObject().apply {
            put("maxLaps", rules.maxLaps)
            put("timeLimitSeconds", rules.timeLimitSeconds)
            put("holeshotEnabled", rules.holeshotEnabled)
            if (rules is Rules.Team) {
                put("swapMode", rules.swapMode.name)
                put("pilotOrderSwapped", rules.pilotOrderSwapped)
            }
        }
    }

    private fun individualRulesFromJson(obj: JSONObject): Rules.Individual {
        return Rules.Individual(
            maxLaps = obj.optInt("maxLaps", Int.MAX_VALUE),
            timeLimitSeconds = obj.optInt("timeLimitSeconds", 180),
            holeshotEnabled = obj.optBoolean("holeshotEnabled", true)
        )
    }

    private fun teamRulesFromJson(obj: JSONObject): Rules.Team {
        val swapMode = when (val mode = obj.optString("swapMode", "")) {
            "TIME" -> SwapMode.TIME
            "LAPS" -> SwapMode.LAPS
            else -> if (obj.optBoolean("swapByTime", true)) SwapMode.TIME else SwapMode.LAPS
        }
        return Rules.Team(
            maxLaps = obj.optInt("maxLaps", Int.MAX_VALUE),
            timeLimitSeconds = obj.optInt("timeLimitSeconds", 180),
            holeshotEnabled = obj.optBoolean("holeshotEnabled", true),
            swapMode = swapMode,
            pilotOrderSwapped = obj.optBoolean("pilotOrderSwapped", false)
        )
    }

    private fun statsToJson(stats: Stats): JSONObject {
        return JSONObject().apply {
            when (stats) {
                is Stats.Individual -> put("type", "INDIVIDUAL")
                is Stats.Team -> put("type", "TEAM")
            }
        }
    }

    private fun statsFromJson(obj: JSONObject?): Stats {
        if (obj == null) return Stats.Individual()
        return when (obj.optString("type", "INDIVIDUAL")) {
            "TEAM" -> Stats.Team()
            else -> Stats.Individual()
        }
    }

    companion object {
        private const val FILENAME = "trainings.json"
    }
}
