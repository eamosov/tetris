package ru.efreet.trading.trainer

data class Metrica(val elements: MutableList<MetricaTerm> = mutableListOf(),
                   var value: Double = 0.0) : Comparable<Metrica> {

    fun add(term: MetricaTerm): Metrica {
        elements.add(term)
        value += term.value
        return this
    }

    fun add(name: String, value: Double): Metrica {
        return add(MetricaTerm(name, value))
    }

    override fun compareTo(other: Metrica): Int {
        return this.value.compareTo(other.value)
    }
}