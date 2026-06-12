package ru.fpvladder.laps.trainer.model

sealed class Stats {
    abstract val records: List<Record>
    abstract val counters: List<Counter>

    data class Individual(
        override val records: List<Record> = emptyList(),
        override val counters: List<Counter> = emptyList(),
    ) : Stats()

    data class Team(
        override val records: List<Record> = emptyList(),
        override val counters: List<Counter> = emptyList(),
        val first: Item = Item(),
        val second: Item = Item(),
    ) : Stats() {
        data class Item(
            val records: List<Record> = emptyList(),
            val recordsBeingHead: List<Record> = emptyList(),
            val recordsBeingTail: List<Record> = emptyList(),
            val counters: List<Counter> = emptyList(),
        )
    }
}

data class Record(
    val count: Int,
    val kind: Kind,
    val intervals: List<TimeInterval> = emptyList()
) {
    enum class Kind { BEST_1, BEST_2, BEST_3, MOST }

    fun rawTimeMs(): Long = intervals.sumOf { it.durationMs }

    fun timeMs(precision: TimerPrecision): Long {
        return intervals.sumOf { precision.roundMs(it.endMs) - precision.roundMs(it.startMs) }
    }

    fun isBetterThan(other: Record): Boolean {
        return count > other.count || (count == other.count && rawTimeMs() < other.rawTimeMs())
    }
}

sealed class Counter {
    abstract val count: Int
    abstract val kind: Kind?
    abstract fun withAddedCount(other: Counter): Counter

    data class Builtin(
        override val count: Int = 1,
        override val kind: Kind
    ) : Counter() {
        override fun withAddedCount(other: Counter): Counter {
            require(other is Builtin && other.kind == kind)
            return copy(count = count + other.count)
        }
    }

    data class Custom(
        override val count: Int = 1,
        val text: String
    ) : Counter() {
        override val kind: Kind?
            get() = null

        override fun withAddedCount(other: Counter): Counter {
            require(other is Custom && other.text == text)
            return copy(count = count + other.count)
        }
    }

    enum class Kind { FLIGHT, LAP }

    companion object {
        val SINGLE_FLIGHT = Builtin(count = 1, kind = Kind.FLIGHT)
    }

    object SortComparator : Comparator<Counter> {
        override fun compare(a: Counter, b: Counter): Int {
            val aKind = a.kind
            val bKind = b.kind
            return when {
                aKind != null && bKind != null -> aKind.ordinal.compareTo(bKind.ordinal)
                aKind == null && bKind == null ->
                    (a as Custom).text.compareTo((b as Custom).text)

                aKind != null -> -1
                else -> 1
            }
        }
    }
}

fun Counter.same(other: Counter): Boolean = when (val k = kind) {
    Counter.Kind.FLIGHT, Counter.Kind.LAP -> other.kind == k
    null -> (this as Counter.Custom).text == (other as Counter.Custom).text
}

private fun MutableList<Counter>.mergeIn(counter: Counter) {
    val index = indexOfFirst { it.same(counter) }
    if (index >= 0) {
        this[index] = this[index].withAddedCount(counter)
    } else {
        add(counter)
    }
}

fun computeFlightRecords(
    laps: List<Lap>,
    enabledKinds: Set<Record.Kind>
): List<Record> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val completedLaps = laps.filter { it.status != Lap.Status.HS }

    if (validLaps.isEmpty() && completedLaps.isEmpty()) {
        return emptyList()
    }

    val records = mutableListOf<Record>()

    if (Record.Kind.BEST_1 in enabledKinds) {
        val best1 = validLaps.minByOrNull { it.timeMs }!!
        records.add(
            Record(
                count = 1,
                kind = Record.Kind.BEST_1,
                intervals = listOf(best1.interval)
            )
        )
    }

    if (Record.Kind.BEST_2 in enabledKinds) {
        val size = minOf(2, validLaps.size)
        val windowLaps = minWindowLaps(validLaps, size)
        records.add(
            Record(
                count = size,
                kind = Record.Kind.BEST_2,
                intervals = windowLaps.map { it.interval }
            )
        )
    }

    if (Record.Kind.BEST_3 in enabledKinds) {
        val size = minOf(3, validLaps.size)
        val windowLaps = minWindowLaps(validLaps, size)
        records.add(
            Record(
                count = size,
                kind = Record.Kind.BEST_3,
                intervals = windowLaps.map { it.interval }
            )
        )
    }

    if (Record.Kind.MOST in enabledKinds && completedLaps.isNotEmpty()) {
        records.add(
            Record(
                count = completedLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
    }

    return records.toList()
}

fun computeFlightCounters(laps: List<Lap>): List<Counter> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    return listOf(
        Counter.Builtin(
            count = validLaps.size,
            kind = Counter.Kind.LAP
        )
    )
}

data class TeamFlightRecords(
    val common: List<Record>,
    val head: List<Record>,
    val tail: List<Record>
)

enum class TeamCounterScope { COMMON, HEAD, TAIL }


