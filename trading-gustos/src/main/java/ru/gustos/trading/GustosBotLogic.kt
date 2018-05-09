package ru.gustos.trading

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import ru.gustos.trading.book.Sheet
import ru.gustos.trading.book.indicators.IndicatorsLib
import java.time.Duration
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask


open class GustosBotLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    var sheet: Sheet
    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XCachedIndicator<XExtBar>
    lateinit var longEma: XCachedIndicator<XExtBar>
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>
    lateinit var sma: XIndicator<XExtBar>
    lateinit var sd: XCachedIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signal2Ema: XCachedIndicator<XExtBar>

    var prepared: Boolean = false

    override fun newInitParams(): SimpleBotLogicParams = SimpleBotLogicParams()

    init {

        sheet = Sheet(IndicatorsLib("indicators_bot.json"))
        println("bars " + bars.size)
        sheet.fromBars(bars)
        of(SimpleBotLogicParams::short, "logic.gustos.short", Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::long, "logic.gustos.long", Duration.ofMinutes(40), Duration.ofMinutes(92), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::signal2, "logic.gustos.signal2", Duration.ofMinutes(156), Duration.ofMinutes(236), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::deviation, "logic.gustos.deviation", 0, 23, 1, false)
        of(SimpleBotLogicParams::deviation2, "logic.gustos.deviation2", 0, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.gustos.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::deviationTimeFrame2, "logic.gustos.deviationTimeFrame2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.gustos.stopLoss", 1.0, 15.0, 0.05, true)
        of(SimpleBotLogicParams::tStopLoss, "logic.gustos.tStopLoss", 1.0, 15.0, 0.05, true)
    }

    override var historyBars: Long
        get() = 100000000L
        set(value) {}


    override fun copyParams(src: SimpleBotLogicParams): SimpleBotLogicParams = src.copy()


    override fun prepareBarsImpl() {
        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, getParams().short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, getParams().long!!)
        macd = XMACDIndicator(shortEma, longEma)
//        signalEma = XMcGinleyIndicator(bars, XExtBar._signalEma, macd, _params.signal!!)
//        signal2Ema = XMcGinleyIndicator(bars, XExtBar._signal2Ema, macd, _params.signal2!!)
        signal2Ema = XDoubleEMAIndicator(bars, XExtBar._signal2Ema1, XExtBar._signal2Ema2, XExtBar._signal2Ema, macd, getParams().signal2!!)

        sd = GustosIndicator2(bars, XExtBar._sd, XExtBar._closePrice, XExtBar._volume, XExtBar._sma, XExtBar._avrVolume, getParams().deviationTimeFrame!!, getParams().deviationTimeFrame2!!)
        sma = object : XIndicator<XExtBar> {
            override fun getValue(index: Int): Double = getBar(index).sma
        }
        val tasks = mutableListOf<ForkJoinTask<*>>()
        tasks.add(ForkJoinPool.commonPool().submit { shortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { longEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { sd.prepare() })
        tasks.forEach { it.join() }

        tasks.clear()
        tasks.add(ForkJoinPool.commonPool().submit { signal2Ema.prepare() })
        tasks.forEach { it.join() }

        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, _ -> getTrendDecision(index) })
        prepared = true

    }

    private fun getTrendDecision(index: Int): Pair<Decision, Map<String, String>> {
        if (!prepared) throw NullPointerException("not prepared!")
        val forest = sheet.data.get(300, index)
        return when {
//            priceLow(index, _params.deviation!!) -> Pair(Decision.BUY, mapOf(Pair("up", "true")))
            forest > 0.5 -> Pair(Decision.BUY, mapOf(Pair("forest", "true")))
            shouldSell(index) -> Pair(Decision.SELL, emptyMap())
            else -> Pair(Decision.NONE, emptyMap())
        }

    }


    fun upperBound(index: Int): Double {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        return sma + sd * getParams().deviation2!! / 10.0

    }

    private fun localDownTrend(index: Int): Boolean {
        return macd.getValue(index) < signal2Ema.getValue(index)
    }

    fun priceLow(index: Int, deviation: Int): Boolean {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        val price = closePrice.getValue(index)
        return price < sma - sd * deviation / 10.0
    }

    fun shouldSell(index: Int): Boolean {
        val price = closePrice.getValue(index)
        return price > upperBound(index) //&& localDownTrend(index)
    }


    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val mm = System.currentTimeMillis()
            bars.add(XExtBar(bar))
            sheet.add(bar, true)
            println("bar added " + (System.currentTimeMillis() - mm));
        }
    }

    override fun insertBars(bars: List<XBaseBar>) {
        synchronized(this) {
            val mm = System.currentTimeMillis()
            bars.forEach { this.bars.add(XExtBar(it)); sheet.add(it, false) }
            sheet.calcIndicatorsForLastBars(bars.size)
            println("bars added " + (System.currentTimeMillis() - mm))
        }
    }


    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf()
    }

    override fun getBotAdviceImpl(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val bar = getBar(index)
            val (decision, decisionArgs) = lastDecisionIndicator.getValue(index)

            val indicators = if (fillIndicators) getIndicators(index) else null


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


