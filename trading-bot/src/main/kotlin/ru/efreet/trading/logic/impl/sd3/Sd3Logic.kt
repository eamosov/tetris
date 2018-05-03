package ru.efreet.trading.logic.impl.sd3

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.trainer.Metrica
import java.time.Duration
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

/**
 * Created by fluder on 20/02/2018.
 */
open class Sd3Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XCachedIndicator<XExtBar>
    lateinit var longEma: XCachedIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XCachedIndicator<XExtBar>
    lateinit var signal2Ema: XCachedIndicator<XExtBar>

    lateinit var sma: XIndicator<XExtBar>
    lateinit var sd: XCachedIndicator<XExtBar>

    lateinit var dayShortEma: XCachedIndicator<XExtBar>
    lateinit var dayLongEma: XCachedIndicator<XExtBar>
    lateinit var dayMacd: XMACDIndicator<XExtBar>
    lateinit var daySignalEma: XCachedIndicator<XExtBar>
    lateinit var daySignal2Ema: XCachedIndicator<XExtBar>

    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>
    lateinit var decisionStartIndicator: XDecisionStartIndicator<XExtBar>
    lateinit var tslIndicator: XTslIndicator<XExtBar>
    lateinit var soldBySLIndicator: XSoldBySLIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                long = 50,
                signal = 203,
                signal2 = 203,
                dayLong = 1494,
                daySignal = 112,
                daySignal2 = 537
