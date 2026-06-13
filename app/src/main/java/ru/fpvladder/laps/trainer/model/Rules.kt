package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.MinMaxIntSerializer
import ru.fpvladder.laps.trainer.settings.TEAM_HOLESHOT_ENABLED
import java.util.EnumSet

@Serializable
sealed class Rules {
    abstract val lapsLimit: Int
    abstract val timeLimitSeconds: Int
    abstract val holeshotEnabled: Boolean
    abstract val enabledRecordKinds: Set<Record.Kind>

    @Serializable
    @SerialName("individual")
    data class Individual(
        @Serializable(MinMaxIntSerializer::class)
        override val lapsLimit: Int = Int.MAX_VALUE,
        @Serializable(MinMaxIntSerializer::class)
        override val timeLimitSeconds: Int = 180,
        override val holeshotEnabled: Boolean = true,
        override val enabledRecordKinds: Set<Record.Kind> = EnumSet.of(
            Record.Kind.BEST_1,
            Record.Kind.BEST_3
        ).apply { if (timeLimitSeconds != Int.MAX_VALUE) add(Record.Kind.MOST) }
    ) : Rules()

    @Serializable
    @SerialName("team")
    data class Team(
        @Serializable(MinMaxIntSerializer::class)
        override val lapsLimit: Int = 50,
        @Serializable(MinMaxIntSerializer::class)
        override val timeLimitSeconds: Int = 1800,
        override val holeshotEnabled: Boolean = TEAM_HOLESHOT_ENABLED,
        val swapMode: SwapMode = SwapMode.LAPS,
        val pilotOrderSwapped: Boolean = false,
        override val enabledRecordKinds: Set<Record.Kind> = EnumSet.of(
            Record.Kind.BEST_1, Record.Kind.MOST,
        )
    ) : Rules() {
        @Serializable
        enum class SwapMode {
            TIME,
            LAPS
        }
    }
}
