package ru.fpvladder.laps.trainer.model

data class LapEntry(
    val lapLabel: String,
    val timeMs: Long,
    val icons: List<LapIcon> = listOf(LapIcon.LAP),
    val status: LapStatus = LapStatus.RUNNING
)