//                persist1 = 4,
//                persist2 = 8,
//                persist3 = 1
        )

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviation2, "logic.sd3.deviation2", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::deviationTimeFrame2, "logic.sd3.deviationTimeFrame2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(40), Duration.ofMinutes(92), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(156), Duration.ofMinutes(236), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::signal2, "logic.sd3.signal2", Duration.ofMinutes(156), Duration.ofMinutes(236), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofMinutes(200), Duration.ofMinutes(500), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofMinutes(1000), Duration.ofMinutes(2000), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofMinutes(65), Duration.ofMinutes(150), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", Duration.ofMinutes(300), Duration.ofMinutes(800), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 1.0, 5.0, 0.05, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.sd3.tStopLoss", 1.0, 5.0, 0.05, true)
//
//        of(SimpleBotLogicParams::takeProfit, "logic.sd3.takeProfit", 1.0, 20.0, 0.05, true)
//        of(SimpleBotLogicParams::tTakeProfit, "logic.sd3.tTakeProfit", 0.1, 5.0, 0.05, true)


        of(SimpleBotLogicParams::persist1, "logic.sd3.persist1", 0, 50, 1, true)
        of(SimpleBotLogicParams::persist2, "logic.sd3.persist2", 0, 50, 1, true)
        of(SimpleBotLogicParams::persist3, "logic.sd3.persist3", 0, 50, 1, true)

    }

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Metrica {

        val targetGoodTrades = 0.7
        val targetProfit = Math.pow(1.015, Duration.between(stats.start, stats.end).toHours().toDouble() / 24.0)
        val targetStopLoss = 10.0
        val targetAvrProfitPerTrade = 1.1
//        val targetTStopLoss = 1.0
//        val targetTrades = 120.0

        return Metrica()
                //.add("fine_trades", BotLogic.fine(minOf(stats.trades.toDouble(), targetTrades), targetTrades, 3.0))
                //.add("fine_goodTrades", BotLogic.fine(minOf(stats.goodTrades, targetGoodTrades) * (1.0 / targetGoodTrades), 1.0, 2.0))
                .add("fine_profit", BotLogic.fine(minOf(stats.profit, targetProfit), targetProfit, 5.0))
                .add("goodTrades", BotLogic.funXP(stats.goodTrades / targetGoodTrades - 1.0, 1.8))
                .add("ppt", 10.0 * BotLogic.funXP(stats.avrProfitPerTrade / targetAvrProfitPerTrade - 1.0, 2.0))
                .add("profit", stats.profit * 0.15)
                //.add("trades", stats.trades * 0.0175)
                .add("sl", -BotLogic.funXP(params.stopLoss / targetStopLoss - 1.0, 0.5))
                .add("persist1", -BotLogic.funXP(params.persist1!!.toDouble() - 1.0, 0.02))
                .add("persist2", -BotLogic.funXP(params.persist2!!.toDouble() - 1.0, 0.02))
                .add("persist3", -BotLogic.funXP(params.persist3!!.toDouble() - 1.0, 0.02))
//                .add("tsl", -BotLogic.funXP(params.tStopLoss / targetTStopLoss - 1.0, 0.1))
//                .add("tp", -BotLogic.funXP(params.takeProfit / 5.0 - 1.0, 0.1))
//                .add("ttp", -BotLogic.funXP(params.tTakeProfit / 0.2 - 1.0, 0.1))
                .add("pearson", (stats.pearson - 0.98) * 100.0)
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams = orig.copy()

    override fun prepare() {

//        shortEma = XMcGinleyIndicator(bars, XExtBar._shortEma, closePrice, _params.short!!)
//        longEma = XMcGinleyIndicator(bars, XExtBar._longEma, closePrice, _params.long!!)
        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, _params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, _params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
//        signalEma = XMcGinleyIndicator(bars, XExtBar._signalEma, macd, _params.signal!!)
//        signal2Ema = XMcGinleyIndicator(bars, XExtBar._signal2Ema, macd, _params.signal2!!)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, _params.signal!!)
        signal2Ema = XDoubleEMAIndicator(bars, XExtBar._signal2Ema1, XExtBar._signal2Ema2, XExtBar._signal2Ema, macd, _params.signal2!!)

        sd = GustosIndicator2(bars, XExtBar._sd, XExtBar._closePrice, XExtBar._volume, XExtBar._sma, XExtBar._avrVolume, _params.deviationTimeFrame!!, _params.deviationTimeFrame2!!)
        sma = object : XIndicator<XExtBar> {
            override fun getValue(index: Int): Double = getBar(index).sma
        }

        dayShortEma = XEMAIndicator(bars, XExtBar._dayShortEma, closePrice, _params.dayShort!!)
        dayLongEma = XEMAIndicator(bars, XExtBar._dayLongEma, closePrice, _params.dayLong!!)
        dayMacd = XMACDIndicator(dayShortEma, dayLongEma)
        daySignalEma = XEMAIndicator(bars, XExtBar._daySignalEma, dayMacd, _params.daySignal!!)
        daySignal2Ema = XEMAIndicator(bars, XExtBar._daySignal2Ema, dayMacd, _params.daySignal2!!)
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, bar -> getTrendDecision(index, bar) })
        decisionStartIndicator = XDecisionStartIndicator(bars, XExtBar._decisionStart, lastDecisionIndicator)
        tslIndicator = XTslIndicator(bars, XExtBar._tslIndicator, lastDecisionIndicator, closePrice)
        soldBySLIndicator = XSoldBySLIndicator(bars, XExtBar._soldBySLIndicator, lastDecisionIndicator, tslIndicator, decisionStartIndicator, _params.stopLoss, _params.tStopLoss, _params.takeProfit, _params.tTakeProfit)

        val tasks = mutableListOf<ForkJoinTask<*>>()
        tasks.add(ForkJoinPool.commonPool().submit { shortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { longEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { sd.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayShortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayLongEma.prepare() })
        tasks.forEach { it.join() }

        tasks.clear()
        tasks.add(ForkJoinPool.commonPool().submit { signalEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { signal2Ema.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { daySignalEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { daySignal2Ema.prepare() })
        tasks.forEach { it.join() }

        soldBySLIndicator.prepare()
    }

    private fun localUpTrend(index: Int): Boolean {
        return macd.getValue(index) > signalEma.getValue(index)
    }

    private fun localDownTrend(index: Int): Boolean {
        return macd.getValue(index) < signal2Ema.getValue(index)
    }

    private fun dayUpTrend(index: Int): Boolean {
        return dayMacd.getValue(index) > daySignalEma.getValue(index)
    }

    private fun dayDownTrend(index: Int): Boolean {
        return dayMacd.getValue(index) < daySignal2Ema.getValue(index)
    }

    private fun getTrendDecision(index: Int, bar: XExtBar): Pair<Decision, Map<String, String>> {

        val sd = sd.getValue(index)
        val sma = bar.sma
        val price = closePrice.getValue(index)

        return if ((maxOf(0, index - _params.persist1!!)..index).all { dayDownTrend(it) }) {
            Pair(Decision.BUY, mapOf(Pair("down", "true")))
        } else {

            when {
                priceLow(index,_params.deviation!!)
                        && (maxOf(0, index - _params.persist2!!)..index).all { localUpTrend(it) } //проверять последние 10 баров
                        && (maxOf(0, index - _params.persist3!!)..index).all { dayUpTrend(it) }
                -> Pair(Decision.BUY, mapOf(Pair("up", "true"))) //проверять последние 2 бара

                shouldSell(index) -> Pair(Decision.SELL, emptyMap())
                else -> Pair(Decision.NONE, emptyMap())
            }
        }
    }

    fun upperBound(index : Int) : Double {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        return sma + sd*_params.deviation2!! / 10.0

    }

    fun priceLow(index : Int, deviation : Int) : Boolean {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        val price = closePrice.getValue(index)
        return price < sma - sd * deviation / 10.0
    }

    fun shouldSell(index : Int) : Boolean {
        val price = closePrice.getValue(index)
        return price > upperBound(index) //&& localDownTrend(index)
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
                    override fun getValue(index: Int): Double {
                        return decisionStartIndicator.getValue(index).closePrice
                    }
                })
        )
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}

    override fun getBotAdvice(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val bar = getBar(index)
            val (decision, decisionArgs) = lastDecisionIndicator.getValue(index)

            val indicators = if (fillIndicators) getIndicators(index, bar) else null


            if (soldBySLIndicator.getValue(index)) {
                return BotAdvice(bar.endTime,
                        Decision.SELL,
                        mapOf(Pair("sl", "true")),
                        instrument,
                        bar.closePrice,
                        trader?.availableAsset(instrument) ?: 0.0,
                        bar,
                        indicators)
            }


            //Если SELL, то безусловно продаем
            if (decision == Decision.SELL) {

                //println("${bar.endTime} SELL ${bar.closePrice}")

                return BotAdvice(bar.endTime,
                        decision,
                        decisionArgs,
                        instrument,
                        bar.closePrice,
                        trader?.availableAsset(instrument) ?: 0.0,
                        bar,
                        indicators)
            }

            if (decision == Decision.BUY) {
                return BotAdvice(bar.endTime,
                        decision,
                        decisionArgs,
                        instrument,
                        bar.closePrice,
                        trader?.let { it.availableUsd(instrument) / bar.closePrice } ?: 0.0,
                        bar,
                        indicators)
            }

            return BotAdvice(bar.endTime,
                    Decision.NONE,
                    decisionArgs,
                    instrument,
                    bar.closePrice,
                    0.0,
                    bar,
                    indicators)
        }

    }

}