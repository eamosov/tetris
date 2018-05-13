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
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import java.time.Duration

open class GustosBotLogic2WithMacd(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<GustosBotLogicParams2>(name, GustosBotLogicParams2::class, instrument, barInterval, bars) {
    val closePrice = XClosePriceIndicator(bars)

    var prepared: Boolean = false
    lateinit var garBuy: GustosAverageRecurrent
    lateinit var garSell: GustosAverageRecurrent
    lateinit var garBuy2: GustosAverageRecurrent
    lateinit var garSell2: GustosAverageRecurrent
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    lateinit var shortEma: XCachedIndicator<XExtBar>
    lateinit var longEma: XCachedIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XCachedIndicator<XExtBar>


    override fun newInitParams(): GustosBotLogicParams2 = GustosBotLogicParams2()

    override fun onInit() {

        of(GustosBotLogicParams2::buyWindow, "buyWindow", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::buyVolumeWindow, "buyVolumeWindow", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::sellWindow, "sellWindow", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::sellVolumeWindow, "sellVolumeWindow", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(GustosBotLogicParams2::volumeShort, "volumeShort", 0, 23, 1, false)
        of(GustosBotLogicParams2::buyDiv, "buyDiv", 0, 23, 1, false)
        of(GustosBotLogicParams2::sellDiv, "sellDiv", 0, 23, 1, false)
        of(GustosBotLogicParams2::buyBoundDiv, "buyBoundDiv", 0, 23, 1, false)
        of(GustosBotLogicParams2::sellBoundDiv, "sellBoundDiv", 0, 23, 1, false)

        of(GustosBotLogicParams2::buyWindow2, "buyWindow2", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::buyVolumeWindow2, "buyVolumeWindow2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::sellWindow2, "sellWindow2", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams2::sellVolumeWindow2, "sellVolumeWindow2", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(GustosBotLogicParams2::volumeShort2, "volumeShort2", 0, 23, 1, false)
        of(GustosBotLogicParams2::buyDiv2, "buyDiv2", 0, 23, 1, false)
        of(GustosBotLogicParams2::sellDiv2, "sellDiv2", 0, 23, 1, false)
        of(GustosBotLogicParams2::buyBoundDiv2, "buyBoundDiv2", 0, 23, 1, false)
        of(GustosBotLogicParams2::sellBoundDiv2, "sellBoundDiv2", 0, 23, 1, false)

        of(GustosBotLogicParams2::macdShort, "macdShort", 1, 2000, 1, false)
        of(GustosBotLogicParams2::macdLong, "macdLong", 1, 2000, 1, false)
        of(GustosBotLogicParams2::macdSignal, "macdSignal", 1, 2000, 1, false)

//        of(SimpleBotLogicParams::stopLoss, "logic.gustos2.stopLoss", 1.0, 15.0, 0.05, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.gustos2.tStopLoss", 1.0, 15.0, 0.05, true)
    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: GustosBotLogicParams2): GustosBotLogicParams2 = src.copy()


    override fun prepareBarsImpl() {
        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, getParams().macdShort!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, getParams().macdLong!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, getParams().macdSignal!!)
        shortEma.prepare()
        longEma.prepare()
        signalEma.prepare()

//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(getParams().buyWindow!!, getParams().buyVolumeWindow!!, getParams().volumeShort!!)
        bars.forEach { val (sma, sd) = garBuy.feed(it.closePrice, it.volume); it.sma = sma;it.sd = sd; }
        garSell = GustosAverageRecurrent(getParams().sellWindow!!, getParams().sellVolumeWindow!!, getParams().volumeShort!!)
        bars.forEach { val (sma, sd) = garSell.feed(it.closePrice, it.volume); it.smaSell = sma;it.sdSell = sd; }
        garBuy2 = GustosAverageRecurrent(getParams().buyWindow2!!, getParams().buyVolumeWindow2!!, getParams().volumeShort2!!)
        bars.forEach { val (sma, sd) = garBuy2.feed(it.closePrice, it.volume); it.sma2 = sma;it.sd2 = sd; }
        garSell2 = GustosAverageRecurrent(getParams().sellWindow2!!, getParams().sellVolumeWindow2!!, getParams().volumeShort2!!)
        bars.forEach { val (sma, sd) = garSell2.feed(it.closePrice, it.volume); it.smaSell2 = sma;it.sdSell2 = sd; }
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

    private fun upTrend(index: Int): Boolean {
        return macd.getValue(index) > signalEma.getValue(index)
    }




    private fun shouldBuy(i: Int): Boolean {
        val pbar = getBar(i - 1)
        val bar = getBar(i)
        if (upTrend(i)) {
            val p = pbar.sma - pbar.sd * getParams().buyDiv!! / 10
            return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * getParams().buyBoundDiv!! / 10 && !falling(i)
        } else {
            val p = pbar.sma2 - pbar.sd2 * getParams().buyDiv2!! / 10
            return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma2 - bar.sd2 * getParams().buyBoundDiv2!! / 10 && !falling(i)

        }
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
        if (upTrend(i)) {
            val p = bar.smaSell + bar.sdSell * getParams().sellDiv!! / 10
            return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * getParams().sellBoundDiv!! / 10 && !rising(i)
        } else {
            val p = bar.smaSell2 + bar.sdSell2 * getParams().sellDiv2!! / 10
            return bar.maxPrice >= p && bar.closePrice > bar.smaSell2 + bar.sdSell2 * getParams().sellBoundDiv2!! / 10 && !rising(i)
        }
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

    override fun metrica(params: GustosBotLogicParams2, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toDouble(), 100.0, 4.0))
                .add("fine_sma10", BotLogic.fine(stats.sma10, 1.0, 10.0))
                .add("fine_profit", BotLogic.fine(stats.profit, 4.0))
                .add("profit", stats.relProfit/4.0)
                .add("pearson", (stats.pearson - 0.96) * 40.0)
    }


    override fun getBotAdviceImpl(index: Int, trader: Trader?, fillIndicators: Boolean): BotAdvice {

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