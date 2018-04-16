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
                long = 50,
                signal = 203,
                deviationTimeFrame = 25,
                deviation = 12,
                dayShort = 331,
                dayLong = 1494,
                daySignal = 112,
                daySignal2 = 537,
                stopLoss = 2.034337868566384,
                tStopLoss = 4.236120532724698,
                persist1 = 4,
                persist2 = 8,
                persist3 = 1
        )

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 15, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(40), Duration.ofMinutes(92), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(156), Duration.ofMinutes(236), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofMinutes(200), Duration.ofMinutes(500), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofMinutes(1000), Duration.ofMinutes(2000), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofMinutes(65), Duration.ofMinutes(150), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", Duration.ofMinutes(300), Duration.ofMinutes(800), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 1.0, 5.0, 0.05, true)
        of(SimpleBotLogicParams::tStopLoss, "logic.sd3.tStopLoss", 1.0, 5.0, 0.05, true)

        of(SimpleBotLogicParams::persist1, "logic.sd3.persist1", 1, 10, 1, true)
        of(SimpleBotLogicParams::persist2, "logic.sd3.persist2", 1, 10, 1, true)
        of(SimpleBotLogicParams::persist3, "logic.sd3.persist3", 1, 10, 1, true)

    }

    fun funXP(x: Double, p: Double): Double {
        return Math.signum(x) * (Math.pow(Math.abs(x) + 1.0, p) - 1.0)
    }

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Metrica {

        val targetGoodTrades = 0.8
        val targetProfit = 7.0
        val targetStopLoss = 2.0
        val targetTStopLoss = 4.0
        val targetTrades = 100.0

        return Metrica().add("fine_trades", BotLogic.fine(minOf(stats.trades.toDouble(), targetTrades), targetTrades, 3.0))
                .add("fine_goodTrades", BotLogic.fine(stats.goodTrades * (1.0 / targetGoodTrades), 1.0, 2.0))
                .add("fine_profit", BotLogic.fine(stats.profit * (1 / targetProfit), 1.0, 2.0))
                .add("goodTrades", funXP(stats.goodTrades / targetGoodTrades - 1.0, 1.0))
                .add("profit", funXP(stats.profit / targetProfit - 1.0, 2.0))
                .add("sl", -funXP(params.stopLoss / targetStopLoss - 1.0, 0.1))
                .add("tsl", -funXP(params.tStopLoss / targetTStopLoss - 1.0, 0.1))
                .add("pearson", (stats.pearson - 0.95) * 10.0)
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
        tasks.add(ForkJoinPool.commonPool().submit { dayShortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayLongEma.prepare() })
        tasks.forEach { it.join() }

        signalEma.prepare()

        daySignalEma.prepare()
        daySignal2Ema.prepare()
        soldBySLIndicator.prepare()
    }

    fun localUpTrend(index: Int, bar: XExtBar): Boolean {
        return macd.getValue(index, bars[index]) > signalEma.getValue(index, bars[index])
    }

    fun localDownTrend(index: Int, bar: XExtBar): Boolean {
        return macd.getValue(index, bars[index]) < signalEma.getValue(index, bars[index])
    }

    fun dayUpTrend(index: Int, bar: XExtBar): Boolean {
        return dayMacd.getValue(index, bars[index]) > daySignalEma.getValue(index, bars[index])
    }

    fun dayDownTrend(index: Int, bar: XExtBar): Boolean {
        return dayMacd.getValue(index, bars[index]) < daySignal2Ema.getValue(index, bars[index])
    }

    fun getTrend(index: Int, bar: XExtBar): Pair<Decision, Map<String, String>> {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)

        if ((maxOf(0, index - _params.persist1!!)..index).all { dayDownTrend(it, bar) }) {
            return Pair(Decision.BUY, mapOf(Pair("down", "true")))
        } else {

            return when {
                price < sma - sd * _params.deviation!! / 10.0
                        && (maxOf(0, index - _params.persist2!!)..index).all { localUpTrend(it, bar) } //проверять последние 10 баров
                        && (maxOf(0, index - _params.persist3!!)..index).all { dayUpTrend(it, bar) }
                -> Pair(Decision.BUY, mapOf(Pair("up", "true"))) //проверять последние 2 бара

                price > sma + sd * _params.deviation!! / 10.0 && localDownTrend(index, bar) -> Pair(Decision.SELL, emptyMap())
                else -> Pair(Decision.NONE, emptyMap())
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

    override fun getBotAdvice(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val bar = getBar(index)
            val (decision, decisionArgs) = lastTrendIndicator.getValue(index, bar)

            val indicators = if (fillIndicators) getIndicators(index, bar) else null

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

            if (soldBySLIndicator.getValue(index, bar)) {
                return BotAdvice(bar.endTime,
                        Decision.SELL,
                        mapOf(Pair("sl", "true")),
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