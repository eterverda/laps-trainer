package ru.fpvladder.laps.trainer.model

sealed class Flight {
    abstract val laps: List<Lap>
    abstract val stopReason: StopReason
    abstract val result: Results

    data class Individual(
        override val laps: List<Lap> = emptyList(),
        override val stopReason: StopReason = StopReason.MANUAL,
        override val result: Results = Results()
    ) : Flight()

    data class Team(
        override val laps: List<Lap> = emptyList(),
        override val stopReason: StopReason = StopReason.MANUAL,
        val pilotSwapIndex: Int? = null,
        val common: Results = Results(),
        val head: Results = Results(),
        val tail: Results = Results()
    ) : Flight() {
        override val result: Results get() = common
    }
}
