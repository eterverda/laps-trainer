package ru.fpvladder.laps.trainer.ui.helpers

import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.settings.TimerPrecision

fun Record.timeMs(precision: TimerPrecision): Long {
    return intervals.sumOf { precision.roundMs(it.endMs) - precision.roundMs(it.startMs) }
}
