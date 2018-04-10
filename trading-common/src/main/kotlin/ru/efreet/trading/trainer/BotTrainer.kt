package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor

interface BotTrainer {
    fun <P, R> getBestParams(genes: List<PropertyEditor<P, Any?>>,
                             origin: List<P>,
                             function: (P) -> R, metrica: (P, R) -> Double,
                             copy: (P) -> P,
                             newBest: ((P, R) -> Unit)? = null): Pair<P, R>
}