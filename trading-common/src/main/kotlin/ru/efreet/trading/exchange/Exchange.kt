package ru.efreet.trading.exchange

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.impl.Binance
import ru.efreet.trading.exchange.impl.Poloniex
import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.round5
import java.time.ZonedDateTime

/**
 * Created by fluder on 08/02/2018.
 */
interface Exchange {

    fun getName(): String

    fun getBalancesMap(): Map<String, Double>

    fun getPricesMap(): Map<Instrument, Double>

    fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord

    fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord

    fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar>

    fun getLastTrades(instrument: Instrument): List<AggTrade>

    fun startTrade(instrument: Instrument, interval: BarInterval, consumer: (XBar, Boolean) -> Unit)

    fun stopTrade()

    fun getFee(): Double

    fun getIntervals(): List<BarInterval>

    fun getTicker(): Map<Instrument, Ticker>

    data class CalBalanceResult(val toBase: Map<String, Double>, val balances: Map<String, Double>, val ticker: Map<Instrument, Ticker>)

    fun calcBalance(toBase: String): CalBalanceResult {
        val balances = getBalancesMap()
        val ticker = getTicker()

        val ret = hashMapOf<String, Double>()

        var total = 0.0

        for ((asset, amount) in balances) {

            if (asset == toBase) {
                total += amount
                ret[toBase] = amount
            } else {

                if (ticker.containsKey(Instrument(asset, toBase))) {
                    val b = amount * ticker[Instrument(asset, toBase)]!!.highestBid
                    total += b
                    ret[asset] = b

                } else if (ticker.containsKey(Instrument(toBase, asset))) {

                    val b = amount / ticker[Instrument(toBase, asset)]!!.lowestAsk
                    total += b
                    ret[asset] = b
                }
            }
        }

        ret["total"] = total
        return CalBalanceResult(ret, balances, ticker)
    }

//    fun logBalance(baseName: String, settings: BotSettings? = null) {
//        val bln = calcBalance(baseName)
//        for ((asset, amount) in bln.toBase) {
//            if (asset != "total") {
//                val instrument = Instrument(asset, baseName)
//                val order = settings?.getLastTrade(instrument)
//                println("$asset : ${(bln.balances[asset]!! * 10000).toInt() / 10000.0}  ( ${(amount * 100).toInt() / 100.0} ${baseName}, price = ${bln.ticker[instrument]?.highestBid}, order=${order?.side}/${order?.amount}/${order?.price})")
//
//            }
//        }
//
//        println("total: ${(bln.toBase["total"]!! * 100).toInt() / 100.0}  ${baseName}")
//    }

    fun logBalance(baseName: String) {
        val bln = calcBalance(baseName)
        for ((asset, amount) in bln.toBase) {
            if (asset != "total") {
                println("$asset : ${bln.balances[asset]!!.round5()}  ( ${amount.round2()} ${baseName})")
            }
        }

        println("total: ${(bln.toBase["total"]!! * 100).toInt() / 100.0}  ${baseName}")
    }

    companion object {
        fun getExchange(name: String): Exchange {
            return when (name) {
                "poloniex" -> Poloniex()
                "binance" -> Binance()
                else -> throw RuntimeException("exchange $name not found")
            }
        }
    }
}