package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Stats {
    abstract val result: Results

    @Serializable
    @SerialName("individual")
    data class Individual(
        override val result: Results = Results()
    ) : Stats()

    @Serializable
    @SerialName("team")
    data class Team(
        val common: Results = Results(),
        val first: Member = Member(),
        val second: Member = Member()
    ) : Stats() {
        override val result: Results
            get() = common

        @Serializable
        @SerialName("member")
        data class Member(
            val total: Results = Results(),
            val head: Results = Results(),
            val tail: Results = Results()
        )
    }
}

fun Stats.Team.mergeTeamFlight(
    common: Results,
    head: Results,
    tail: Results,
    pilotOrderSwapped: Boolean
): Stats.Team {
    val mergedCommonRecords = mergeRecordLists(this.common.records, common.records)
    val mergedCommonCounters = mergeCounterLists(
        this.common.counters,
        common.counters + Counter.SINGLE_FLIGHT
    )

    val (headBest1, headMost) = head.records.partition { it.kind == Record.Kind.BEST_1 }
    val (tailBest1, tailMost) = tail.records.partition { it.kind == Record.Kind.BEST_1 }

    fun List<Counter>.withFlightIfMissing(): List<Counter> {
        if (isEmpty()) return this
        val hasFlight = any { it.kind == Counter.Builtin.Kind.FLIGHT }
        return if (hasFlight) this else this + Counter.SINGLE_FLIGHT
    }

    fun mergeMember(
        existing: Stats.Team.Member,
        commonRecords: List<Record>,
        headRecords: List<Record>,
        tailRecords: List<Record>,
        counters: List<Counter>
    ) = existing.copy(
        total = Results(
            records = mergeRecordLists(existing.total.records, commonRecords),
            counters = mergeCounterLists(
                existing.total.counters,
                counters.withFlightIfMissing()
            )
        ),
        head = Results(
            records = mergeRecordLists(existing.head.records, headRecords)
        ),
        tail = Results(
            records = mergeRecordLists(existing.tail.records, tailRecords)
        )
    )

    val (firstMember, secondMember) = if (pilotOrderSwapped) {
        // name2 flew head, name1 flew tail.
        mergeMember(this.first, tailBest1, emptyList(), tailMost, tail.counters) to
        mergeMember(this.second, headBest1, headMost, emptyList(), head.counters)
    } else {
        // name1 flew head, name2 flew tail.
        mergeMember(this.first, headBest1, headMost, emptyList(), head.counters) to
        mergeMember(this.second, tailBest1, emptyList(), tailMost, tail.counters)
    }

    return copy(
        common = Results(
            records = mergedCommonRecords,
            counters = mergedCommonCounters
        ),
        first = firstMember,
        second = secondMember
    )
}

fun Stats.Individual.mergeFlightRecords(
    flightResult: Results
): Stats.Individual {
    val newRecords = mergeRecordLists(emptyList(), flightResult.records)

    val mergedCounters = buildList {
        addAll(result.counters)
        for (it in flightResult.counters) {
            mergeIn(it)
        }
        mergeIn(Counter.SINGLE_FLIGHT)
        sortWith(Counter.SortComparator)
    }

    return copy(
        result = Results(
            records = newRecords,
            counters = mergedCounters
        )
    )
}
