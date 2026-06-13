package ru.fpvladder.laps.trainer.storage

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Results
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight
import ru.fpvladder.laps.trainer.model.mergeCounterLists
import ru.fpvladder.laps.trainer.model.mergeFlightRecords
import ru.fpvladder.laps.trainer.model.mergeRecordLists
import ru.fpvladder.laps.trainer.model.mergeTeamFlight

private fun mergeResults(a: Results, b: Results): Results = Results(
    records = mergeRecordLists(a.records, b.records),
    counters = mergeCounterLists(a.counters, b.counters)
)

class YamlSamplesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `print individual training yaml`() {
        val laps1 = listOf(
            Lap(0, TimeInterval(0, 0), success = true),
            Lap(1, TimeInterval(0, 12000), success = true),
            Lap(2, TimeInterval(12000, 25000), success = true),
            Lap(3, TimeInterval(25000, 41000), success = true),
        )
        val laps2 = listOf(
            Lap(0, TimeInterval(0, 0), success = true),
            Lap(1, TimeInterval(0, 11000), success = true),
            Lap(2, TimeInterval(11000, 24000), success = true),
        )

        val flight1 = computeIndividualFlight(laps1, StopReason.MANUAL)
        val flight2 = computeIndividualFlight(laps2, StopReason.MANUAL)

        val training = Training.Individual(
            pilot = Pilot.Individual("Alex", Channel("R", 1, 0xFFFF0000.toInt())),
        ).copy(
            rules = Rules.Individual(lapsLimit = 50, timeLimitSeconds = 180),
            flights = listOf(flight1, flight2),
            stats = Stats.Individual().mergeFlightRecords(flight1.result).mergeFlightRecords(flight2.result)
        )

        val file = tempFolder.newFile("individual.yaml")
        TrainingStorage(file).save(training)
        println("===== INDIVIDUAL =====")
        println(file.readText())
    }

    @Test
    fun `print team training yaml`() {
        val laps1 = listOf(
            Lap(0, TimeInterval(0, 0), success = true),
            Lap(1, TimeInterval(0, 12000), success = true),
            Lap(2, TimeInterval(12000, 26000), success = true),
            Lap(3, TimeInterval(26000, 42000), success = true),
            Lap(4, TimeInterval(42000, 60000), success = true),
        )
        val laps2 = listOf(
            Lap(0, TimeInterval(0, 0), success = true),
            Lap(1, TimeInterval(0, 13000), success = true),
            Lap(2, TimeInterval(13000, 27000), success = true),
            Lap(3, TimeInterval(27000, 45000), success = true),
        )

        val flight1 = computeTeamFlight(laps1, StopReason.MANUAL, pilotSwapIndex = 2)
        val flight2 = computeTeamFlight(laps2, StopReason.MANUAL, pilotSwapIndex = 2)

        val customCounter = Counter.Custom(count = 1, text = "bonus")
        val flight1WithCustom = flight1.copy(
            common = flight1.common.copy(counters = flight1.common.counters + customCounter)
        )
        val flight2WithCustom = flight2.copy(
            common = flight2.common.copy(counters = flight2.common.counters + customCounter)
        )

        val training = Training.Team(
            pilot = Pilot.Team("A", "B", Channel("B", 5, 0xFF2979FF.toInt())),
        ).copy(
            rules = Rules.Team(lapsLimit = 100, timeLimitSeconds = 600),
            flights = listOf(flight1WithCustom, flight2WithCustom),
            stats = Stats.Team().mergeTeamFlight(
                common = mergeResults(flight1.common, flight2.common),
                head = mergeResults(flight1.head, flight2.head),
                tail = mergeResults(flight1.tail, flight2.tail),
                pilotOrderSwapped = false
            )
        )

        val file = tempFolder.newFile("team.yaml")
        TrainingStorage(file).save(training)
        println("===== TEAM =====")
        println(file.readText())
    }
}
