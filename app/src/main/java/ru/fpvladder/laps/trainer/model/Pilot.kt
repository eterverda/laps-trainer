@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Pilot {
    abstract val name: String
    abstract val channel: Channel
    abstract fun isAnonymous(): Boolean

    @Serializable
    @SerialName("individual")
    data class Individual(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        override val name: String = "",
        override val channel: Channel = Channel()
    ) : Pilot() {
        override fun isAnonymous(): Boolean = name.isBlank()
    }

    @Serializable
    @SerialName("team")
    data class Team(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val name1: String = "",
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val name2: String = "",
        override val channel: Channel = Channel()
    ) : Pilot() {
        override val name: String
            get() = when {
                name1.isNotBlank() && name2.isNotBlank() -> "$name1 / $name2"
                name1.isNotBlank() -> name1
                name2.isNotBlank() -> name2
                else -> ""
            }

        override fun isAnonymous(): Boolean = name1.isBlank() && name2.isBlank()
    }
}
