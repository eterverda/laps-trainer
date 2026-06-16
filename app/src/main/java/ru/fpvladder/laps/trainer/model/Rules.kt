package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.MinMaxIntSerializer

@Serializable
sealed class Rules {
    abstract val lapsLimit: Int
    abstract val timeLimitSeconds: Int
    abstract val holeshotEnabled: Boolean
    abstract val showRecordKinds: Set<Record.Kind>

    @Serializable
    @SerialName("individual")
    data class Individual(
        @Serializable(MinMaxIntSerializer::class)
        override val lapsLimit: Int,
        @Serializable(MinMaxIntSerializer::class)
        override val timeLimitSeconds: Int,
        override val holeshotEnabled: Boolean,
        override val showRecordKinds: Set<Record.Kind>
    ) : Rules()

    @Serializable
    @SerialName("team")
    data class Team(
        @Serializable(MinMaxIntSerializer::class)
        override val lapsLimit: Int,
        @Serializable(MinMaxIntSerializer::class)
        override val timeLimitSeconds: Int,
        override val holeshotEnabled: Boolean,
        val changeMode: ChangeMode,
        val swapMode: SwapMode,
        override val showRecordKinds: Set<Record.Kind>
    ) : Rules() {
        @Serializable
        enum class ChangeMode {
            TIME,
            LAPS
        }

        @Serializable
        enum class SwapMode {
            STRAIGHT,
            SWAPPED;

            fun rotate(): SwapMode = when (this) {
                STRAIGHT -> SWAPPED
                SWAPPED -> STRAIGHT
            }
        }
    }
}
