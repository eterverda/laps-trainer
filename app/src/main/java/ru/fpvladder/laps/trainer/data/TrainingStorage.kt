package ru.fpvladder.laps.trainer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
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
            put("flights", JSONArray().apply {
                training.flights.forEach { put(flightToJson(it)) }
            })
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
                flights = flightsIndividualFromJson(obj.optJSONArray("flights")),
                createdAt = createdAt,
                isArchived = isArchived
            )
            "TEAM" -> Training.Team(
                id = id,
                pilot = pilotTeamFromJson(obj.getJSONObject("pilot")),
                rules = teamRulesFromJson(obj.getJSONObject("rules")),
                stats = statsFromJson(obj.optJSONObject("stats")),
                flights = flightsTeamFromJson(obj.optJSONArray("flights")),
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
            put("enabledRecordKinds", JSONArray().apply {
                rules.enabledRecordKinds.forEach { put(it.name) }
            })
        }
    }

    private fun teamRulesToJson(rules: Rules.Team): JSONObject {
        return rulesToJson(rules).apply {
            put("enabledRecordKinds", JSONArray().apply {
                rules.enabledRecordKinds.forEach { put(it.name) }
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
        val defaultKinds = EnumSet.of(Record.Kind.BEST_1, Record.Kind.BEST_3).apply {
            if (timeLimit != Int.MAX_VALUE) add(Record.Kind.MOST)
        }
        val kindsArray = obj.optJSONArray("enabledRecordKinds")
        val kinds = if (kindsArray != null) {
            EnumSet.noneOf(Record.Kind::class.java).apply {
                for (i in 0 until kindsArray.length()) {
                    try {
                        add(Record.Kind.valueOf(kindsArray.getString(i)))
                    } catch (_: Exception) {}
                }
            }
        } else defaultKinds
        return Rules.Individual(
            maxLaps = obj.optInt("maxLaps", Int.MAX_VALUE),
            timeLimitSeconds = timeLimit,
            holeshotEnabled = obj.optBoolean("holeshotEnabled", true),
            enabledRecordKinds = kinds
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
        val defaultKinds = EnumSet.of(Record.Kind.BEST_1, Record.Kind.MOST)
        val kindsArray = obj.optJSONArray("enabledRecordKinds")
        val kinds = if (kindsArray != null) {
            EnumSet.noneOf(Record.Kind::class.java).apply {
                for (i in 0 until kindsArray.length()) {
                    try {
                        add(Record.Kind.valueOf(kindsArray.getString(i)))
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
            enabledRecordKinds = kinds
        )
    }

    private fun statsToJson(stats: Stats): JSONObject {
        return JSONObject().apply {
            when (stats) {
                is Stats.Individual -> {
                    put("type", "INDIVIDUAL")
                    put("records", JSONArray().apply {
                        stats.records.forEach { put(recordToJson(it)) }
                    })
                    put("counters", JSONArray().apply {
                        stats.counters.forEach { put(counterToJson(it)) }
                    })
                }
                is Stats.Team -> {
                    put("type", "TEAM")
                    put("records", JSONArray().apply { stats.records.forEach { put(recordToJson(it)) } })
                    put("counters", JSONArray().apply { stats.counters.forEach { put(counterToJson(it)) } })
                    put("first", teamStatsItemToJson(stats.first))
                    put("second", teamStatsItemToJson(stats.second))
                }
            }
        }
    }

    private fun statsFromJson(obj: JSONObject?): Stats {
        if (obj == null) return Stats.Individual()
        return when (obj.optString("type", "INDIVIDUAL")) {
            "TEAM" -> Stats.Team(
                records = recordsFromJson(obj.optJSONArray("records")),
                counters = countersFromJson(obj.optJSONArray("counters")),
                first = teamStatsItemFromJson(obj.optJSONObject("first")),
                second = teamStatsItemFromJson(obj.optJSONObject("second"))
            )
            else -> Stats.Individual(
                records = recordsFromJson(obj.optJSONArray("records")),
                counters = countersFromJson(obj.optJSONArray("counters"))
            )
        }
    }

    private fun flightToJson(flight: Flight): JSONObject {
        return JSONObject().apply {
            put("id", flight.id)
            put("trainingId", flight.trainingId)
            put("rules", when (val rules = flight.rules) {
                is Rules.Individual -> individualRulesToJson(rules)
                is Rules.Team -> teamRulesToJson(rules)
            })
            put("pilot", when (val pilot = flight.pilot) {
                is Pilot.Individual -> pilotIndividualToJson(pilot)
                is Pilot.Team -> pilotTeamToJson(pilot)
            })
            put("laps", JSONArray().apply { flight.laps.forEach { put(lapToJson(it)) } })
            put("stopReason", flight.stopReason.name)
            put("createdAt", flight.createdAt)
            put("completedAt", flight.completedAt ?: JSONObject.NULL)
            put("type", when (flight) {
                is Flight.Individual -> "INDIVIDUAL"
                is Flight.Team -> "TEAM"
            })
            when (flight) {
                is Flight.Individual -> {
                    put("records", JSONArray().apply { flight.records.forEach { put(recordToJson(it)) } })
                    put("counters", JSONArray().apply { flight.counters.forEach { put(counterToJson(it)) } })
                }
                is Flight.Team -> {
                    put("pilotSwapIndex", flight.pilotSwapIndex ?: JSONObject.NULL)
                    put("common", teamFlightItemToJson(flight.common))
                    put("head", teamFlightItemToJson(flight.head))
                    put("tail", teamFlightItemToJson(flight.tail))
                }
            }
        }
    }

    private fun flightsIndividualFromJson(array: JSONArray?): List<Flight.Individual> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            flightFromJson(array.getJSONObject(index)) as Flight.Individual
        }
    }

    private fun flightsTeamFromJson(array: JSONArray?): List<Flight.Team> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            flightFromJson(array.getJSONObject(index)) as Flight.Team
        }
    }

    private fun flightFromJson(obj: JSONObject): Flight {
        val type = obj.getString("type")
        val id = obj.getString("id")
        val trainingId = obj.getString("trainingId")
        val createdAt = obj.getLong("createdAt")
        val completedAt = if (obj.isNull("completedAt")) null else obj.getLong("completedAt")
        val stopReason = try {
            StopReason.valueOf(obj.optString("stopReason", "MANUAL"))
        } catch (_: Exception) {
            StopReason.MANUAL
        }
        val laps = lapsFromJson(obj.optJSONArray("laps"))
        return when (type) {
            "INDIVIDUAL" -> Flight.Individual(
                id = id,
                trainingId = trainingId,
                pilot = pilotIndividualFromJson(obj.getJSONObject("pilot")),
                rules = individualRulesFromJson(obj.getJSONObject("rules")),
                laps = laps,
                stopReason = stopReason,
                records = recordsFromJson(obj.optJSONArray("records")),
                counters = countersFromJson(obj.optJSONArray("counters")),
                createdAt = createdAt,
                completedAt = completedAt
            )
            "TEAM" -> Flight.Team(
                id = id,
                trainingId = trainingId,
                pilot = pilotTeamFromJson(obj.getJSONObject("pilot")),
                rules = teamRulesFromJson(obj.getJSONObject("rules")),
                laps = laps,
                stopReason = stopReason,
                pilotSwapIndex = if (obj.isNull("pilotSwapIndex")) null else obj.getInt("pilotSwapIndex"),
                common = teamFlightItemFromJson(obj.optJSONObject("common")),
                head = teamFlightItemFromJson(obj.optJSONObject("head") ?: obj.optJSONObject("first")),
                tail = teamFlightItemFromJson(obj.optJSONObject("tail") ?: obj.optJSONObject("second")),
                createdAt = createdAt,
                completedAt = completedAt
            )
            else -> throw IllegalArgumentException("Unknown flight type: $type")
        }
    }

    private fun teamStatsItemToJson(item: Stats.Team.Item): JSONObject {
        return JSONObject().apply {
            put("records", JSONArray().apply { item.records.forEach { put(recordToJson(it)) } })
            put("recordsBeingHead", JSONArray().apply { item.recordsBeingHead.forEach { put(recordToJson(it)) } })
            put("recordsBeingTail", JSONArray().apply { item.recordsBeingTail.forEach { put(recordToJson(it)) } })
            put("counters", JSONArray().apply { item.counters.forEach { put(counterToJson(it)) } })
        }
    }

    private fun teamStatsItemFromJson(obj: JSONObject?): Stats.Team.Item {
        if (obj == null) return Stats.Team.Item()
        return Stats.Team.Item(
            records = recordsFromJson(obj.optJSONArray("records")),
            recordsBeingHead = recordsFromJson(obj.optJSONArray("recordsBeingHead") ?: obj.optJSONArray("recordsBeingFirst")),
            recordsBeingTail = recordsFromJson(obj.optJSONArray("recordsBeingTail") ?: obj.optJSONArray("recordsBeingSecond")),
            counters = countersFromJson(obj.optJSONArray("counters"))
        )
    }

    private fun teamFlightItemToJson(item: Flight.Team.Item): JSONObject {
        return JSONObject().apply {
            put("records", JSONArray().apply { item.records.forEach { put(recordToJson(it)) } })
            put("counters", JSONArray().apply { item.counters.forEach { put(counterToJson(it)) } })
        }
    }

    private fun teamFlightItemFromJson(obj: JSONObject?): Flight.Team.Item {
        if (obj == null) return Flight.Team.Item()
        return Flight.Team.Item(
            records = recordsFromJson(obj.optJSONArray("records")),
            counters = countersFromJson(obj.optJSONArray("counters"))
        )
    }

    private fun lapToJson(lap: Lap): JSONObject {
        return JSONObject().apply {
            put("label", lap.label)
            put("interval", JSONArray().apply {
                put(lap.startMs)
                put(lap.endMs)
            })
            put("status", lap.status.name)
        }
    }

    private fun lapsFromJson(array: JSONArray?): List<Lap> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            val intervalArray = obj.getJSONArray("interval")
            val interval = TimeInterval(intervalArray.getLong(0), intervalArray.getLong(1))
            Lap(
                label = obj.optString("label", ""),
                interval = interval,
                status = try {
                    Lap.Status.valueOf(obj.optString("status", "SUCCESS"))
                } catch (_: Exception) {
                    Lap.Status.SUCCESS
                }
            )
        }
    }

    private fun recordToJson(record: Record): JSONObject {
        return JSONObject().apply {
            put("count", record.count)
            put("kind", record.kind.name)
            put("intervals", JSONArray().apply {
                record.intervals.forEach { interval ->
                    put(JSONArray().apply {
                        put(interval.startMs)
                        put(interval.endMs)
                    })
                }
            })
        }
    }

    private fun recordsFromJson(array: JSONArray?): List<Record> {
        if (array == null) return emptyList()
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            val intervalsArray = obj.optJSONArray("intervals")
            val intervals = if (intervalsArray != null) {
                List(intervalsArray.length()) { i ->
                    val interval = intervalsArray.getJSONArray(i)
                    TimeInterval(interval.getLong(0), interval.getLong(1))
                }
            } else emptyList()
            Record(
                count = obj.optInt("count", 0),
                kind = try {
                    Record.Kind.valueOf(obj.optString("kind", "MOST"))
                } catch (_: Exception) {
                    Record.Kind.MOST
                },
                intervals = intervals
            )
        }
    }

    private fun counterToJson(counter: Counter): JSONObject {
        return JSONObject().apply {
            put("count", counter.count)
            val kind = counter.kind
            if (kind != null) {
                put("type", "BUILTIN")
                put("kind", kind.name)
            } else {
                put("type", "CUSTOM")
                put("text", (counter as Counter.Custom).text)
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
                        Counter.Kind.valueOf(obj.optString("kind", "LAP"))
                    } catch (_: Exception) {
                        Counter.Kind.LAP
                    }
                )
            }
        }
    }

    companion object {
        private const val FILENAME = "trainings.json"
    }
}
