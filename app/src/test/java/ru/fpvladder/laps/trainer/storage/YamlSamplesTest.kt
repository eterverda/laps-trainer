package ru.fpvladder.laps.trainer.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.NO_PILOT_CHANGE
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.Training
import ru.fpvladder.laps.trainer.model.computeIndividualFlight
import ru.fpvladder.laps.trainer.model.computeTeamFlight
import java.io.File
import java.util.EnumSet

class YamlSamplesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `reads all sample training yaml files from test resources`() {
        val resourceDir = File(javaClass.classLoader!!.getResource("trainings")!!.file)
        val storage = TrainingStorage(resourceDir)

        val loaded = storage.loadAll()

        assertEquals(3, loaded.size)
        loaded.forEach { training ->
            assertFalse(training.isDefault())
            assertTrue(training.flights.isNotEmpty())
        }
    }

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
            rules = Rules.Individual(
                lapsLimit = 50,
                timeLimitSeconds = 180,
                holeshotEnabled = true,
                showRecordKinds = EnumSet.of(
                    Record.Kind.BEST_1,
                    Record.Kind.BEST_3,
                    Record.Kind.MOST
                )
            )
        ).copy(
            flights = listOf(flight1, flight2)
        )

        TrainingStorage(tempFolder.root).save(training)
        println("===== INDIVIDUAL =====")
        println(File(tempFolder.root, "${training.id}.yaml").readText())
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

        val flight1 = computeTeamFlight(
            laps1,
            StopReason.MANUAL,
            pilotChangeIndex = 1,
            swapMode = Rules.Team.SwapMode.STRAIGHT
        )
        val flight2 = computeTeamFlight(
            laps2,
            StopReason.MANUAL,
            pilotChangeIndex = NO_PILOT_CHANGE,
            swapMode = Rules.Team.SwapMode.STRAIGHT
        )

        val training = Training.Team(
            pilot = Pilot.Team("A", "B", Channel("B", 5, 0xFF2979FF.toInt())),
            rules = Rules.Team(
                lapsLimit = 100,
                timeLimitSeconds = 600,
                holeshotEnabled = false,
                changeMode = Rules.Team.ChangeMode.LAPS,
                swapMode = Rules.Team.SwapMode.STRAIGHT,
                showRecordKinds = EnumSet.of(
                    Record.Kind.BEST_1,
                    Record.Kind.MOST
                )
            )
        ).copy(
            flights = listOf(flight1, flight2)
        )

        TrainingStorage(tempFolder.root).save(training)
        println("===== TEAM =====")
        println(File(tempFolder.root, "${training.id}.yaml").readText())
    }
}
