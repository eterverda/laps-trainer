package ru.fpvladder.laps.trainer.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.fpvladder.laps.trainer.model.TimerPrecision

class TimerFormatTest {

    @Test
    fun `roundMs SECONDS`() {
        val p = TimerPrecision.SECONDS
        assertEquals(0L, roundMs(0L, p))
        assertEquals(0L, roundMs(499L, p))
        assertEquals(1000L, roundMs(500L, p))
        assertEquals(1000L, roundMs(999L, p))
        assertEquals(1000L, roundMs(1000L, p))
        assertEquals(1000L, roundMs(1499L, p))
        assertEquals(2000L, roundMs(1500L, p))
        assertEquals(60000L, roundMs(59500L, p))
        assertEquals(60000L, roundMs(59999L, p))
        assertEquals(60000L, roundMs(60000L, p))
    }

    @Test
    fun `roundMs DECISECONDS`() {
        val p = TimerPrecision.DECISECONDS
        assertEquals(0L, roundMs(0L, p))
        assertEquals(0L, roundMs(49L, p))
        assertEquals(100L, roundMs(50L, p))
        assertEquals(100L, roundMs(99L, p))
        assertEquals(100L, roundMs(100L, p))
        assertEquals(100L, roundMs(149L, p))
        assertEquals(200L, roundMs(150L, p))
        assertEquals(900L, roundMs(949L, p))
        assertEquals(1000L, roundMs(950L, p))
        assertEquals(1000L, roundMs(1000L, p))
        assertEquals(60000L, roundMs(59950L, p))
        assertEquals(60000L, roundMs(59999L, p))
    }

    @Test
    fun `roundMs CENTISECONDS`() {
        val p = TimerPrecision.CENTISECONDS
        assertEquals(0L, roundMs(0L, p))
        assertEquals(0L, roundMs(4L, p))
        assertEquals(10L, roundMs(5L, p))
        assertEquals(10L, roundMs(9L, p))
        assertEquals(10L, roundMs(10L, p))
        assertEquals(10L, roundMs(14L, p))
        assertEquals(20L, roundMs(15L, p))
        assertEquals(990L, roundMs(994L, p))
        assertEquals(1000L, roundMs(995L, p))
        assertEquals(1000L, roundMs(1000L, p))
    }

    @Test
    fun `roundMs MILLISECONDS`() {
        val p = TimerPrecision.MILLISECONDS
        assertEquals(0L, roundMs(0L, p))
        assertEquals(1L, roundMs(1L, p))
        assertEquals(999L, roundMs(999L, p))
        assertEquals(1234L, roundMs(1234L, p))
    }

    @Test
    fun `roundMs carry over seconds`() {
        // 0.95s rounded to 1.0s with DECISECONDS
        assertEquals(1000L, roundMs(950L, TimerPrecision.DECISECONDS))
        // 0.995s rounded to 1.0s with CENTISECONDS
        assertEquals(1000L, roundMs(995L, TimerPrecision.CENTISECONDS))
        // 59.95s rounded to 60.0s with DECISECONDS
        assertEquals(60000L, roundMs(59950L, TimerPrecision.DECISECONDS))
    }

    @Test
    fun `formatTime rounds and carries over`() {
        // 59.95s with DECISECONDS should display as 1:00.0 (timeLimit default 0 adds leading space)
        assertEquals(" 1:00.0", formatTime(59950L, TimerPrecision.DECISECONDS))
        // 59.995s with CENTISECONDS should display as 1:00.00
        assertEquals(" 1:00.00", formatTime(59995L, TimerPrecision.CENTISECONDS))
    }

    @Test
    fun `formatTimeDynamic rounds and carries over`() {
        // 59.95s with DECISECONDS should display as 1:00.0
        assertEquals("1:00.0", formatTimeDynamic(59950L, TimerPrecision.DECISECONDS))
        // 59.995s with CENTISECONDS should display as 1:00.00
        assertEquals("1:00.00", formatTimeDynamic(59995L, TimerPrecision.CENTISECONDS))
    }

    @Test
    fun `formatTime with timeLimit padding`() {
        // 83450ms rounded to 83500ms = 83.5s = 1:23.5
        // timeLimit < 600 (10 min) => single space before single-digit minute
        assertEquals(" 1:23.5", formatTime(83450L, TimerPrecision.DECISECONDS, timeLimitSeconds = 300))
        // timeLimit >= 600 => zero-padded
        assertEquals("01:23.5", formatTime(83450L, TimerPrecision.DECISECONDS, timeLimitSeconds = 600))
    }

    @Test
    fun `sum of rounded values matches displayed sum`() {
        // Three laps with raw times
        val times = listOf(12340L, 12350L, 12360L) // raw times
        val p = TimerPrecision.CENTISECONDS // tick = 10ms
        val rounded = times.map { roundMs(it, p) }
        // 12340 -> 12340 (12.34s), 12350 -> 12350 (12.35s), 12360 -> 12360 (12.36s)
        assertEquals(listOf(12340L, 12350L, 12360L), rounded)
        val sum = rounded.sum()
        assertEquals("12.34", formatTimeDynamic(rounded[0], p))
        assertEquals("12.35", formatTimeDynamic(rounded[1], p))
        assertEquals("12.36", formatTimeDynamic(rounded[2], p))
        assertEquals("37.05", formatTimeDynamic(sum, p))
    }
}
