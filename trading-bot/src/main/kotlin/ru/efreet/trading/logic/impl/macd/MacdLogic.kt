package ru.efreet.trading.logic.impl.macd

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.Advice
import ru.efreet.trading.bot.OrderSideExt
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import java.time.Duration
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

/**
 * Created by fluder on 20/02/2018.
 */
class MacdLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XDoubleEMAIndicator<XExtBar>
    lateinit var longEma: XDoubleEMAIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XDoubleEMAIndicator<XExtBar>

//    lateinit var lastTrendIndicator: XLastTrendIndicator<XExtBar>
//    lateinit var trendStartIndicator: XTrendStartIndicator<XExtBar>
//    lateinit var tslIndicator: XTslIndicator<XExtBar>
//    lateinit var soldBySLIndicator: XSoldBySLIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                short = 344,
                long = 1044,
                signal = 1187,
                stopLoss = 2.0,
                tStopLoss = 4.0
        )

        of(SimpleBotLogicParams::short, "logic.macd.short", 5, 2000, 1, false)
        of(SimpleBotLogicParams::long, "logic.macd.long", 5, 2000, 1, false)
        of(SimpleBotLogicParams::signal, "logic.macd.signal", 5, 2000, 1, false)

//        of(SimpleBotLogicParams::stopLoss, "logic.macd.stopLoss", 1.0, 10.0, 0.05, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.macd.tStopLoss", 1.0, 10.0, 0.05, true)
    }

    fun funXP(x: Double, p: Double): Double {
        return Math.signum(x) * (Math.pow(Math.abs(x) + 1.0, p) - 1.0)
    }

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Double {

        val targetGoodTrades = 0.8
        val targetProfit = 4.5
        val targetStopLoss = 2
        val targetTStopLoss = 4

//        return BotLogic.fine(stats.trades.toDouble(), 20.0, 5.0) +
//                BotLogic.fine(stats.goodTrades * (1.0 / targetGoodTrades), 1.0, 2.0) +
//                BotLogic.fine(stats.profit * (1 / targetProfit), 1.0, 2.0) +
//                funXP(stats.goodTrades / targetGoodTrades - 1.0, 1.0) +
//                funXP(stats.profit / targetProfit - 1.0, 0.5) -
//                funXP(params.stopLoss / targetStopLoss - 1.0, 0.2) -
//                funXP(params.tStopLoss / targetTStopLoss - 1.0, 0.2) +
//                (stats.pearson - 0.9) * 10.0

        return stats.profit
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams = orig.copy()

    override fun prepare() {

        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, _params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, _params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, _params.signal!!)

//        lastTrendIndicator = XLastTrendIndicator(bars, XExtBar._lastTrend, { index, bar -> getTrend(index, bar) })
//        trendStartIndicator = XTrendStartIndicator(bars, XExtBar._trendStart, lastTrendIndicator)
//        tslIndicator = XTslIndicator(bars, XExtBar._tslIndicator, lastTrendIndicator, closePrice)
//        soldBySLIndicator = XSoldBySLIndicator(bars, XExtBar._soldBySLIndicator, lastTrendIndicator, tslIndicator, trendStartIndicator, _params.stopLoss, _params.tStopLoss)

        val tasks = mutableListOf<ForkJoinTask<*>>()
        tasks.add(ForkJoinPool.commonPool().submit { shortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { longEma.prepare() })
        tasks.forEach { it.join() }

        signalEma.prepare()

//        soldBySLIndicator.prepare()
    }

//    fun getTrend(index: Int, bar: XExtBar): OrderSideExt? {
//
//        return when {
//            macd.getValue(index, bars[index]) > signalEma.getValue(index, bars[index]) -> OrderSideExt(OrderSide.BUY, true)
//            macd.getValue(index, bars[index]) < signalEma.getValue(index, bars[index]) -> OrderSideExt(OrderSide.SELL, false)
//            else -> null
//        }
//    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(
                Pair("shortEma", shortEma),
                Pair("longEma", longEma),
                Pair("price", closePrice),
                Pair("macd", XMinusIndicator(macd, signalEma))/*,
                Pair("tsl", tslIndicator),
                Pair("sl", object : XIndicator<XExtBar> {
                    override fun getValue(index: Int, bar: XExtBar): Double {
                        return trendStartIndicator.getValue(index, bar).closePrice
                    }
                })*/
        )
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}

    override fun getAdvice(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): Advice {

        synchronized(this) {

            val orderSide = when {
                macd.getValue(index, bars[index]) > signalEma.getValue(index, bars[index]) -> OrderSideExt(OrderSide.BUY, true)
                macd.getValue(index, bars[index]) < signalEma.getValue(index, bars[index]) -> OrderSideExt(OrderSide.SELL, false)
                else -> null
            }

//
            val bar = getBar(index)
//            val orderSide = lastTrendIndicator.getValue(index, bar)
//
            val indicators = if (fillIndicators) getIndicators(index, bar) else null
//
//            //Если SELL, то безусловно продаем
//            if (orderSide.side == OrderSide.SELL) {
//
//                //println("${bar.endTime} SELL ${bar.closePrice}")
//
//                return Advice(bar.endTime,
//                        OrderSideExt(OrderSide.SELL, false),
//                        false,
//                        instrument,
//                        bar.closePrice,
//                        trader?.availableAsset(instrument) ?: 0.0,
//                        bar,
//                        indicators)
//            }
//
//            if (soldBySLIndicator.getValue(index, bar)) {
//                return Advice(bar.endTime,
//                        OrderSideExt(OrderSide.SELL, false),
//                        true,
//                        instrument,
//                        bar.closePrice,
//                        trader?.availableAsset(instrument) ?: 0.0,
//                        bar,
//                        indicators)
//            }
//
            return Advice(bar.endTime,
                    orderSide,
                    false,
                    instrument,
                    bar.closePrice,
                    if (trader != null && orderSide != null) {
                        if (orderSide.side == OrderSide.BUY)
                            trader.availableUsd(instrument) / bar.closePrice
                        else
                            trader.availableAsset(instrument)
                    } else 0.0,
                    bar,
                    indicators)
        }

    }

}