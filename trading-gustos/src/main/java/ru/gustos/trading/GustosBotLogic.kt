package ru.gustos.trading

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.ta.indicators.*
import ru.gustos.trading.book.Sheet
import ru.gustos.trading.book.indicators.IndicatorsLib
import java.time.Duration

open class GustosLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<GustosBotLogicParams>(name, GustosBotLogicParams::class, instrument, barInterval, bars) {

    var sheet: Sheet
    val closePrice = XClosePriceIndicator(bars)

    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>
    lateinit var sma: XIndicator<XExtBar>
    lateinit var sd: XCachedIndicator<XExtBar>


    init {

        _params = GustosBotLogicParams()
        sheet = Sheet(IndicatorsLib("indicators_bot.json"))
        sheet.fromBars(bars)
        of(GustosBotLogicParams::deviation, "logic.gustos.deviation", 15, 23, 1, false)
        of(GustosBotLogicParams::deviation2, "logic.gustos.deviation2", 15, 23, 1, false)
        of(GustosBotLogicParams::deviationTimeFrame, "logic.gustos.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::deviationTimeFrame2, "logic.gustos.deviationTimeFrame2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

    }

    override var historyBars: Long
        get() = 100000000L
        set(value) {}


    override fun copyParams(src: GustosBotLogicParams): GustosBotLogicParams = src.copy()

    override fun prepare() {
        sheet.calcIndicators()
        sd = GustosIndicator2(bars, XExtBar._sd, XExtBar._closePrice, XExtBar._volume, XExtBar._sma, XExtBar._avrVolume, _params.deviationTimeFrame!!, _params.deviationTimeFrame2!!)
        sma = object : XIndicator<XExtBar> {
            override fun getValue(index: Int): Double = getBar(index).sma
        }

        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, bar -> getTrendDecision(index, bar) })

    }

    private fun getTrendDecision(index: Int, bar: XExtBar): Pair<Decision, Map<String, String>> {

        return when {
            priceLow(index, _params.deviation!!) -> Pair(Decision.BUY, mapOf(Pair("up", "true")))
            shouldSell(index) -> Pair(Decision.SELL, emptyMap())
            else -> Pair(Decision.NONE, emptyMap())
        }

    }


    fun upperBound(index: Int): Double {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        return sma + sd * _params.deviation2!! / 10.0

    }

    fun priceLow(index: Int, deviation: Int): Boolean {
        val sd = sd.getValue(index)
        val sma = getBar(index).sma
        val price = closePrice.getValue(index)
        return price < sma - sd * deviation / 10.0
    }

    fun shouldSell(index: Int): Boolean {
        val price = closePrice.getValue(index)
        return price > upperBound(index)
    }


    override fun insertBar(bar: XBar) {
        synchronized(this) {
            bars.add(XExtBar(bar))
            sheet.add(bar)
        }
    }


    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf()
    }

    override fun getBotAdvice(index: Int, stats: TradesStats?, trader: Trader?, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val bar = getBar(index)
            val (decision, decisionArgs) = lastDecisionIndicator.getValue(index)

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