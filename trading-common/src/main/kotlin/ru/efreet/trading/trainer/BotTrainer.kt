package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor

interface BotTrainer {
    fun <P, R, M:Comparable<M>> getBestParams(genes: List<PropertyEditor<P, Any?>>,
                             origin: List<P>,
                             function: (P) -> R, metrica: (P, R) -> M,
                             copy: (P) -> P,
                             newBest: ((P, R) -> Unit)? = null): Pair<P, R>
}