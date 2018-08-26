package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor

interface BotMetrica {
    fun toFloat(): Float
}

class FloatBotMetrica(val value: Float) : BotMetrica, Comparable<FloatBotMetrica> {

    override fun toFloat(): Float = value

    override fun compareTo(other: FloatBotMetrica): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()
}

data class TrainItem<P, R, M>(var args: P, var result: R, var metrica: M) {
    companion object {
        fun <P, R, M> of(args: P, function: (P) -> R, metrica: (P, R) -> M): TrainItem<P, R, M> {
            val result = function(args)
            return TrainItem(args, result, metrica(args, result))
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