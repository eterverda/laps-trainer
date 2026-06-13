package ru.fpvladder.laps.trainer.model

sealed class Counter {
    abstract val count: Int
    abstract val kind: Builtin.Kind?
    abstract fun withAddedCount(other: Counter): Counter

    data class Builtin(
        override val count: Int = 1,
        override val kind: Kind
    ) : Counter() {
        override fun withAddedCount(other: Counter): Counter {
            require(other is Builtin && other.kind == kind)
            return copy(count = count + other.count)
        }

        enum class Kind { FLIGHT, LAP }
    }

    data class Custom(
        override val count: Int = 1,
        val text: String
    ) : Counter() {
        override val kind: Builtin.Kind?
            get() = null

        override fun withAddedCount(other: Counter): Counter {
            require(other is Custom && other.text == text)
            return copy(count = count + other.count)
        }
    }

    companion object {
        val SINGLE_FLIGHT = Builtin(count = 1, kind = Builtin.Kind.FLIGHT)
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
    Counter.Builtin.Kind.FLIGHT, Counter.Builtin.Kind.LAP -> other.kind == k
    null -> (this as Counter.Custom).text == (other as Counter.Custom).text
}

internal fun MutableList<Counter>.mergeIn(counter: Counter) {
    val index = indexOfFirst { it.same(counter) }
    if (index >= 0) {
        this[index] = this[index].withAddedCount(counter)
    } else {
        add(counter)
    }
}

internal fun mergeCounterLists(
    existing: List<Counter>,
    new: List<Counter>
): List<Counter> {
    return buildList {
        addAll(existing)
        for (counter in new) {
            mergeIn(counter)
        }
        sortWith(Counter.SortComparator)
    }
}
