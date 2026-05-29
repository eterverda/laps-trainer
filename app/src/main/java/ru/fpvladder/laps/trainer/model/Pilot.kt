package ru.fpvladder.laps.trainer.model

sealed class Pilot {
    abstract val name: String
    abstract val channel: Channel

    data class Individual(
        override val name: String = "",
        override val channel: Channel = Channel()
    ) : Pilot()

    data class Team(
        val name1: String = "",
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
    }
}
