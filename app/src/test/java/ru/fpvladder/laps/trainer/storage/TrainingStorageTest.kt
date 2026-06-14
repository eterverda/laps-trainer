package ru.fpvladder.laps.trainer.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.io.File
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

        val storage = TrainingStorage(tempFolder.root)

        storage.save(training)
        val loaded = storage.loadAll().single()

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
                        Lap(1, TimeInterval(0, 12000), success = true)
                    ),
                    tailLaps = listOf(
                        Lap(2, TimeInterval(12000, 26000), success = true)
                    ),
                    stopReason = StopReason.TIME_LIMIT
                )
            )
        )

        val storage = TrainingStorage(tempFolder.root)

        storage.save(training)
        val loaded = storage.loadAll().single()

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

        val storage = TrainingStorage(tempFolder.root)
        storage.save(training)
        val yaml = File(tempFolder.root, "${training.id}.yaml").readText()

        assertTrue("expected max for laps_limit", yaml.contains("laps_limit: max"))
        assertTrue("expected snake_case time_limit_seconds", yaml.contains("time_limit_seconds:"))
        assertTrue("expected snake_case holeshot_enabled", yaml.contains("holeshot_enabled:"))
        assertTrue("expected time interval scalar", yaml.contains("1000 -> 15000"))
        assertTrue("expected channel scalar", yaml.contains("R1 ff0000"))
    }

    @Test
    fun `default training is never saved`() {
        val storage = TrainingStorage(tempFolder.root)

        assertTrue(Training.DEFAULT.isDefault())
        storage.save(Training.DEFAULT)

        assertTrue("storage dir should not be created for default training", !tempFolder.root.exists() || tempFolder.root.listFiles().isNullOrEmpty())
        assertEquals(emptyList<Training>(), storage.loadAll())
    }

    @Test
    fun `touch updates file modification time`() {
        val training = Training.Individual(
            pilot = Pilot.Individual(name = "X"),
        )
        val storage = TrainingStorage(tempFolder.root)
        storage.save(training)

        val file = File(tempFolder.root, "${training.id}.yaml")
        val before = System.currentTimeMillis() - 10_000
        file.setLastModified(before)
        storage.touch(training)
        val after = file.lastModified()

        assertTrue("touch should update modification time", after > before)
    }

    @Test
    fun `touch does nothing for default training`() {
        val storage = TrainingStorage(tempFolder.root)
        storage.touch(Training.DEFAULT)
        assertTrue(!tempFolder.root.exists() || tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `loadAll returns trainings sorted by modification time descending`() {
        val first = Training.Individual(pilot = Pilot.Individual(name = "First"))
        val second = Training.Individual(pilot = Pilot.Individual(name = "Second"))
        val third = Training.Individual(pilot = Pilot.Individual(name = "Third"))

        val storage = TrainingStorage(tempFolder.root)
        storage.save(first)
        storage.save(second)
        storage.save(third)

        val now = System.currentTimeMillis()
        File(tempFolder.root, "${first.id}.yaml").setLastModified(now - 30_000)
        File(tempFolder.root, "${second.id}.yaml").setLastModified(now - 20_000)
        File(tempFolder.root, "${third.id}.yaml").setLastModified(now - 10_000)

        storage.touch(first)

        val loaded = storage.loadAll()
        assertEquals(listOf(first.id, third.id, second.id), loaded.map { it.id })
    }

    @Test
    fun `non default empty-named training is still saved`() {
        val training = Training.Individual().copy(
            flights = listOf(
                Flight.Individual(
                    laps = listOf(Lap(1, TimeInterval(0, 1000), success = true))
                )
            )
        )

        val storage = TrainingStorage(tempFolder.root)
        storage.save(training)

        assertFalse(training.isDefault())
        assertEquals(listOf(training), storage.loadAll())
    }
}
