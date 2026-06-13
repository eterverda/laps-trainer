package ru.fpvladder.laps.trainer.ui.helpers

import ru.fpvladder.laps.trainer.model.Lap

val Lap.runningLabel: String
    get() = when (number) {
        0 -> "HS"
        else -> "$number)"
    }

val Lap.label: String
    get() = if (success) runningLabel else ""
