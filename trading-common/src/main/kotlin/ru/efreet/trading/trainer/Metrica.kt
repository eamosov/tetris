package ru.efreet.trading.trainer

import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.round5

class Metrica(val elements: MutableList<MetricaTerm> = mutableListOf(),
                   var value: Double = 0.0) : Comparable<Metrica>, BotMetrica {

    fun add(term: MetricaTerm): Metrica {
        elements.add(term)
        value += term.value
        return this
    }

    fun add(name: String, value: Double): Metrica {
        return add(MetricaTerm(name, value))
    }

    fun get(name: String) : Double{
        elements.forEach { if (it.name.equals(name)) return it.value; }
        return 0.0;
    }

    override fun compareTo(other: Metrica): Int {
        return this.value.compareTo(other.value)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(value.round5())
        sb.append(" (")
        for (e in elements){
            sb.append("${e.name}:${e.value.round5()} ")
        }
        sb.append(")")
        return sb.toString()
    }

    override fun toDouble(): Double {
        return value
    }
}