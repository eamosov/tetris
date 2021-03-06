package ru.gustos.trading

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractXExtBarBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import java.time.Duration

open class GustosBotLogic2WithMacd(name: String, instrument: Instrument, barInterval: BarInterval) : AbstractXExtBarBotLogic<GustosBotLogicParams2>(name, GustosBotLogicParams2::class, instrument, barInterval) {
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

    init {

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

    override val historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()

    override fun copyParams(src: GustosBotLogicParams2): GustosBotLogicParams2 = src.copy()


    override fun prepareBars() {
        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, params.macdShort!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, params.macdLong!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, params.macdSignal!!)
        shortEma.prepare()
        longEma.prepare()
        signalEma.prepare()

//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(params.buyWindow!!, params.buyVolumeWindow!!, params.volumeShort!!)
        bars.forEach { val (sma, sd) = garBuy.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.sma = sma!!.toFloat();it.sd = sd!!.toFloat(); }
        garSell = GustosAverageRecurrent(params.sellWindow!!, params.sellVolumeWindow!!, params.volumeShort!!)
        bars.forEach { val (sma, sd) = garSell.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.smaSell = sma.toFloat();it.sdSell = sd.toFloat(); }
        garBuy2 = GustosAverageRecurrent(params.buyWindow2!!, params.buyVolumeWindow2!!, params.volumeShort2!!)
        bars.forEach { val (sma, sd) = garBuy2.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.sma2 = sma.toFloat();it.sd2 = sd.toFloat(); }
        garSell2 = GustosAverageRecurrent(params.sellWindow2!!, params.sellVolumeWindow2!!, params.volumeShort2!!)
        bars.forEach { val (sma, sd) = garSell2.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.smaSell2 = sma.toFloat();it.sdSell2 = sd.toFloat(); }
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, _ -> getTrendDecision(index) })
        prepared = true
        lastDecisionIndicator.prepare()
        super.prepareBars()
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
            val p = pbar.sma - pbar.sd * params.buyDiv!! / 10
            return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * params.buyBoundDiv!! / 10 && !falling(i)
        } else {
            val p = pbar.sma2 - pbar.sd2 * params.buyDiv2!! / 10
            return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma2 - bar.sd2 * params.buyBoundDiv2!! / 10 && !falling(i)

        }
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
        if (upTrend(i)) {
            val p = bar.smaSell + bar.sdSell * params.sellDiv!! / 10
            return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * params.sellBoundDiv!! / 10 && !rising(i)
        } else {
            val p = bar.smaSell2 + bar.sdSell2 * params.sellDiv2!! / 10
            return bar.maxPrice >= p && bar.closePrice > bar.smaSell2 + bar.sdSell2 * params.sellBoundDiv2!! / 10 && !rising(i)
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


    override fun insertBar(bar: XBar, marketBar: MarketBar?) {
        synchronized(this) {
            val b = XExtBar(bar)
            val (sma, sd) = garBuy.feed(bar.closePrice.toDouble(), bar.volume.toDouble())
            b.sma = sma!!.toFloat()
            b.sd = sd!!.toFloat()
            val (sma2, sd2) = garSell.feed(bar.closePrice.toDouble(), bar.volume.toDouble())
            b.smaSell = sma2.toFloat()
            b.sdSell = sd2.toFloat()
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


    override fun indicators(): Map<String, XIndicator> {
        return mapOf()
    }

    override fun metrica(params: GustosBotLogicParams2, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toFloat(), 100.0f, 4.0f))
                .add("fine_sma10", BotLogic.fine(stats.sma10, 1.0f, 10.0f))
                .add("fine_profit", BotLogic.fine(stats.profit, 4.0f))
                .add("profit", stats.relProfit / 4.0f)
                .add("pearson", (stats.pearson - 0.96f) * 40.0f)
    }


    override fun getAdviceImpl(index: Int, fillIndicators: Boolean): BotAdvice {

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