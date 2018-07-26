package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.Order
import ru.efreet.trading.exchange.TradeRecord

/**
 * Created by fluder on 23/02/2018.
 */
interface Trader {

    val usd: Double

    val instruments: Collection<Instrument>

    fun price(instrument: Instrument): Double

    fun availableAsset(instrument: Instrument): Double

    fun executeAdvice(advice: BotAdvice): TradeRecord?

    fun history(): TradeHistory

    fun getOpenOrders(instrument: Instrument): List<Order>

    fun cancelAllOrders(instrument: Instrument)

    fun updateBalance(force: Boolean = true)
}