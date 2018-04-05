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
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

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

    lateinit var lastTrendIndicator: XLastTrendIndicator<XExtBar>
    lateinit var trendStartIndicator: XTrendStartIndicator<XExtBar>
    lateinit var tslIndicator: XTslIndicator<XExtBar>
    lateinit var soldBySLIndicator: XSoldBySLIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                short = 1,
                long = 52,
                signal = 160,
                deviationTimeFrame = 26,
                deviation = 11,
                dayShort = 1002,
                dayLong = 327,
                daySignal = 79,
                daySignal2 = 808,
                stopLoss = 1.9044203147701881,
                tStopLoss = 4.252559979496663
        )

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(4), Duration.ofMinutes(8), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(60), Duration.ofMinutes(92), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(156), Duration.ofMinutes(236), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofMinutes(584), Duration.ofMinutes(876), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofMinutes(443), Duration.ofMinutes(1329), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofMinutes(65), Duration.ofMinutes(99), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", Duration.ofMinutes(1815), Duration.ofMinutes(2723), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 1.75, 2.63, 0.05, true)
        of(SimpleBotLogicParams::tStopLoss, "logic.sd3.tStopLoss", 3.29, 4.93, 0.05, true)
    }

    fun funXP(x: Double, p: Double): Double {
        return Math.signum(x) * (Math.pow(Math.abs(x) + 1.0, p) - 1.0)
    }

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Double {

        val targetGoodTrades = 0.8
        val targetProfit = 4.5
        val targetStopLoss = 2
        val targetTStopLoss = 4

        return BotLogic.fine(stats.trades.toDouble(), 20.0, 5.0) +
                BotLogic.fine(stats.goodTrades * (1.0 / targetGoodTrades), 1.0, 2.0) +
                BotLogic.fine(stats.profit * (1 / targetProfit), 1.0, 2.0) +
                funXP(stats.goodTrades / targetGoodTrades - 1.0, 1.0) +
                funXP(stats.profit / targetProfit - 1.0, 0.5) -
                funXP(params.stopLoss / targetStopLoss - 1.0, 0.2) -
                funXP(params.tStopLoss / targetTStopLoss - 1.0, 0.2)
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
        lastTrendIndicator = XLastTrendIndicator(bars, XExtBar._lastTrend, { index, bar -> getTrend(index, bar) })
        trendStartIndicator = XTrendStartIndicator(bars, XExtBar._trendStart, lastTrendIndicator)
        tslIndicator = XTslIndicator(bars, XExtBar._tslIndicator, lastTrendIndicator, closePrice)
        soldBySLIndicator = XSoldBySLIndicator(bars, XExtBar._soldBySLIndicator, lastTrendIndicator, tslIndicator, trendStartIndicator, _params.stopLoss, _params.tStopLoss)

        val tasks = mutableListOf<ForkJoinTask<*>>()
        tasks.add(ForkJoinPool.commonPool().submit { shortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { longEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { sma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { sd.prepare() })
        //tasks.add(ForkJoinPool.commonPool().submit { signalEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayShortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayLongEma.prepare() })
        //tasks.add(ForkJoinPool.commonPool().submit { daySignalEma.prepare() })
        //tasks.add(ForkJoinPool.commonPool().submit { daySignal2Ema.prepare() })
        //tasks.add(ForkJoinPool.commonPool().submit { soldBySLIndicator.prepare() })
        tasks.forEach { it.join() }

//        shortEma.prepare()
//        longEma.prepare()
//        sma.prepare()
//        sd.prepare()
        signalEma.prepare()

//        dayShortEma.prepare()
//        dayLongEma.prepare()
        daySignalEma.prepare()
        daySignal2Ema.prepare()
////        lastTrendIndicator.prepare()
////        trendStartIndicator.prepare()
////        tslIndicator.prepare()
        soldBySLIndicator.prepare()
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
                Pair("tsl", tslIndicator),
                Pair("sl", object : XIndicator<XExtBar> {
                    override fun getValue(index: Int, bar: XExtBar): Double {
                        return trendStartIndicator.getValue(index, bar).closePrice
                    }
                })
        )
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}

    override fun getAdvice(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): Advice {

        synchronized(this) {

            val bar = getBar(index)
            val orderSide = lastTrendIndicator.getValue(index, bar)

            val indicators = if (fillIndicators) getIndicators(index, bar) else null

            //Если SELL, то безусловно продаем
            if (orderSide.side == OrderSide.SELL) {

                //println("${bar.endTime} SELL ${bar.closePrice}")

                return Advice(bar.endTime,
                        OrderSideExt(OrderSide.SELL, false),
                        false,
                        instrument,
                        bar.closePrice,
                        trader?.availableAsset(instrument) ?: 0.0,
                        bar,
                        indicators)
            }

            if (soldBySLIndicator.getValue(index, bar)) {
                return Advice(bar.endTime,
                        OrderSideExt(OrderSide.SELL, false),
                        true,
                        instrument,
                        bar.closePrice,
                        trader?.availableAsset(instrument) ?: 0.0,
                        bar,
                        indicators)
            }

            return Advice(bar.endTime,
                    orderSide,
                    false,
                    instrument,
                    bar.closePrice,
                    if (trader != null) {
                        trader.availableUsd(instrument) / bar.closePrice
                    } else 0.0,
                    bar,
                    indicators)
        }

    }

}