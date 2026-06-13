package ru.fpvladder.laps.trainer.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.Flight
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.StopReason
import ru.fpvladder.laps.trainer.model.TimeInterval
import ru.fpvladder.laps.trainer.model.Training
import java.util.EnumSet

class TrainingStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `serializes and deserializes individual training`() {
        val training = Training.Individual(
            pilot = Pilot.Individual(name = "Alex", channel = Channel(letter = "R", number = 1, color = 0xFFFF0000.toInt())),
        ).copy(
            rules = Rules.Individual(lapsLimit = 10, timeLimitSeconds = 120),
            flights = listOf(
                Flight.Individual(
                    laps = listOf(
                        Lap(
                            number = 1,
                            interval = TimeInterval(startMs = 1000, endMs = 15000),
                            success = true
                        )
                    ),
                    stopReason = StopReason.MANUAL
                )
            )
        )

        val file = tempFolder.newFile("training.yaml")
        val storage = TrainingStorage(file)

        storage.save(training)
        val loaded = storage.load()

        assertEquals(training, loaded)
    }

    @Test
    fun `serializes and deserializes team training`() {
        val training = Training.Team(
            pilot = Pilot.Team(name1 = "A", name2 = "B", channel = Channel(letter = "B", number = 5, color = 0xFF2979FF.toInt())),
        ).copy(
            rules = Rules.Team(
                lapsLimit = 50,
                changeMode = Rules.Team.ChangeMode.TIME,
                showRecordKinds = EnumSet.of(Record.Kind.BEST_1, Record.Kind.MOST)
            ),
            flights = listOf(
                Flight.Team(
                    headLaps = listOf(
                        Lap(0, TimeInterval(0, 0), success = true),
                        Lap(1, TimeInterval(0, 12000), success = true)
                    ),
                    tailLaps = listOf(
                        Lap(2, TimeInterval(12000, 26000), success = true)
                    ),
                    stopReason = StopReason.TIME_LIMIT
                )
            )
        )

        val file = tempFolder.newFile("team.yaml")
        val storage = TrainingStorage(file)

        storage.save(training)
        val loaded = storage.load()

        assertEquals(training, loaded)
    }

    @Test
    fun `yaml uses snake_case names and custom scalar formats`() {
        val training = Training.Individual(
            pilot = Pilot.Individual(name = "Alex", channel = Channel(letter = "R", number = 1, color = 0xFFFF0000.toInt())),
        ).copy(
            flights = listOf(
                Flight.Individual(
                    laps = listOf(
                        Lap(
                            number = 1,
                            interval = TimeInterval(startMs = 1000, endMs = 15000),
                            success = true
                        )
                    )
                )
            )
        )

        val file = tempFolder.newFile("format.yaml")
        TrainingStorage(file).save(training)
        val yaml = file.readText()

        assertTrue("expected max for laps_limit", yaml.contains("laps_limit: max"))
        assertTrue("expected snake_case time_limit_seconds", yaml.contains("time_limit_seconds:"))
        assertTrue("expected snake_case holeshot_enabled", yaml.contains("holeshot_enabled:"))
        assertTrue("expected time interval scalar", yaml.contains("1000 -> 15000"))
        assertTrue("expected channel scalar", yaml.contains("R1 ff0000"))
    }
}
