package ru.fpvladder.laps.trainer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeIntervalAndRecordTest {

    @Test
    fun `TimeInterval duration is end minus start`() {
        val interval = TimeInterval(startMs = 12340L, endMs = 24680L)
        assertEquals(12340L, interval.durationMs)
    }

    @Test
    fun `Lap exposes derived start end and time`() {
        val lap = Lap(
            label = "1)",
            interval = TimeInterval(startMs = 1000L, endMs = 12450L),
            status = Lap.Status.SUCCESS
        )
        assertEquals(1000L, lap.startMs)
        assertEquals(12450L, lap.endMs)
        assertEquals(11450L, lap.timeMs)
    }

    @Test
    fun `Record rawTimeMs sums interval durations`() {
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
    fun `Record timeMs rounds marks not intervals`() {
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
    fun `Record timeMs with multiple intervals is stable`() {
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
    fun `computeFlightRecords builds records from intervals`() {
        val laps = listOf(
            Lap("1)", TimeInterval(0L, 12340L)),
            Lap("2)", TimeInterval(12340L, 24680L)),
            Lap("3)", TimeInterval(24680L, 37030L))
        )
        val records = computeFlightRecords(laps, Record.Kind.values().toSet())

        val best1 = records.first { it.kind == Record.Kind.BEST_1 }
        assertEquals(1, best1.count)
        assertEquals(1, best1.intervals.size)
        assertEquals(12340L, best1.intervals.single().durationMs)

        val most = records.first { it.kind == Record.Kind.MOST }
        assertEquals(3, most.count)
        assertEquals(3, most.intervals.size)
        assertEquals(37030L, most.rawTimeMs())
    }

    @Test
    fun `MOST includes completed laps not only successful`() {
        val laps = listOf(
            Lap("HS", TimeInterval(0L, 0L), Lap.Status.HS),
            Lap("1)", TimeInterval(0L, 12000L), Lap.Status.SUCCESS),
            Lap("2)", TimeInterval(12000L, 25000L), Lap.Status.FAIL),
            Lap("3)", TimeInterval(25000L, 37000L), Lap.Status.SUCCESS)
        )
        val records = computeFlightRecords(laps, setOf(Record.Kind.MOST))
        val most = records.single { it.kind == Record.Kind.MOST }
        assertEquals(3, most.count)
        assertEquals(listOf(12000L, 13000L, 12000L), most.intervals.map { it.durationMs })
    }

    @Test
    fun `Record isBetterThan uses rawTimeMs`() {
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
