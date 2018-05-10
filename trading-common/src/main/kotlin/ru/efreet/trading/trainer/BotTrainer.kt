package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor

interface BotMetrica {
    fun toDouble(): Double
}

class DoubleBotMetrica(val value: Double) : BotMetrica, Comparable<DoubleBotMetrica> {

    override fun toDouble(): Double {
        return value
    }

    override fun compareTo(other: DoubleBotMetrica): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.toString()
    }
}

interface BotTrainer<P, R, M> where M : Comparable<M>, M : BotMetrica {
    fun getBestParams(genes: List<PropertyEditor<P, Any?>>,
                                origin: List<P>,
                                function: (P) -> R, metrica: (P, R) -> M,
                                copy: (P) -> P,
                                newBest: ((P, R) -> Unit)? = null): Pair<P, R>
}