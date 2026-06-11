package ru.fpvladder.laps.trainer.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.fpvladder.laps.trainer.model.TimerPrecision

class TimerFormatTest {

    @Test
    fun `roundMs SECONDS`() {
        val p = TimerPrecision.SECONDS
        assertEquals(0L, p.roundMs(0L))
        assertEquals(0L, p.roundMs(499L))
        assertEquals(1000L, p.roundMs(500L))
        assertEquals(1000L, p.roundMs(999L))
        assertEquals(1000L, p.roundMs(1000L))
        assertEquals(1000L, p.roundMs(1499L))
        assertEquals(2000L, p.roundMs(1500L))
        assertEquals(60000L, p.roundMs(59500L))
        assertEquals(60000L, p.roundMs(59999L))
        assertEquals(60000L, p.roundMs(60000L))
    }

    @Test
    fun `roundMs DECISECONDS`() {
        val p = TimerPrecision.DECISECONDS
        assertEquals(0L, p.roundMs(0L))
        assertEquals(0L, p.roundMs(49L))
        assertEquals(100L, p.roundMs(50L))
        assertEquals(100L, p.roundMs(99L))
        assertEquals(100L, p.roundMs(100L))
        assertEquals(100L, p.roundMs(149L))
        assertEquals(200L, p.roundMs(150L))
        assertEquals(900L, p.roundMs(949L))
        assertEquals(1000L, p.roundMs(950L))
        assertEquals(1000L, p.roundMs(1000L))
        assertEquals(60000L, p.roundMs(59950L))
        assertEquals(60000L, p.roundMs(59999L))
    }

    @Test
    fun `roundMs CENTISECONDS`() {
        val p = TimerPrecision.CENTISECONDS
        assertEquals(0L, p.roundMs(0L))
        assertEquals(0L, p.roundMs(4L))
        assertEquals(10L, p.roundMs(5L))
        assertEquals(10L, p.roundMs(9L))
        assertEquals(10L, p.roundMs(10L))
        assertEquals(10L, p.roundMs(14L))
        assertEquals(20L, p.roundMs(15L))
        assertEquals(990L, p.roundMs(994L))
        assertEquals(1000L, p.roundMs(995L))
        assertEquals(1000L, p.roundMs(1000L))
    }

    @Test
    fun `roundMs MILLISECONDS`() {
        val p = TimerPrecision.MILLISECONDS
        assertEquals(0L, p.roundMs(0L))
        assertEquals(1L, p.roundMs(1L))
        assertEquals(999L, p.roundMs(999L))
        assertEquals(1234L, p.roundMs(1234L))
    }

    @Test
    fun `roundMs carry over seconds`() {
        // 0.95s rounded to 1.0s with DECISECONDS
        assertEquals(1000L, TimerPrecision.DECISECONDS.roundMs(950L))
        // 0.995s rounded to 1.0s with CENTISECONDS
        assertEquals(1000L, TimerPrecision.CENTISECONDS.roundMs(995L))
        // 59.95s rounded to 60.0s with DECISECONDS
        assertEquals(60000L, TimerPrecision.DECISECONDS.roundMs(59950L))
    }

    @Test
    fun `formatTime rounds and carries over`() {
        // 59.95s with DECISECONDS should display as 1:00.0 (timeLimit default 0 adds leading space)
        assertEquals("1:00.0", formatTime(59950L, TimerPrecision.DECISECONDS))
        // 59.995s with CENTISECONDS should display as 1:00.00
        assertEquals("1:00.00", formatTime(59995L, TimerPrecision.CENTISECONDS))
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
        assertEquals("1:23.5", formatTime(83450L, TimerPrecision.DECISECONDS, timeLimitSeconds = 300))
        // timeLimit >= 600 => zero-padded
        assertEquals("01:23.5", formatTime(83450L, TimerPrecision.DECISECONDS, timeLimitSeconds = 600))
    }

    @Test
    fun `sum of rounded values matches displayed sum`() {
        // Three laps with raw times
        val times = listOf(12340L, 12350L, 12360L) // raw times
        val p = TimerPrecision.CENTISECONDS // tick = 10ms
        val rounded = times.map { p.roundMs(it) }
        // 12340 -> 12340 (12.34s), 12350 -> 12350 (12.35s), 12360 -> 12360 (12.36s)
        assertEquals(listOf(12340L, 12350L, 12360L), rounded)
        val sum = rounded.sum()
        assertEquals("12.34", formatTimeDynamic(rounded[0], p))
        assertEquals("12.35", formatTimeDynamic(rounded[1], p))
        assertEquals("12.36", formatTimeDynamic(rounded[2], p))
        assertEquals("37.05", formatTimeDynamic(sum, p))
    }
}
