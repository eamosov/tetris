package ru.efreet.trading.logic.impl.sd3

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.Advice
import ru.efreet.trading.bot.OrderSideExt
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class Sd3Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XDoubleEMAIndicator<XExtBar>
    lateinit var longEma: XDoubleEMAIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XDoubleEMAIndicator<XExtBar>

    lateinit var sma: XSMAIndicator<XExtBar>
    lateinit var sd: XStandardDeviationIndicator<XExtBar>


    lateinit var dayShortEma: XEMAIndicator<XExtBar>
    lateinit var dayLongEma: XEMAIndicator<XExtBar>
    lateinit var dayMacd: XMACDIndicator<XExtBar>
    lateinit var daySignalEma: XEMAIndicator<XExtBar>
    lateinit var daySignal2Ema: XEMAIndicator<XExtBar>

    lateinit var trend: XLastTrendIndicator<XExtBar>
    lateinit var trendStart: XTrendStartIndicator<XExtBar>
    lateinit var tslIndicator: XTslIndicator<XExtBar>
    lateinit var xSoldBySLIndicator: XSoldBySLIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                short = 50,
                long = 97,
                signal = 108,
                deviationTimeFrame = 10,
                deviation = 10,
                dayShort = 283,
                dayLong = 1749,
                daySignal = 540,
                daySignal2 = 2806,
                stopLoss = 5.72,
                tStopLoss = 1.5
        )

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 8, 12, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(40), Duration.ofMinutes(60), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(77), Duration.ofMinutes(117), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(86), Duration.ofMinutes(130), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofMinutes(226), Duration.ofMinutes(340), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofMinutes(1399), Duration.ofMinutes(2099), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofMinutes(432), Duration.ofMinutes(648), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", Duration.ofMinutes(2244), Duration.ofMinutes(3368), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 4.0, 7.0, 0.25, true)
        of(SimpleBotLogicParams::tStopLoss, "logic.sd3.tStopLoss", 1.0, 3.0, 0.1, true)
    }

    override fun metrica(stats: TradesStats): Double {

        val targetGoodTrades = 0.8
        val targetProfit = 1.1

        return BotLogic.fine(stats.goodTrades * (1.0 / targetGoodTrades), 1.0, 2.0) +
                BotLogic.fine(stats.profit * (1 / targetProfit), 1.0, 2.0) +
                stats.goodTrades * (1.0 / targetGoodTrades) +
                stats.profit * (1.0 / targetProfit)
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams = orig.copy()

    override fun prepare() {

        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, _params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, _params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, _params.signal!!)

        sma = XSMAIndicator(bars, XExtBar._sma, closePrice, _params.deviationTimeFrame!!)
        sd = XStandardDeviationIndicator(bars, XExtBar._sd, closePrice, sma, _params.deviationTimeFrame!!)

        dayShortEma = XEMAIndicator(bars, XExtBar._dayShortEma, closePrice, _params.dayShort!!)
        dayLongEma = XEMAIndicator(bars, XExtBar._dayLongEma, closePrice, _params.dayLong!!)
        dayMacd = XMACDIndicator(dayShortEma, dayLongEma)
        daySignalEma = XEMAIndicator(bars, XExtBar._daySignalEma, dayMacd, _params.daySignal!!)
        daySignal2Ema = XEMAIndicator(bars, XExtBar._daySignal2Ema, dayMacd, _params.daySignal2!!)
        trend = XLastTrendIndicator(bars, XExtBar._trend, { index, bar -> getTrend(index, bar) })
        trendStart = XTrendStartIndicator(bars, XExtBar._trendStart, trend)
        tslIndicator = XTslIndicator(bars, trend, closePrice)
        xSoldBySLIndicator = XSoldBySLIndicator(bars, trend, tslIndicator, trendStart, _params.stopLoss, _params.tStopLoss)


        shortEma.prepare()
        longEma.prepare()
        sma.prepare()
        sd.prepare()
        signalEma.prepare()

        dayShortEma.prepare()
        dayLongEma.prepare()
        daySignalEma.prepare()
        daySignal2Ema.prepare()
        trendStart.prepare()
    }

    fun getTrend(index: Int, bar: XExtBar): OrderSideExt? {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)


        fun localUpTrend(index: Int, bar: XExtBar): Boolean {
            return macd.getValue(index, bars[index]) > signalEma.getValue(index, bars[index])
        }

        val macd = macd.getValue(index, bar)
        val signalEma = signalEma.getValue(index, bar)


        fun dayUpTrend2(index: Int, bar: XExtBar): Boolean {
            return dayMacd.getValue(index, bars[index]) > daySignal2Ema.getValue(index, bars[index])
        }

        fun dayLowTrend(index: Int, bar: XExtBar): Boolean {
            return dayMacd.getValue(index, bars[index]) < daySignalEma.getValue(index, bars[index])
        }

        if ((maxOf(0, index - 4)..index).all { dayUpTrend2(it, bar) }) {
            //Всегда покупать на установившемся восходящем тренде
            return OrderSideExt(OrderSide.BUY, true)
        } else {

            //Нисходящем тренде покупать в локальном минимуме и продовать в локальном максимуме
            return when {
                price < sma - sd * _params.deviation!! / 10.0 &&
                        (maxOf(0, index - 9)..index).all { localUpTrend(it, bar) } && //проверять последние 10 баров
                        (maxOf(0, index - 1)..index).all { dayLowTrend(it, bar) } -> OrderSideExt(OrderSide.BUY, false) //проверять последние 2 бара

                price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma -> OrderSideExt(OrderSide.SELL, false)
                else -> null
            }
        }

    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("sma", sma),
                Pair("sd", sd),
                Pair("shortEma", shortEma),
                Pair("longEma", longEma),
                Pair("price", closePrice),
                Pair("macd", XMinusIndicator(macd, signalEma)),
                Pair("dayShortEma", dayShortEma),
                Pair("dayLongEma", dayLongEma),
                Pair("sl", tslIndicator)
        )
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}

    override fun getAdvice(index: Int, stats: TradesStats?, trader: Trader, fillIndicators: Boolean): Advice {

        val bar = getBar(index)
        val orderSide = trend.getValue(index, bar)

//        val _advice = if (stats == null || isProfitable(stats)) {
//            getAdvice(index, bar)
//        } else {
//            //println("Dangerous statistic, SELL all")
//            Pair(OrderSide.SELL, false)
//        }
//
//        val advice = _advice?.first
//        val long = _advice?.second ?: false

        val indicators = if (fillIndicators) getIndicators(index, bar) else null

        //Если SELL, то безусловно продаем
        if (orderSide.side == OrderSide.SELL) {

            //println("${bar.endTime} SELL ${bar.closePrice}")

            return Advice(bar.endTime,
                    OrderSideExt(OrderSide.SELL, false),
                    false,
                    false,
                    null,
                    instrument,
                    bar.closePrice,
                    trader.availableAsset(instrument),
                    bar,
                    indicators)
        }

        if (xSoldBySLIndicator.getValue(index, bar)) {
            return Advice(bar.endTime,
                    OrderSideExt(OrderSide.SELL, false),
                    true,
                    true,
                    null,
                    instrument,
                    bar.closePrice,
                    trader.availableAsset(instrument),
                    bar,
                    indicators)
        }

//        val trendStart = trendStart.getValue(index, bar)
//
//        //Проверяем StopLoss
//        if (bar.closePrice < (1.0 - _params.stopLoss / 100.0) * trendStart.closePrice) {
//
//            return Advice(bar.endTime,
//                    OrderSideExt(OrderSide.SELL, false),
//                    true,
//                    false,
//                    null,
//                    instrument,
//                    bar.closePrice,
//                    trader.availableAsset(instrument),
//                    bar,
//                    indicators)
//        }
//
//
//        val lastTrade = trader.lastTrade()
//
////        //Повышаем TSL, если надо
////        if (lastTrade?.tsl != null && (1.0 - _params.tStopLoss / 100.0) * bar.closePrice > lastTrade.tsl!!) {
////            lastTrade.tsl = (1.0 - _params.tStopLoss / 100.0) * bar.closePrice
////        }
//
//        //Проверка на SL/TSL
//        if (lastTrade != null
//                && lastTrade.side == OrderSide.BUY
//                //&& (lastTrade.tsl != null && bar.closePrice < lastTrade.tsl!!)
//                && (lastTrade.long == true && bar.closePrice < tslIndicator.getValue(index, bar) * (1.0 - _params.tStopLoss / 100.0))
//                ) {
//
//            //val tsl = lastTrade.tsl != null && bar.closePrice < lastTrade.tsl!!
//
//            //println("${bar.endTime} SELL ${if (tsl) "TSL" else "SL"} ${bar.closePrice}")
//
//            return Advice(bar.endTime,
//                    OrderSideExt(OrderSide.SELL, lastTrade.long ?: false),
//                    false,
//                    true,
//                    null,
//                    instrument,
//                    bar.closePrice,
//                    trader.availableAsset(instrument),
//                    bar,
//                    indicators)
//
//        }
//
//
//        //Не надо покупать в текущем uptrend, если продали по (T)SL
//        if (lastTrade != null &&
//                lastTrade.side == OrderSide.SELL &&
//                ((lastTrade.sellBySl == true) || (lastTrade.sellByTsl == true)) &&
//                lastTrade.time!!.isAfter(trendStart.endTime)) {
//
//            return Advice(bar.endTime,
//                    null,
//                    false,
//                    false,
//                    null,
//                    instrument,
//                    bar.closePrice,
//                    0.0,
//                    bar,
//                    indicators)
//        }

        //если пред бар - продажа, то этот бар - начало покупок


        return Advice(bar.endTime,
                orderSide,
                false,
                false,
                if (orderSide.long) {
                    (1.0 - _params.tStopLoss / 100.0) * bar.closePrice
                } else null,
                instrument,
                bar.closePrice,
                trader.availableUsd(instrument) / bar.closePrice,
                bar,
                indicators)

    }

}