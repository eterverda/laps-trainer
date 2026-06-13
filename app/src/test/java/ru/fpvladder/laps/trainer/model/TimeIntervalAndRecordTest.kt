package ru.fpvladder.laps.trainer.model

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.helpers.timeMs

class TimeIntervalAndRecordTest {

    @Test
    fun `TimeInterval duration is end minus start`() {
        val interval = TimeInterval(startMs = 12340L, endMs = 24680L)
        assertEquals(12340L, interval.durationMs)
    }

    @Test
    fun `Lap exposes derived start end and time`() {
        val lap = Lap(
            number = 1,
            interval = TimeInterval(startMs = 1000L, endMs = 12450L),
            success = true
        )
        assertEquals(1000L, lap.startMs)
        assertEquals(12450L, lap.endMs)
        assertEquals(11450L, lap.durationMs)
    }

    @Test
    fun `Results Record rawTimeMs sums interval durations`() {
        val record = Record(
            count = 2,
            kind = Record.Kind.BEST_2,
            intervals = listOf(
                TimeInterval(1000L, 12340L),
                TimeInterval(12340L, 24680L)
            )
        )
        assertEquals(23680L, record.rawTimeMs())
    }

    @Test
    fun `Results Record timeMs rounds marks not intervals`() {
        // Duration-based rounding would round (end-start) of each lap.
        // Mark-based rounding rounds start and end first, then subtracts.
        // These can differ when the interval straddles a rounding boundary.
        val record = Record(
            count = 1,
            kind = Record.Kind.BEST_1,
            intervals = listOf(
                TimeInterval(startMs = 12344L, endMs = 24685L)
            )
        )
        val precision = TimerPrecision.CENTISECONDS // 10ms
        // duration = 12341 -> rounded duration = 12340
        assertEquals(12340L, precision.roundMs(12341L))
        // rounded marks: 12344 -> 12340, 24685 -> 24690 => diff = 12350
        assertEquals(12350L, record.timeMs(precision))
    }

    @Test
    fun `Results Record timeMs with multiple intervals is stable`() {
        val record = Record(
            count = 3,
            kind = Record.Kind.MOST,
            intervals = listOf(
                TimeInterval(0L, 12340L),
                TimeInterval(12340L, 24685L),
                TimeInterval(24685L, 37025L)
            )
        )
        val precision = TimerPrecision.CENTISECONDS
        val expected =
            (precision.roundMs(12340L) - precision.roundMs(0L)) +
            (precision.roundMs(24685L) - precision.roundMs(12340L)) +
            (precision.roundMs(37025L) - precision.roundMs(24685L))
        assertEquals(expected, record.timeMs(precision))
    }

    @Test
    fun `computeIndividualFlight builds records from intervals`() {
        val laps = listOf(
            Lap(1, TimeInterval(0L, 12340L), success = true),
            Lap(2, TimeInterval(12340L, 24680L), success = true),
            Lap(3, TimeInterval(24680L, 37030L), success = true)
        )
        val results = computeIndividualFlight(laps, StopReason.MANUAL).results
        val records = results.records

        val best1 = records.first { it.kind == Record.Kind.BEST_1 }
        assertEquals(1, best1.count)
        assertEquals(1, best1.intervals.size)
        assertEquals(12340L, best1.intervals.single().durationMs)

        val most = records.first { it.kind == Record.Kind.MOST }
        assertEquals(3, most.count)
        assertEquals(3, most.intervals.size)
        assertEquals(37030L, most.rawTimeMs())

        val lapCounter = results.counters.single()
        assertEquals(Counter.Builtin.Kind.LAP, lapCounter.kind)
        assertEquals(3, lapCounter.count)
    }

    @Test
    fun `MOST intervals include failed laps but count only successful ones`() {
        val laps = listOf(
            Lap(0, TimeInterval(0L, 0L), success = true),
            Lap(1, TimeInterval(0L, 12000L), success = true),
            Lap(2, TimeInterval(12000L, 25000L), success = false),
            Lap(3, TimeInterval(25000L, 37000L), success = true)
        )
        val records = computeIndividualFlight(laps, StopReason.MANUAL).results.records
        val most = records.single { it.kind == Record.Kind.MOST }
        assertEquals(2, most.count)
        assertEquals(listOf(12000L, 13000L, 12000L), most.intervals.map { it.durationMs })
    }

    @Test
    fun `Results Record isBetterThan uses rawTimeMs`() {
        val a = Record(
            count = 2,
            kind = Record.Kind.BEST_2,
            intervals = listOf(
                TimeInterval(0L, 1000L),
                TimeInterval(1000L, 2100L)
            )
        )
        val b = Record(
            count = 2,
            kind = Record.Kind.BEST_2,
            intervals = listOf(
                TimeInterval(0L, 1100L),
                TimeInterval(1100L, 2200L)
            )
        )
        assertEquals(true, a.isBetterThan(b))
    }
}
