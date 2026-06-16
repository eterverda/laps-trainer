package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.Rules.Team.SwapMode

@Serializable
sealed class Flight {
    abstract val laps: List<Lap>
    abstract val stopReason: StopReason
    abstract val results: Results

    @Serializable
    @SerialName("individual")
    data class Individual(
        override val laps: List<Lap>,
        override val stopReason: StopReason,
    ) : Flight() {
        override val results: Results by lazy { computeFlightResults(laps) }
    }

    @Serializable
    @SerialName("team")
    data class Team(
        val headLaps: List<Lap>,
        val tailLaps: List<Lap>,
        override val stopReason: StopReason,
        val swapMode: SwapMode,
    ) : Flight() {
        override val laps: List<Lap> by lazy { headLaps + tailLaps }
        override val results: Results by lazy { computeTeamCommonResults(laps) }
        val headResults: Results by lazy { computeFlightResults(headLaps) }
        val tailResults: Results by lazy { computeFlightResults(tailLaps) }
    }
}
