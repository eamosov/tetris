package ru.efreet.trading.logic.impl.sd3

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractXExtBarBotLogic
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
open class Sd3Logic(name: String, instrument: Instrument, barInterval: BarInterval) : AbstractXExtBarBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XCachedIndicator<XExtBar>
    lateinit var longEma: XCachedIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XCachedIndicator<XExtBar>
    lateinit var signal2Ema: XCachedIndicator<XExtBar>

    lateinit var sma: XIndicator
    lateinit var sd: XCachedIndicator<XExtBar>

    lateinit var dayShortEma: XCachedIndicator<XExtBar>
    lateinit var dayLongEma: XCachedIndicator<XExtBar>
    lateinit var dayMacd: XMACDIndicator<XExtBar>
    lateinit var daySignalEma: XCachedIndicator<XExtBar>
    lateinit var daySignal2Ema: XCachedIndicator<XExtBar>

    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>
    lateinit var decisionStartIndicator: XDecisionStartIndicator
    lateinit var tslIndicator: XTslIndicator
    lateinit var soldBySLIndicator: XSoldBySLIndicator

    override fun newInitParams(): SimpleBotLogicParams {
        return SimpleBotLogicParams(
                long = 50,
                signal = 203,
                signal2 = 203,
                dayLong = 1494,
                daySignal = 112,
                daySignal2 = 537,

                tStopLoss = 50.0F,
                takeProfit = 100.0F
        )
    }

    init {

        describeCommomParams()

        of(SimpleBotLogicParams::deviationTimeFrame2, "logic.sd3.deviationTimeFrame2", 80, 240, 1, false)
    }

    fun describeCommomParams() {
        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviation2, "logic.sd3.deviation2", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", 20, 60, 1, false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", 1, 200, 1, false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", 200, 1000, 1, false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", 200, 500, 1, false)
        of(SimpleBotLogicParams::signal2, "logic.sd3.signal2", 1, 100, 1, false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", 1, 2000, 1, false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", 1, 2000, 1, false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", 1, 2000, 1, false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", 1, 2000, 1, false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 1.0F, 10.0F, 0.05F, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.sd3.tStopLoss", 1.0, 5.0, 0.05, true)
//
//        of(SimpleBotLogicParams::takeProfit, "logic.sd3.takeProfit", 1.0, 20.0, 0.05, true)
//        of(SimpleBotLogicParams::tTakeProfit, "logic.sd3.tTakeProfit", 0.1, 5.0, 0.05, true)


        of(SimpleBotLogicParams::persist1, "logic.sd3.persist1", 0, 50, 1, true)
        of(SimpleBotLogicParams::persist2, "logic.sd3.persist2", 0, 50, 1, true)
        of(SimpleBotLogicParams::persist3, "logic.sd3.persist3", 0, 50, 1, true)
    }

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Metrica {

        val targetGoodTrades = 0.7F
        val targetProfit = Math.pow(1.015, Duration.between(stats.start, stats.end).toHours().toDouble() / 24.0).toFloat()
        val targetStopLoss = 10.0F
        val targetAvrProfitPerTrade = 1.0075F
//        val targetTStopLoss = 1.0
//        val targetTrades = 120.0

        return Metrica()
                .add("fine_trades", BotLogic.fine(minOf(stats.trades.toFloat(), 30.0F), 30.0F, 3.0F))
                //.add("fine_goodTrades", BotLogic.fine(minOf(stats.goodTrades, targetGoodTrades) * (1.0 / targetGoodTrades), 1.0, 2.0))
                .add("fine_profit", BotLogic.fine(minOf(stats.profit, targetProfit), targetProfit, 5.0F))
                .add("goodTrades", BotLogic.funXP(stats.goodTrades / targetGoodTrades - 1.0F, 1.8F))
                .add("ppt", 10.0F * BotLogic.fine(stats.profitPerTrade, targetAvrProfitPerTrade, 2.0F))
                .add("profit", stats.relProfit * 0.15F)
                //.add("trades", stats.trades * 0.0175)
                .add("sl", -BotLogic.funXP(params.stopLoss / targetStopLoss - 1.0F, 0.5F))
                .add("persist1", -BotLogic.funXP(params.persist1!!.toFloat() - 1.0F, 0.02F))
                .add("persist2", -BotLogic.funXP(params.persist2!!.toFloat() - 1.0F, 0.02F))
                .add("persist3", -BotLogic.funXP(params.persist3!!.toFloat() - 1.0F, 0.02F))
//                .add("tsl", -BotLogic.funXP(params.tStopLoss / targetTStopLoss - 1.0, 0.1))
//                .add("tp", -BotLogic.funXP(params.takeProfit / 5.0 - 1.0, 0.1))
//                .add("ttp", -BotLogic.funXP(params.tTakeProfit / 0.2 - 1.0, 0.1))
//                .add("pearson", (stats.pearson - 0.98) * 100.0)
    }

    override fun copyParams(src: SimpleBotLogicParams): SimpleBotLogicParams = src.copy()

    override fun prepareBars() {

//        shortEma = XMcGinleyIndicator(bars, XExtBar._shortEma, closePrice, _params.short!!)
//        longEma = XMcGinleyIndicator(bars, XExtBar._longEma, closePrice, _params.long!!)
        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, params.signal!!)
        signal2Ema = XDoubleEMAIndicator(bars, XExtBar._signal2Ema1, XExtBar._signal2Ema2, XExtBar._signal2Ema, macd, params.signal2!!)

        sd = GustosIndicator2(bars, XExtBar._sd, XExtBar._closePrice, XExtBar._volume, XExtBar._sma, XExtBar._avrVolume, params.deviationTimeFrame!!, params.deviationTimeFrame2!!)
        sma = object : XIndicator {
            override fun getValue(index: Int): Float = getBar(index).sma
        }

        dayShortEma = XEMAIndicator(bars, XExtBar._dayShortEma, closePrice, params.dayShort!!)
        dayLongEma = XEMAIndicator(bars, XExtBar._dayLongEma, closePrice, params.dayLong!!)
        dayMacd = XMACDIndicator(dayShortEma, dayLongEma)
        daySignalEma = XEMAIndicator(bars, XExtBar._daySignalEma, dayMacd, params.daySignal!!)
        daySignal2Ema = XEMAIndicator(bars, XExtBar._daySignal2Ema, dayMacd, params.daySignal2!!)
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, bar -> getTrendDecision(index, bar) })
        decisionStartIndicator = XDecisionStartIndicator(bars, XExtBar._decisionStart, lastDecisionIndicator)
        tslIndicator = XTslIndicator(bars, XExtBar._tslIndicator, lastDecisionIndicator, closePrice)
        soldBySLIndicator = XSoldBySLIndicator(bars, XExtBar._soldBySLIndicator, lastDecisionIndicator, tslIndicator, decisionStartIndicator, params.stopLoss, params.tStopLoss, params.takeProfit, params.tTakeProfit)

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

        super.prepareBars()
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

    private fun rising(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        return pbar.closePrice < bar.minPrice
    }

    private fun falling(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        return pbar.closePrice >= bar.maxPrice
    }

    open protected fun getTrendDecision(index: Int, bar: XExtBar): Pair<Decision, Map<String, String>> {

        return if ((maxOf(0, index - params.persist1!!)..index).all { dayDownTrend(it) } && !falling(index)) {
            Pair(Decision.BUY, mapOf(Pair("down", "true")))
        } else {

            when {
                priceLow(index, params.deviation!!)
                        && (maxOf(0, index - params.persist2!!)..index).all { localUpTrend(it) } //проверять последние 10 баров
                        && (maxOf(0, index - params.persist3!!)..index).all { dayUpTrend(it) }
                -> Pair(Decision.BUY, mapOf(Pair("up", "true"))) //проверять последние 2 бара

                shouldSell(index) -> Pair(Decision.SELL, emptyMap())
                else -> Pair(Decision.NONE, emptyMap())
            }
        }
    }

    fun upperBound(index: Int): Double {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        return sma + sd * params.deviation2!! / 10.0

    }

    fun priceLow(index: Int, deviation: Int): Boolean {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        val price = closePrice.getValue(index)
        return price < sma - sd * deviation / 10.0
    }

    fun shouldSell(index: Int): Boolean {
        val price = closePrice.getValue(index)
        return price > upperBound(index) && localDownTrend(index)
    }

    override fun indicators(): Map<String, XIndicator> {
        return mapOf(Pair("sma", sma),
                Pair("sd", sd),
                Pair("shortEma", shortEma),
                Pair("longEma", longEma),
                Pair("price", closePrice),
                Pair("macd", XMinusIndicator(macd, signalEma)),
                Pair("dayShortEma", dayShortEma),
                Pair("dayLongEma", dayLongEma),
                Pair("tsl", tslIndicator),
                Pair("sl", object : XIndicator {
                    override fun getValue(index: Int): Float {
                        return decisionStartIndicator.getValue(index).closePrice
                    }
                })
        )
    }

    override val historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()

    override fun getAdviceImpl(index: Int, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val bar = getBar(index)
            val (decision, decisionArgs) = lastDecisionIndicator.getValue(index)

            val indicators = if (fillIndicators) getIndicators(index) else null


            if (soldBySLIndicator.getValue(index)) {
                return BotAdvice(bar.endTime,
                        Decision.SELL,
                        mapOf(Pair("sl", "true")),
                        instrument,
                        bar.closePrice,
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
                        bar,
                        indicators)
            }

            if (decision == Decision.BUY) {
                return BotAdvice(bar.endTime,
                        decision,
                        decisionArgs,
                        instrument,
                        bar.closePrice,
                        bar,
                        indicators)
            }

            return BotAdvice(bar.endTime,
                    Decision.NONE,
                    decisionArgs,
                    instrument,
                    bar.closePrice,
                    bar,
                    indicators)
        }

    }

}