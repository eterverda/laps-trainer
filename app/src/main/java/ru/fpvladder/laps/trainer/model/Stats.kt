package ru.fpvladder.laps.trainer.model

sealed class Stats {
    abstract val results: Results

    data class Individual(
        override val results: Results = Results()
    ) : Stats()

    data class Team(
        val common: Results = Results(),
        val first: Member = Member(),
        val second: Member = Member()
    ) : Stats() {
        override val results: Results
            get() = common

        data class Member(
            val total: Results = Results(),
            val head: Results = Results(),
            val tail: Results = Results()
        )
    }
}

fun computeTrainingStats(training: Training): Stats = when (training) {
    is Training.Individual -> computeIndividualStats(training.flights)
    is Training.Team -> computeTeamStats(training.flights)
}

private fun computeIndividualStats(flights: List<Flight.Individual>): Stats.Individual {
    if (flights.isEmpty()) return Stats.Individual(Results())

    val records = flights
        .map { it.results.records }
        .reduce { acc, list -> mergeRecordLists(acc, list) }

    val counters = flights
        .map { flight -> flight.results.counters + Counter.SINGLE_FLIGHT }
        .reduce { acc, list -> mergeCounterLists(acc, list) }

    return Stats.Individual(Results(records, counters))
}

private fun computeTeamStats(
    flights: List<Flight.Team>
): Stats.Team {
    if (flights.isEmpty()) {
        return Stats.Team(
            common = Results(),
            first = Stats.Team.Member(Results(), Results(), Results()),
            second = Stats.Team.Member(Results(), Results(), Results())
        )
    }

    val commonRecords = flights
        .map { it.results.records }
        .reduce { acc, list -> mergeRecordLists(acc, list) }
    val commonCounters = flights
        .map { flight -> flight.results.counters + Counter.SINGLE_FLIGHT }
        .reduce { acc, list -> mergeCounterLists(acc, list) }

    data class Accumulator(
        val totalRecords: List<Record> = emptyList(),
        val headRecords: List<Record> = emptyList(),
        val tailRecords: List<Record> = emptyList(),
        val totalCounters: List<Counter> = emptyList()
    )

    fun mergeAccumulator(acc: Accumulator, records: Results, head: Results, tail: Results): Accumulator {
        val (best1, most) = records.records.partition { it.kind == Record.Kind.BEST_1 }
        return Accumulator(
            totalRecords = mergeRecordLists(acc.totalRecords, best1),
            headRecords = mergeRecordLists(acc.headRecords, most),
            tailRecords = acc.tailRecords,
            totalCounters = mergeCounterLists(
                acc.totalCounters,
                records.counters.withFlightIfMissing()
            )
        )
    }

    fun mergeAccumulatorSwapped(acc: Accumulator, records: Results, head: Results, tail: Results): Accumulator {
        val (best1, most) = records.records.partition { it.kind == Record.Kind.BEST_1 }
        return Accumulator(
            totalRecords = mergeRecordLists(acc.totalRecords, best1),
            headRecords = acc.headRecords,
            tailRecords = mergeRecordLists(acc.tailRecords, most),
            totalCounters = mergeCounterLists(
                acc.totalCounters,
                records.counters.withFlightIfMissing()
            )
        )
    }

    val first = flights.fold(Accumulator()) { acc, flight ->
        if (flight.swapMode == Rules.Team.SwapMode.SWAPPED) {
            mergeAccumulatorSwapped(acc, flight.tailResults, flight.headResults, flight.tailResults)
        } else {
            mergeAccumulator(acc, flight.headResults, flight.headResults, flight.tailResults)
        }
    }

    val second = flights.fold(Accumulator()) { acc, flight ->
        if (flight.swapMode == Rules.Team.SwapMode.SWAPPED) {
            mergeAccumulator(acc, flight.headResults, flight.headResults, flight.tailResults)
        } else {
            mergeAccumulatorSwapped(acc, flight.tailResults, flight.headResults, flight.tailResults)
        }
    }

    return Stats.Team(
        common = Results(commonRecords, commonCounters),
        first = Stats.Team.Member(
            total = Results(first.totalRecords, first.totalCounters),
            head = Results(first.headRecords),
            tail = Results(first.tailRecords)
        ),
        second = Stats.Team.Member(
            total = Results(second.totalRecords, second.totalCounters),
            head = Results(second.headRecords),
            tail = Results(second.tailRecords)
        )
    )
}

private fun List<Counter>.withFlightIfMissing(): List<Counter> {
    if (isEmpty()) return this
    val hasFlight = any { it.kind == Counter.Builtin.Kind.FLIGHT }
    return if (hasFlight) this else this + Counter.SINGLE_FLIGHT
}