fun computeTeamFlightCounters(
    laps: List<Lap>,
    scope: TeamCounterScope,
    pilotSwapIndex: Int?
): List<Counter> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val lapCount = when (scope) {
        TeamCounterScope.COMMON -> validLaps.size
        TeamCounterScope.HEAD -> if (pilotSwapIndex != null) {
            laps.take(pilotSwapIndex + 1).count { it.status == Lap.Status.SUCCESS }
        } else {
            validLaps.size
        }

        TeamCounterScope.TAIL -> if (pilotSwapIndex != null) {
            laps.drop(pilotSwapIndex + 1).count { it.status == Lap.Status.SUCCESS }
        } else {
            0
        }
    }
    if (scope == TeamCounterScope.TAIL && lapCount == 0) {
        return emptyList()
    }
    return listOf(
        Counter.Builtin(count = lapCount, kind = Counter.Kind.LAP)
    )
}

fun Stats.Team.mergeTeamFlight(
    common: Flight.Team.Item,
    head: Flight.Team.Item,
    tail: Flight.Team.Item,
    pilotOrderSwapped: Boolean
): Stats.Team {
    val mergedCommonRecords = mergeRecordLists(records, common.records)
    val mergedCommonCounters = mergeCounterLists(
        counters,
        common.counters + Counter.SINGLE_FLIGHT
    )

    val name1Best1: List<Record>
    val name1Most: List<Record>
    val name1Counters: List<Counter>
    val name1MostBeingHead: List<Record>
    val name1MostBeingTail: List<Record>

    val name2Best1: List<Record>
    val name2Most: List<Record>
    val name2Counters: List<Counter>
    val name2MostBeingHead: List<Record>
    val name2MostBeingTail: List<Record>

    if (pilotOrderSwapped) {
        // name2 flew head, name1 flew tail.
        val (headBest1, headMost) = head.records.partition { it.kind == Record.Kind.BEST_1 }
        val (tailBest1, tailMost) = tail.records.partition { it.kind == Record.Kind.BEST_1 }
        name1Best1 = tailBest1
        name1Most = tailMost
        name1Counters = tail.counters
        name1MostBeingHead = emptyList()
        name1MostBeingTail = name1Most

        name2Best1 = headBest1
        name2Most = headMost
        name2Counters = head.counters
        name2MostBeingHead = name2Most
        name2MostBeingTail = emptyList()
    } else {
        // name1 flew head, name2 flew tail.
        val (headBest1, headMost) = head.records.partition { it.kind == Record.Kind.BEST_1 }
        val (tailBest1, tailMost) = tail.records.partition { it.kind == Record.Kind.BEST_1 }
        name1Best1 = headBest1
        name1Most = headMost
        name1Counters = head.counters
        name1MostBeingHead = name1Most
        name1MostBeingTail = emptyList()

        name2Best1 = tailBest1
        name2Most = tailMost
        name2Counters = tail.counters
        name2MostBeingHead = emptyList()
        name2MostBeingTail = name2Most
    }

    fun List<Counter>.withFlightIfMissing(): List<Counter> {
        if (isEmpty()) return this
        val hasFlight = any { it.kind == Counter.Kind.FLIGHT }
        return if (hasFlight) this else this + Counter.SINGLE_FLIGHT
    }

    return copy(
        records = mergedCommonRecords,
        counters = mergedCommonCounters,
        first = this.first.copy(
            records = mergeRecordLists(this.first.records, name1Best1),
            recordsBeingHead = mergeRecordLists(
                this.first.recordsBeingHead,
                name1MostBeingHead
            ),
            recordsBeingTail = mergeRecordLists(
                this.first.recordsBeingTail,
                name1MostBeingTail
            ),
            counters = mergeCounterLists(this.first.counters, name1Counters.withFlightIfMissing())
        ),
        second = this.second.copy(
            records = mergeRecordLists(this.second.records, name2Best1),
            recordsBeingHead = mergeRecordLists(
                this.second.recordsBeingHead,
                name2MostBeingHead
            ),
            recordsBeingTail = mergeRecordLists(
                this.second.recordsBeingTail,
                name2MostBeingTail
            ),
            counters = mergeCounterLists(this.second.counters, name2Counters.withFlightIfMissing())
        )
    )
}

private fun mergeRecordLists(
    existing: List<Record>,
    new: List<Record>
): List<Record> {
    if (new.isEmpty()) return existing
    val bestMap = existing.associateBy { it.kind }.toMutableMap()
    new.forEach { record ->
        val current = bestMap[record.kind]
        if (current == null || record.isBetterThan(current)) {
            bestMap[record.kind] = record
        }
    }
    return bestMap.values.toList().sortedBy { it.kind.ordinal }
}

private fun mergeCounterLists(existing: List<Counter>, new: List<Counter>): List<Counter> {
    return buildList {
        addAll(existing)
        for (counter in new) {
            mergeIn(counter)
        }
        sortWith(Counter.SortComparator)
    }
}

