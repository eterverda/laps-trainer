package ru.fpvladder.laps.trainer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlightResultsTest {

    @Test
    fun `best 2 ignores non consecutive valid laps separated by error`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = false),
            Lap(3, TimeInterval(3000, 5000), success = true),
            Lap(4, TimeInterval(5000, 6500), success = true)
        )

        val results = computeFlightResults(laps)

        val best2 = results.records.single { it.kind == Record.Kind.BEST_2 }
        assertEquals(3500L, best2.rawTimeMs())
        assertEquals(listOf(3, 4), best2.intervals.map { intervalToLapNumber(laps, it) })
    }

    @Test
    fun `best 3 requires three consecutive valid laps`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = true),
            Lap(3, TimeInterval(3000, 5000), success = false),
            Lap(4, TimeInterval(5000, 7000), success = true),
            Lap(5, TimeInterval(7000, 9000), success = true)
        )

        val results = computeFlightResults(laps)

        assertNull(results.records.singleOrNull { it.kind == Record.Kind.BEST_3 })
    }

    @Test
    fun `best 3 picks best consecutive triple`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = true),
            Lap(3, TimeInterval(3000, 6000), success = true),
            Lap(4, TimeInterval(6000, 7500), success = false),
            Lap(5, TimeInterval(7500, 8500), success = true),
            Lap(6, TimeInterval(8500, 9500), success = true),
            Lap(7, TimeInterval(9500, 10500), success = true)
        )

        val results = computeFlightResults(laps)

        val best3 = results.records.single { it.kind == Record.Kind.BEST_3 }
        assertEquals(3000L, best3.rawTimeMs())
        assertEquals(listOf(5, 6, 7), best3.intervals.map { intervalToLapNumber(laps, it) })
    }

    @Test
    fun `best 1 still ignores errors and picks fastest valid lap`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = false),
            Lap(3, TimeInterval(3000, 5000), success = true)
        )

        val results = computeFlightResults(laps)

        val best1 = results.records.single { it.kind == Record.Kind.BEST_1 }
        assertEquals(1000L, best1.rawTimeMs())
    }

    @Test
    fun `legacy best 2 allows skipping error laps`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = false),
            Lap(3, TimeInterval(3000, 5000), success = true),
            Lap(4, TimeInterval(5000, 6500), success = true)
        )

        val results = computeFlightResults(laps, consecutiveBest = false)

        val best2 = results.records.single { it.kind == Record.Kind.BEST_2 }
        assertEquals(3000L, best2.rawTimeMs())
        assertEquals(listOf(1, 3), best2.intervals.map { intervalToLapNumber(laps, it) })
        assertEquals(false, best2.allSuccess)
    }

    @Test
    fun `legacy best 3 allows skipping error laps`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = true),
            Lap(3, TimeInterval(3000, 5000), success = false),
            Lap(4, TimeInterval(5000, 7000), success = true),
            Lap(5, TimeInterval(7000, 9000), success = true)
        )

        val results = computeFlightResults(laps, consecutiveBest = false)

        val best3 = results.records.single { it.kind == Record.Kind.BEST_3 }
        assertEquals(5000L, best3.rawTimeMs())
        assertEquals(listOf(1, 2, 4), best3.intervals.map { intervalToLapNumber(laps, it) })
        assertEquals(false, best3.allSuccess)
    }

    @Test
    fun `legacy best 2 still picks consecutive valid laps when they are best`() {
        val laps = listOf(
            Lap(1, TimeInterval(0, 1000), success = true),
            Lap(2, TimeInterval(1000, 3000), success = true),
            Lap(3, TimeInterval(3000, 5000), success = true)
        )

        val results = computeFlightResults(laps, consecutiveBest = false)

        val best2 = results.records.single { it.kind == Record.Kind.BEST_2 }
        assertEquals(3000L, best2.rawTimeMs())
        assertEquals(listOf(1, 2), best2.intervals.map { intervalToLapNumber(laps, it) })
        assertEquals(true, best2.allSuccess)
    }

    private fun intervalToLapNumber(laps: List<Lap>, interval: TimeInterval): Int {
        return laps.first { it.interval == interval }.number
    }
}
