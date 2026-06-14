package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.LocalDateSerializer
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

@Serializable
sealed class Training {
    abstract val id: String
    abstract val date: LocalDate
    abstract val pilot: Pilot
    abstract val rules: Rules
    abstract val flights: List<Flight>

    val stats: Stats by lazy { computeTrainingStats(this) }

    fun isEmpty(): Boolean = flights.isEmpty()
    fun isDefault(): Boolean = isEmpty() && pilot.isAnonymous()

    companion object {
        val DEFAULT: Training = Training.Individual()
    }

    @Serializable
    @SerialName("individual")
    class Individual private constructor(
        override val id: String,
        @Serializable(with = LocalDateSerializer::class)
        override val date: LocalDate,
        override val pilot: Pilot.Individual,
        override val rules: Rules.Individual,
        override val flights: List<Flight.Individual>,
    ) : Training() {

        constructor(
            pilot: Pilot.Individual = Pilot.Individual(),
            date: LocalDate = LocalDate.now(),
        ) : this(
            id = UUID.randomUUID().toString(),
            date = date,
            pilot = pilot,
            rules = Rules.Individual(),
            flights = emptyList(),
        )

        fun copy(
            pilot: Pilot.Individual = this.pilot,
            rules: Rules.Individual = this.rules,
            flights: List<Flight.Individual> = this.flights,
            date: LocalDate = this.date,
        ) = Individual(
            id = id,
            date = date,
            pilot = pilot,
            rules = rules,
            flights = flights
        )
    }

    @Serializable
    @SerialName("team")
    class Team private constructor(
        override val id: String,
        @Serializable(with = LocalDateSerializer::class)
        override val date: LocalDate,
        override val pilot: Pilot.Team = Pilot.Team(),
        override val rules: Rules.Team = Rules.Team(),
        override val flights: List<Flight.Team> = emptyList(),
    ) : Training() {

        constructor(
            pilot: Pilot.Team = Pilot.Team(),
            date: LocalDate = LocalDate.now(),
        ) : this(
            id = UUID.randomUUID().toString(),
            date = date,
            pilot = pilot,
            rules = Rules.Team(),
            flights = emptyList(),
        )

        fun copy(
            pilot: Pilot.Team = this.pilot,
            rules: Rules.Team = this.rules,
            flights: List<Flight.Team> = this.flights,
            date: LocalDate = this.date,
        ) = Team(
            id = id,
            date = date,
            pilot = pilot,
            rules = rules,
            flights = flights
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Training) return false
        return id == other.id &&
                date == other.date &&
                pilot == other.pilot &&
                rules == other.rules &&
                flights == other.flights
    }

    override fun hashCode(): Int = Objects.hash(id, date, pilot, rules, flights)

    override fun toString(): String =
        "${javaClass.simpleName}(id=$id, date=$date, pilot=$pilot, rules=$rules, flights.size=${flights.size})"
}
