package ru.fpvladder.laps.trainer.storage

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight

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
            flights = listOf(flight1, flight2)
        )

        val file = tempFolder.newFile("individual.yaml")
        TrainingStorage(file).save(training)
        println("===== INDIVIDUAL =====")
        println(file.readText())
    }

    @Test
    fun `print team training yaml`() {
        val laps1 = listOf(
            Lap(1, TimeInterval(0, 12000), success = true),
            Lap(2, TimeInterval(12000, 26000), success = true),
            Lap(3, TimeInterval(26000, 42000), success = true),
            Lap(4, TimeInterval(42000, 60000), success = true),
        )
        val laps2 = listOf(
            Lap(1, TimeInterval(0, 13000), success = true),
            Lap(2, TimeInterval(13000, 27000), success = true),
            Lap(3, TimeInterval(27000, 45000), success = true),
        )

        val flight1 = computeTeamFlight(laps1, StopReason.MANUAL, pilotChangeIndex = 1)
        val flight2 = computeTeamFlight(laps2, StopReason.MANUAL, pilotChangeIndex = null)

        val training = Training.Team(
            pilot = Pilot.Team("A", "B", Channel("B", 5, 0xFF2979FF.toInt())),
        ).copy(
            rules = Rules.Team(lapsLimit = 100, timeLimitSeconds = 600),
            flights = listOf(flight1, flight2)
        )

        val file = tempFolder.newFile("team.yaml")
        TrainingStorage(file).save(training)
        println("===== TEAM =====")
        println(file.readText())
    }
}
