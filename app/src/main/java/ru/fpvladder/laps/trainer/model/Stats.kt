package ru.fpvladder.laps.trainer.model

sealed class Stats {
    data class Individual(
        val bestLaps: List<BestLap> = emptyList(),
        val counters: List<Counter> = emptyList(),
    ) : Stats()

    class Team : Stats()
}

data class BestLap(
    val count: Int,
    val timeMs: Long,
    val kind: Kind
) : Comparable<BestLap> {
    enum class Kind { BEST_1, BEST_2, BEST_3, MOST }

    override fun compareTo(other: BestLap): Int {
        return compareValuesBy(this, other, { -it.count }, { it.timeMs })
    }

    fun isBetterThan(other: BestLap): Boolean {
        return when (kind) {
            Kind.MOST -> count > other.count || (count == other.count && timeMs < other.timeMs)
            else -> timeMs < other.timeMs
        }
    }
}

sealed class Counter {
    abstract val count: Int

    data class Builtin(
        override val count: Int,
        val kind: Kind
    ) : Counter() {
        enum class Kind { FLIGHT, LAP }
    }

    data class Custom(
        override val count: Int,
        val text: String
    ) : Counter()

    object SortComparator : Comparator<Counter> {
        override fun compare(a: Counter, b: Counter): Int {
            return when {
                a is Builtin && b is Builtin -> a.kind.ordinal.compareTo(b.kind.ordinal)
                a is Custom && b is Custom -> a.text.compareTo(b.text)
                a is Builtin && b is Custom -> -1
                else -> 1
            }
        }
    }
}

fun Counter.same(other: Counter): Boolean = when (this) {
    is Counter.Builtin -> other is Counter.Builtin && this.kind == other.kind
    is Counter.Custom -> other is Counter.Custom && this.text == other.text
}

private fun MutableList<Counter>.mergeIn(counter: Counter) {
    val index = indexOfFirst { it.same(counter) }
    if (index >= 0) {
        val existing = this[index]
        this[index] = when {
            existing is Counter.Builtin && counter is Counter.Builtin ->
                existing.copy(count = existing.count + counter.count)

            existing is Counter.Custom && counter is Counter.Custom ->
                existing.copy(count = existing.count + counter.count)

            else -> counter
        }
    } else {
        add(counter)
    }
}

fun computeFlightRecords(
    laps: List<Lap>,
    timerPrecision: TimerPrecision,
    enabledKinds: Set<BestLap.Kind>
): List<BestLap> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }

    if (validLaps.isEmpty()) {
        return emptyList()
    }

    val records = mutableListOf<BestLap>()

    if (BestLap.Kind.BEST_1 in enabledKinds) {
        val best1 = validLaps.minByOrNull { timerPrecision.roundMs(it.timeMs) }!!
        records.add(
            BestLap(
                count = 1,
                timeMs = timerPrecision.roundMs(best1.timeMs),
                kind = BestLap.Kind.BEST_1
            )
        )
    }

    if (BestLap.Kind.BEST_2 in enabledKinds) {
        val size = minOf(2, validLaps.size)
        val minSum = minWindowSum(validLaps, size, timerPrecision)
        records.add(
            BestLap(
                count = size,
                timeMs = minSum,
                kind = BestLap.Kind.BEST_2
            )
        )
    }

    if (BestLap.Kind.BEST_3 in enabledKinds) {
        val size = minOf(3, validLaps.size)
        val minSum = minWindowSum(validLaps, size, timerPrecision)
        records.add(
            BestLap(
                count = size,
                timeMs = minSum,
                kind = BestLap.Kind.BEST_3
            )
        )
    }

    if (BestLap.Kind.MOST in enabledKinds) {
        val total = validLaps.sumOf { timerPrecision.roundMs(it.timeMs) }
        records.add(
            BestLap(
                count = validLaps.size,
                timeMs = total,
                kind = BestLap.Kind.MOST
            )
        )
    }

    return records.toList()
}

fun computeFlightCounters(laps: List<Lap>): List<Counter> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    return if (validLaps.isNotEmpty()) {
        listOf(
            Counter.Builtin(
                count = validLaps.size,
                kind = Counter.Builtin.Kind.LAP
            )
        )
    } else {
        emptyList()
    }
}

private fun minWindowSum(
    laps: List<Lap>,
    size: Int,
    timerPrecision: TimerPrecision
): Long {
    if (size >= laps.size) {
        return laps.sumOf { timerPrecision.roundMs(it.timeMs) as Long }
    }
    var minSum = Long.MAX_VALUE
    for (i in 0..laps.size - size) {
        var sum = 0L
        for (j in 0 until size) {
            sum += timerPrecision.roundMs(laps[i + j].timeMs)
        }
        if (sum < minSum) minSum = sum
    }
    return minSum
}

fun Stats.Individual.mergeFlightRecords(
    records: List<BestLap>,
    flightCounters: List<Counter>,
): Stats.Individual {
    val newBestLaps = if (records.isNotEmpty()) {
        val bestMap = bestLaps.associateBy { it.kind }.toMutableMap()
        records.forEach { record ->
            val existing = bestMap[record.kind]
            if (existing == null || record.isBetterThan(existing)) {
                bestMap[record.kind] = record
            }
        }
        bestMap.values.toList().sortedBy { it.kind.ordinal }
    } else {
        bestLaps
    }

    val mergedCounters = buildList {
        addAll(counters)
        for (it in flightCounters) {
            mergeIn(it)
        }
        mergeIn(Counter.Builtin(count = 1, kind = Counter.Builtin.Kind.FLIGHT))
        sortWith(Counter.SortComparator)
    }

    return copy(
        bestLaps = newBestLaps,
        counters = mergedCounters
    )
}
