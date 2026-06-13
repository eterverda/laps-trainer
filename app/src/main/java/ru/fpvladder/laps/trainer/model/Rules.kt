package ru.fpvladder.laps.trainer.model

import ru.fpvladder.laps.trainer.settings.TEAM_HOLESHOT_ENABLED
import java.util.EnumSet

sealed class Rules {
    abstract val maxLaps: Int
    abstract val timeLimitSeconds: Int
    abstract val holeshotEnabled: Boolean
    abstract val enabledRecordKinds: Set<Record.Kind>

    data class Individual(
        override val maxLaps: Int = Int.MAX_VALUE,
        override val timeLimitSeconds: Int = 180,
        override val holeshotEnabled: Boolean = true,
        override val enabledRecordKinds: Set<Record.Kind> = EnumSet.of(
            Record.Kind.BEST_1,
            Record.Kind.BEST_3
        ).apply { if (timeLimitSeconds != Int.MAX_VALUE) add(Record.Kind.MOST) }
    ) : Rules()

    data class Team(
        override val maxLaps: Int = 50,
        override val timeLimitSeconds: Int = 1800,
        override val holeshotEnabled: Boolean = TEAM_HOLESHOT_ENABLED,
        val swapMode: SwapMode = SwapMode.LAPS,
        val pilotOrderSwapped: Boolean = false,
        override val enabledRecordKinds: Set<Record.Kind> = EnumSet.of(
            Record.Kind.BEST_1, Record.Kind.MOST,
        )
    ) : Rules() {
        enum class SwapMode {
            TIME,
            LAPS
        }
    }
}
