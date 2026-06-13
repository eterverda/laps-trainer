package ru.fpvladder.laps.trainer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsComputationTest {

    @Test
    fun `individual stats are computed from flights`() {
        val flight = Flight.Individual(
            laps = listOf(
                Lap(1, TimeInterval(0, 12000), success = true),
                Lap(2, TimeInterval(12000, 25000), success = true),
                Lap(3, TimeInterval(25000, 39000), success = true)
            ),
            stopReason = StopReason.MANUAL
        )
        val training = Training.Individual().copy(flights = listOf(flight))

        val stats = training.stats as Stats.Individual
        val recordKinds = stats.results.records.map { it.kind }
        assertTrue(Record.Kind.BEST_1 in recordKinds)
        assertTrue(Record.Kind.BEST_2 in recordKinds)
        assertTrue(Record.Kind.BEST_3 in recordKinds)
        assertTrue(Record.Kind.MOST in recordKinds)

        val lapCounter = stats.results.counters.filterIsInstance<Counter.Builtin>().single { it.kind == Counter.Builtin.Kind.LAP }
        assertEquals(3, lapCounter.count)
        val flightCounter = stats.results.counters.filterIsInstance<Counter.Builtin>().single { it.kind == Counter.Builtin.Kind.FLIGHT }
        assertEquals(1, flightCounter.count)
    }

    @Test
    fun `team stats map head and tail to first and second pilot`() {
        val flight = Flight.Team(
            headLaps = listOf(
                Lap(1, TimeInterval(0, 12000), success = true),
                Lap(2, TimeInterval(12000, 25000), success = true)
            ),
            tailLaps = listOf(
                Lap(3, TimeInterval(25000, 40000), success = true),
                Lap(4, TimeInterval(40000, 56000), success = true)
            ),
            stopReason = StopReason.MANUAL
        )
        val training = Training.Team().copy(
            rules = Rules.Team(swapMode = Rules.Team.SwapMode.STRAIGHT),
            flights = listOf(flight)
        )

        val stats = training.stats as Stats.Team
        assertEquals(4, stats.common.records.single().count)

        val firstBest1 = stats.first.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(1, firstBest1.count)
        assertEquals(TimeInterval(0, 12000), firstBest1.intervals.single())

        val secondBest1 = stats.second.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(1, secondBest1.count)
        assertEquals(TimeInterval(25000, 40000), secondBest1.intervals.single())
    }

    @Test
    fun `team stats swap pilots when swapMode is SWAPPED`() {
        val flight = Flight.Team(
            headLaps = listOf(
                Lap(1, TimeInterval(0, 12000), success = true)
            ),
            tailLaps = listOf(
                Lap(2, TimeInterval(12000, 26000), success = true)
            ),
            stopReason = StopReason.MANUAL,
            swapMode = Rules.Team.SwapMode.SWAPPED
        )
        val training = Training.Team().copy(
            flights = listOf(flight)
        )

        val stats = training.stats as Stats.Team
        val firstBest1 = stats.first.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(TimeInterval(12000, 26000), firstBest1.intervals.single())

        val secondBest1 = stats.second.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(TimeInterval(0, 12000), secondBest1.intervals.single())
    }

    @Test
    fun `each team flight keeps its own pilot order for stats`() {
        val flight1 = Flight.Team(
            headLaps = listOf(Lap(1, TimeInterval(0, 12000), success = true)),
            tailLaps = listOf(Lap(2, TimeInterval(12000, 25000), success = true)),
            stopReason = StopReason.MANUAL,
            swapMode = Rules.Team.SwapMode.STRAIGHT
        )
        val flight2 = Flight.Team(
            headLaps = listOf(Lap(3, TimeInterval(0, 11000), success = true)),
            tailLaps = listOf(Lap(4, TimeInterval(11000, 24000), success = true)),
            stopReason = StopReason.MANUAL,
            swapMode = Rules.Team.SwapMode.SWAPPED
        )
        val training = Training.Team().copy(flights = listOf(flight1, flight2))

        val stats = training.stats as Stats.Team
        val firstBest1 = stats.first.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(TimeInterval(0, 12000), firstBest1.intervals.single())

        val secondBest1 = stats.second.total.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(TimeInterval(0, 11000), secondBest1.intervals.single())
    }
}
