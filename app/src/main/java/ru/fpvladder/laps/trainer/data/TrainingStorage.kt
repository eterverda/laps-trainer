package ru.fpvladder.laps.trainer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.fpvladder.laps.trainer.model.BestLap
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.SwapMode
import ru.fpvladder.laps.trainer.model.Training
import java.io.File
import java.util.EnumSet

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
        return rulesToJson(rules).apply {
            put("enabledBestLapKinds", JSONArray().apply {
                rules.enabledBestLapKinds.forEach { put(it.name) }
            })
        }
    }

    private fun teamRulesToJson(rules: Rules.Team): JSONObject {
        return rulesToJson(rules).apply {
            put("enabledBestLapKinds", JSONArray().apply {
                rules.enabledBestLapKinds.forEach { put(it.name) }
            })
        }
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
        val timeLimit = obj.optInt("timeLimitSeconds", 180)
        val defaultKinds = EnumSet.of(BestLap.Kind.BEST_1, BestLap.Kind.BEST_3).apply {
            if (timeLimit != Int.MAX_VALUE) add(BestLap.Kind.MOST)
        }
        val kindsArray = obj.optJSONArray("enabledBestLapKinds")
        val kinds = if (kindsArray != null) {
            EnumSet.noneOf(BestLap.Kind::class.java).apply {
                for (i in 0 until kindsArray.length()) {
                    try {
                        add(BestLap.Kind.valueOf(kindsArray.getString(i)))
                    } catch (_: Exception) {}
                }
            }
        } else defaultKinds
        return Rules.Individual(
            maxLaps = obj.optInt("maxLaps", Int.MAX_VALUE),
            timeLimitSeconds = timeLimit,
            holeshotEnabled = obj.optBoolean("holeshotEnabled", true),
            enabledBestLapKinds = kinds
        )
    }

    private fun teamRulesFromJson(obj: JSONObject): Rules.Team {
        val swapMode = when (val mode = obj.optString("swapMode", "")) {
            "TIME" -> SwapMode.TIME
            "LAPS" -> SwapMode.LAPS
            else -> if (obj.optBoolean("swapByTime", false)) SwapMode.TIME else SwapMode.LAPS
        }
        val maxLaps = obj.optInt("maxLaps", 10).coerceAtLeast(1)
        val timeLimit = obj.optInt("timeLimitSeconds", 60)
        val defaultKinds = EnumSet.of(BestLap.Kind.BEST_1, BestLap.Kind.MOST)
        val kindsArray = obj.optJSONArray("enabledBestLapKinds")
        val kinds = if (kindsArray != null) {
            EnumSet.noneOf(BestLap.Kind::class.java).apply {
                for (i in 0 until kindsArray.length()) {
                    try {
                        add(BestLap.Kind.valueOf(kindsArray.getString(i)))
                    } catch (_: Exception) {}
                }
            }
        } else defaultKinds
        return Rules.Team(
            maxLaps = maxLaps,
            timeLimitSeconds = timeLimit,
            holeshotEnabled = obj.optBoolean("holeshotEnabled", true),
            swapMode = swapMode,
            pilotOrderSwapped = obj.optBoolean("pilotOrderSwapped", false),
            enabledBestLapKinds = kinds
        )
    }

    private fun statsToJson(stats: Stats): JSONObject {
        return JSONObject().apply {
            when (stats) {
                is Stats.Individual -> {
                    put("type", "INDIVIDUAL")
                    put("bestLaps", JSONArray().apply {
                        stats.bestLaps.forEach { put(bestLapToJson(it)) }
                    })
                    put("counters", JSONArray().apply {
                        stats.counters.forEach { put(counterToJson(it)) }
                    })
                }
                is Stats.Team -> put("type", "TEAM")
            }
        }
    }

    private fun statsFromJson(obj: JSONObject?): Stats {
        if (obj == null) return Stats.Individual()
        return when (obj.optString("type", "INDIVIDUAL")) {
            "TEAM" -> Stats.Team()
            else -> Stats.Individual(
                bestLaps = bestLapsFromJson(obj.optJSONArray("bestLaps")),
                counters = countersFromJson(obj.optJSONArray("counters"))
            )
        }
    }

    private fun bestLapToJson(bestLap: BestLap): JSONObject {
        return JSONObject().apply {
            put("count", bestLap.count)
            put("timeMs", bestLap.timeMs)
            put("kind", bestLap.kind.name)
        }
    }

    private fun bestLapsFromJson(array: JSONArray?): List<BestLap> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            BestLap(
                count = obj.optInt("count", 0),
                timeMs = obj.optLong("timeMs", 0L),
                kind = try {
                    BestLap.Kind.valueOf(obj.optString("kind", "MOST"))
                } catch (_: Exception) {
                    BestLap.Kind.MOST
                }
            )
        }
    }

    private fun counterToJson(counter: Counter): JSONObject {
        return JSONObject().apply {
            put("count", counter.count)
            when (counter) {
                is Counter.Builtin -> {
                    put("type", "BUILTIN")
                    put("kind", counter.kind.name)
                }
                is Counter.Custom -> {
                    put("type", "CUSTOM")
                    put("text", counter.text)
                }
            }
        }
    }

    private fun countersFromJson(array: JSONArray?): List<Counter> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            val count = obj.optInt("count", 0)
            when (obj.optString("type", "")) {
                "CUSTOM" -> Counter.Custom(count = count, text = obj.optString("text", ""))
                else -> Counter.Builtin(
                    count = count,
                    kind = try {
                        Counter.Builtin.Kind.valueOf(obj.optString("kind", "LAP"))
                    } catch (_: Exception) {
                        Counter.Builtin.Kind.LAP
                    }
                )
            }
        }
    }

    companion object {
        private const val FILENAME = "trainings.json"
    }
}
