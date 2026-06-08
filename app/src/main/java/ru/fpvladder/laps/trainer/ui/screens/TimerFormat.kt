package ru.fpvladder.laps.trainer.ui.screens

import ru.fpvladder.laps.trainer.model.TimerPrecision

fun formatTime(elapsedMs: Long, timerPrecision: TimerPrecision, timeLimitSeconds: Int = 0): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = elapsedMs % 1000

    val minutesFormat = if (timeLimitSeconds < 600) " %d" else "%02d"

    return when (timerPrecision.fractionDigits) {
        0 -> String.format("$minutesFormat:%02d", minutes, seconds)
        1 -> String.format("$minutesFormat:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("$minutesFormat:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("$minutesFormat:%02d.%03d", minutes, seconds, ms)
    }
}

fun formatCountdown(remainingMs: Long, timerPrecision: TimerPrecision): String {
    val totalSeconds = remainingMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = remainingMs % 1000

    return when (timerPrecision.fractionDigits) {
        0 -> String.format("-%d:%02d", minutes, seconds)
        1 -> String.format("-%d:%02d.%01d", minutes, seconds, ms / 100)
        2 -> String.format("-%d:%02d.%02d", minutes, seconds, ms / 10)
        else -> String.format("-%d:%02d.%03d", minutes, seconds, ms)
    }
}
