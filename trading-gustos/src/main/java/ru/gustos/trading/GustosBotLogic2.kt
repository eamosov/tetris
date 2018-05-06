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
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import ru.gustos.trading.book.indicators.IndicatorsLib
import java.time.Duration
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

open class GustosBotLogic2(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    var prepared : Boolean = false
    lateinit var gar : GustosAverageRecurrent
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    init {

        _params = SimpleBotLogicParams()
        of(SimpleBotLogicParams::deviation, "logic.gustos2.deviation", 0, 23, 1, false)
        of(SimpleBotLogicParams::deviation2, "logic.gustos2.deviation2", 0, 23, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.gustos2.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(SimpleBotLogicParams::deviationTimeFrame2, "logic.gustos2.deviationTimeFrame2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.gustos2.stopLoss", 1.0, 15.0, 0.05, true)
        of(SimpleBotLogicParams::tStopLoss, "logic.gustos2.tStopLoss", 1.0, 15.0, 0.05, true)
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: SimpleBotLogicParams): SimpleBotLogicParams = src.copy()


    override fun prepare() {
//        println("timeframe1 ${_params.deviationTimeFrame} timeframe2 ${_params.deviationTimeFrame2} bars ${bars.size}")
        gar = GustosAverageRecurrent(_params.deviationTimeFrame!!,_params.deviationTimeFrame2!!)
        bars.forEach { val (sma,sd) = gar.feed(it.closePrice,it.volume); it.sma = sma;it.sd = sd; }
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, bar -> getTrendDecision(index, bar) })
        prepared = true
        lastDecisionIndicator.prepare()

    }

    private fun getTrendDecision(index: Int, bar: XExtBar): Pair<Decision, Map<String, String>> {
        if (!prepared) throw NullPointerException("not prepared!")
        if (index==0) return Pair(Decision.NONE, emptyMap())

        return when {
            shouldBuy(index) -> Pair(Decision.BUY, emptyMap())
            shouldSell(index) -> Pair(Decision.SELL, emptyMap())
            else -> Pair(Decision.NONE, emptyMap())
        }

    }

    private fun shouldBuy(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        val p = pbar.sma- pbar.sd * _params.deviation!!/10
        return bar.minPrice <= p && bar.maxPrice >= p && !falling(i) && bar.closePrice < bar.sma - bar.sd

    }

    private fun shouldSell(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        val p = pbar.sma+ pbar.sd * _params.deviation2!!/10
        return bar.minPrice <= p && bar.maxPrice >= p && !rising(i) && bar.closePrice > bar.sma + bar.sd

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
            val (sma,sd) = gar.feed(bar.closePrice,bar.volume)
            b.sma = sma
            b.sd = sd
            bars.add(b)
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