private fun minWindowLaps(
    laps: List<Lap>,
    size: Int
): List<Lap> {
    if (size >= laps.size) {
        return laps
    }
    var minSum = Long.MAX_VALUE
    var minIndex = 0
    for (i in 0..laps.size - size) {
        var sum = 0L
        for (j in 0 until size) {
            sum += laps[i + j].timeMs
        }
        if (sum < minSum) {
            minSum = sum
            minIndex = i
        }
    }
    return laps.subList(minIndex, minIndex + size)
}


fun Stats.Individual.mergeFlightRecords(
    records: List<Record>,
    flightCounters: List<Counter>
): Stats.Individual {
    val newRecords = mergeRecordLists(emptyList(), records)

    val mergedCounters = buildList {
        addAll(counters)
        for (it in flightCounters) {
            mergeIn(it)
        }
        mergeIn(Counter.SINGLE_FLIGHT)
        sortWith(Counter.SortComparator)
    }

    return copy(
        records = newRecords,
        counters = mergedCounters
    )
}

fun computeTeamFlightRecords(
    laps: List<Lap>,
    enabledKinds: Set<Record.Kind>,
    pilotSwapIndex: Int?
): TeamFlightRecords {
    if (enabledKinds.isEmpty()) {
        return TeamFlightRecords(emptyList(), emptyList(), emptyList())
    }

    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val completedLaps = laps.filter { it.status != Lap.Status.HS }

    val headPilotRaw = if (pilotSwapIndex != null) laps.take(pilotSwapIndex + 1) else laps
    val tailPilotRaw = if (pilotSwapIndex != null) laps.drop(pilotSwapIndex + 1) else emptyList()

    val headPilotSuccessLaps = headPilotRaw.filter { it.status == Lap.Status.SUCCESS }
    val tailPilotSuccessLaps = tailPilotRaw.filter { it.status == Lap.Status.SUCCESS }

    val headPilotCompletedLaps = headPilotRaw.filter { it.status != Lap.Status.HS }
    val tailPilotCompletedLaps = tailPilotRaw.filter { it.status != Lap.Status.HS }

    val common = mutableListOf<Record>()
    val head = mutableListOf<Record>()
    val tail = mutableListOf<Record>()

    if (Record.Kind.MOST in enabledKinds && completedLaps.isNotEmpty()) {
        common.add(
            Record(
                count = validLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
        if (headPilotCompletedLaps.isNotEmpty()) {
            head.add(
                Record(
                    count = headPilotSuccessLaps.size,
                    kind = Record.Kind.MOST,
                    intervals = headPilotCompletedLaps.map { it.interval }
                )
            )
        }
        if (tailPilotCompletedLaps.isNotEmpty()) {
            tail.add(
                Record(
                    count = tailPilotSuccessLaps.size,
                    kind = Record.Kind.MOST,
                    intervals = tailPilotCompletedLaps.map { it.interval }
                )
            )
        }
    }

    if (Record.Kind.BEST_1 in enabledKinds && headPilotSuccessLaps.isNotEmpty()) {
        headPilotSuccessLaps.minByOrNull { it.timeMs }?.let {
            head.add(
                Record(
                    count = 1,
                    kind = Record.Kind.BEST_1,
                    intervals = listOf(it.interval)
                )
            )
        }
    }

    if (Record.Kind.BEST_1 in enabledKinds && tailPilotSuccessLaps.isNotEmpty()) {
        tailPilotSuccessLaps.minByOrNull { it.timeMs }?.let {
            tail.add(
                Record(
                    count = 1,
                    kind = Record.Kind.BEST_1,
                    intervals = listOf(it.interval)
                )
            )
        }
    }

    if (Record.Kind.BEST_2 in enabledKinds) {
        val size1 = minOf(2, headPilotSuccessLaps.size)
        if (size1 > 0) {
            val windowLaps = minWindowLaps(headPilotSuccessLaps, size1)
            head.add(
                Record(
                    count = size1,
                    kind = Record.Kind.BEST_2,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
        val size2 = minOf(2, tailPilotSuccessLaps.size)
        if (size2 > 0) {
            val windowLaps = minWindowLaps(tailPilotSuccessLaps, size2)
            tail.add(
                Record(
                    count = size2,
                    kind = Record.Kind.BEST_2,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
    }

    if (Record.Kind.BEST_3 in enabledKinds) {
        val size1 = minOf(3, headPilotSuccessLaps.size)
        if (size1 > 0) {
            val windowLaps = minWindowLaps(headPilotSuccessLaps, size1)
            head.add(
                Record(
                    count = size1,
                    kind = Record.Kind.BEST_3,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
        val size2 = minOf(3, tailPilotSuccessLaps.size)
        if (size2 > 0) {
            val windowLaps = minWindowLaps(tailPilotSuccessLaps, size2)
            tail.add(
                Record(
                    count = size2,
                    kind = Record.Kind.BEST_3,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
    }

    return TeamFlightRecords(common, head, tail)
}
