package ru.efreet.trading.exchange

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.impl.Binance
//import ru.efreet.trading.exchange.impl.Poloniex
import java.time.ZonedDateTime

/**
 * Created by fluder on 08/02/2018.
 */
interface Exchange {

    fun getName(): String

    fun getBalancesMap(): Map<String, Float>

    fun buy(instrument: Instrument, asset: Float, price: Float, type: OrderType, now:ZonedDateTime): Order

    fun sell(instrument: Instrument, asset: Float, price: Float, type: OrderType, now:ZonedDateTime): Order

    fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar>

    fun getLastTrades(instrument: Instrument): List<AggTrade>

//    fun startTrade(instrument: Instrument, interval: BarInterval, consumer: (XBar, Boolean) -> Unit)
//
//    fun stopTrade()

    fun getFee(): Float

    fun getIntervals(): List<BarInterval>

    fun getTicker(): Map<Instrument, Ticker>

    fun getOpenOrders(instrument: Instrument): List<Order>

    fun cancelOrder(order: Order)

    companion object {
        fun getExchange(name: String): Exchange {
            return when (name) {
                //"poloniex" -> Poloniex()
                "binance" -> Binance()
                else -> throw RuntimeException("exchange $name not found")
            }
        }
    }
}