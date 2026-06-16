package ru.fpvladder.laps.trainer.ui.screens

import ru.fpvladder.laps.trainer.settings.TimerPrecision
import kotlin.math.abs

fun formatTime(elapsedMs: Long, timerPrecision: TimerPrecision, timeLimitSeconds: Int = 0): String {
    val rounded = abs(timerPrecision.roundMs(elapsedMs))
    val totalSeconds = rounded / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = rounded % 1000

    val sign = when {
        elapsedMs >= 0 -> " "
        else -> "-"
    }
    val minutesFormat = when {
        timeLimitSeconds < 600 -> "%01d"
        else -> "%02d"
    }

    return when (timerPrecision.fractionDigits) {
        0 -> String.format("$sign$minutesFormat:%02d", minutes, seconds)
        1 -> String.format("$sign$minutesFormat:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("$sign$minutesFormat:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("$sign$minutesFormat:%02d.%03d", minutes, seconds, ms)
    }
}

fun formatTimeDynamic(elapsedMs: Long, timerPrecision: TimerPrecision): String {
    val rounded = timerPrecision.roundMs(elapsedMs)
    val totalSeconds = rounded / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = rounded % 1000

    return when (timerPrecision.fractionDigits) {
        0 -> {
            if (minutes > 0) String.format("%d:%02d", minutes, seconds)
            else "$seconds"
        }

        1 -> {
            val frac = ms / 100
            if (minutes > 0) String.format("%d:%02d.%01d", minutes, seconds, frac)
            else String.format("%d.%01d", seconds, frac)
        }

        2 -> {
            val frac = ms / 10
            if (minutes > 0) String.format("%d:%02d.%02d", minutes, seconds, frac)
            else String.format("%d.%02d", seconds, frac)
        }

        else -> {
            if (minutes > 0) String.format("%d:%02d.%03d", minutes, seconds, ms)
            else String.format("%d.%03d", seconds, ms)
        }
    }
}

fun formatCountdown(remainingMs: Long, timerPrecision: TimerPrecision): String {
    val rounded = timerPrecision.roundMs(remainingMs)
    val totalSeconds = rounded / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = rounded % 1000

    return when (timerPrecision.fractionDigits) {
        0 -> String.format("-%d:%02d", minutes, seconds)
        1 -> String.format("-%d:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("-%d:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("-%d:%02d.%03d", minutes, seconds, ms)
    }
}
