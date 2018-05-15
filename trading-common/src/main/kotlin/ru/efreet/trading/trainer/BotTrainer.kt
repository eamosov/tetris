package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor

interface BotMetrica {
    fun toDouble(): Double
}

class DoubleBotMetrica(val value: Double) : BotMetrica, Comparable<DoubleBotMetrica> {

    override fun toDouble(): Double = value

    override fun compareTo(other: DoubleBotMetrica): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()
}

data class TrainItem<P, R, M>(var args: P, var result: R, var metrica: M) {
    companion object {
        fun <P, R, M> of(args: P, function: (P) -> R, metrica: (P, R) -> M): TrainItem<P, R, M> {
            val result = function(args)
            val metrica = metrica(args, result)
            return TrainItem(args, result, metrica)
        }
    }
}


interface BotTrainer<P, R, M> where M : Comparable<M>, M : BotMetrica {
    fun getBestParams(genes: List<PropertyEditor<P, Any?>>,
                      population: List<P>,
                      function: (P) -> R, metrica: (P, R) -> M,
                      copy: (P) -> P,
                      newBest: ((TrainItem<P, R, M>) -> Unit)? = null): List<TrainItem<P, R, M>>
}