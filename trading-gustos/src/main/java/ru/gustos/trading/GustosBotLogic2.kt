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
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.ta.indicators.XLastDecisionIndicator
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import java.time.Duration

open class GustosBotLogic2(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<GustosBotLogicParams>(name, GustosBotLogicParams::class, instrument, barInterval, bars) {

    var prepared: Boolean = false
    lateinit var garBuy: GustosAverageRecurrent
    lateinit var garSell: GustosAverageRecurrent
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    override fun newInitParams(): GustosBotLogicParams = GustosBotLogicParams()

    override fun onInit() {

        of(GustosBotLogicParams::deviationTimeFrame, "logic.gustos2.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::deviationTimeFrame2, "logic.gustos2.deviationTimeFrame2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::deviationTimeFrameSell, "logic.gustos2.deviationTimeFrameSell", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::deviationTimeFrameSell2, "logic.gustos2.deviationTimeFrameSell2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        resetGenes()

        of(GustosBotLogicParams::short, "logic.gustos2.short", 0, 23, 1, false)
        of(GustosBotLogicParams::deviation, "logic.gustos2.deviation", 0, 23, 1, false)
        of(GustosBotLogicParams::deviation2, "logic.gustos2.deviation2", 0, 23, 1, false)
//        of(SimpleBotLogicParams::deviationTimeFrame3, "logic.gustos2.deviationTimeFrame3", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
//        of(SimpleBotLogicParams::deviationTimeFrameSell3, "logic.gustos2.deviationTimeFrameSell3", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
//        of(SimpleBotLogicParams::persist1, "logic.gustos2.persist1", 0, 50, 1, false)
//        of(SimpleBotLogicParams::persist2, "logic.gustos2.persist2", 0, 50, 1, false)
        of(GustosBotLogicParams::deviation3, "logic.gustos2.deviation3", 0, 23, 1, false)

//        of(SimpleBotLogicParams::stopLoss, "logic.gustos2.stopLoss", 1.0, 15.0, 0.05, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.gustos2.tStopLoss", 1.0, 15.0, 0.05, true)
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: GustosBotLogicParams): GustosBotLogicParams = src.copy()


    override fun prepareBarsImpl() {
//        println("timeframe1 ${_params.deviationTimeFrame} timeframe2 ${_params.deviationTimeFrame2} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(getParams().deviationTimeFrame!!, getParams().deviationTimeFrame2!!, getParams().short!!, getParams().persist1!! / 10.0, getParams().persist2!! / 10.0)
        bars.forEach { val (sma, sd) = garBuy.feed(it.closePrice, it.volume); it.sma = sma;it.sd = sd; }
        garSell = GustosAverageRecurrent(getParams().deviationTimeFrameSell!!, getParams().deviationTimeFrameSell2!!, getParams().short!!, getParams().persist1!! / 10.0, getParams().persist2!! / 10.0)
        bars.forEach { val (sma, sd) = garSell.feed(it.closePrice, it.volume); it.smaSell = sma;it.sdSell = sd; }
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, _ -> getTrendDecision(index) })
        prepared = true
        lastDecisionIndicator.prepare()

    }

    private fun getTrendDecision(index: Int): Pair<Decision, Map<String, String>> {
        if (!prepared) throw NullPointerException("not prepared!")
        if (index == 0) return Pair(Decision.NONE, emptyMap())

        return when {
            shouldBuy(index) -> Pair(Decision.BUY, emptyMap())
            shouldSell(index) -> Pair(Decision.SELL, emptyMap())
            else -> Pair(Decision.NONE, emptyMap())
        }

    }

    private fun shouldBuy(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        val p = pbar.sma - pbar.sd * getParams().deviation!! / 10
        return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * getParams().deviation3!! / 10 && !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
        val p = bar.smaSell + bar.sdSell * getParams().deviation2!! / 10
        return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * getParams().deviation3!! / 10 && !rising(i)

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


    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val b = XExtBar(bar)
            val (sma, sd) = garBuy.feed(bar.closePrice, bar.volume)
            b.sma = sma
            b.sd = sd
            val (sma2, sd2) = garSell.feed(bar.closePrice, bar.volume)
            b.smaSell = sma2
            b.sdSell = sd2
            bars.add(b)
        }
    }

//    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Metrica {
//
//        return Metrica()
//                .add("fine_profit", BotLogic.fine(stats.profit, 1.0))
//                .add("profit", BotLogic.fine(stats.profit, 1.0))
//                .add("pearson", BotLogic.fine(stats.pearson, - 0.92))
//    }


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


