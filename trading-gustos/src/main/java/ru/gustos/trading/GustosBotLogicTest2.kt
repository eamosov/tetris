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
import ru.gustos.trading.book.indicators.GustosVolumeLevel2
import ru.gustos.trading.book.indicators.LevelsTrader
import java.time.Duration

open class GustosBotLogicTest2(name: String, instrument: Instrument, barInterval: BarInterval) : AbstractXExtBarBotLogic<GustosBotLogicParams>(name, GustosBotLogicParams::class, instrument, barInterval) {

    val closePrice = XClosePriceIndicator(bars)

    var prepared: Boolean = false
    lateinit var garBuy: GustosAverageRecurrent
    lateinit var garSell: GustosAverageRecurrent
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    lateinit var shortEma: XCachedIndicator<XExtBar>
    lateinit var longEma: XCachedIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XCachedIndicator<XExtBar>
    lateinit var levels: GustosVolumeLevel2
    lateinit var levelsTrade: LevelsTrader


    override fun newInitParams(): GustosBotLogicParams = GustosBotLogicParams()

    init {

        of(GustosBotLogicParams::buyWindow, "buyWindow", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::buyVolumeWindow, "buyVolumeWindow", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::sellWindow, "sellWindow", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofSeconds(1), false)
        of(GustosBotLogicParams::sellVolumeWindow, "sellVolumeWindow", Duration.ofMinutes(32), Duration.ofMinutes(48), Duration.ofSeconds(1), false)

        of(GustosBotLogicParams::volumeShort, "volumeShort", 0, 23, 1, false)
        of(GustosBotLogicParams::buyDiv, "buyDiv", 0, 23, 1, false)
        of(GustosBotLogicParams::sellDiv, "sellDiv", 0, 23, 1, false)
        of(GustosBotLogicParams::buyBoundDiv, "buyBoundDiv", 0, 23, 1, false)
        of(GustosBotLogicParams::sellBoundDiv, "sellBoundDiv", 0, 23, 1, false)

        of(GustosBotLogicParams::volumePow1, "volumePow1", 1, 50, 1, false)
        of(GustosBotLogicParams::volumePow2, "volumePow2", 1, 50, 1, false)

//        of(SimpleBotLogicParams::stopLoss, "logic.gustos2.stopLoss", 1.0, 15.0, 0.05, true)
//        of(SimpleBotLogicParams::tStopLoss, "logic.gustos2.tStopLoss", 1.0, 15.0, 0.05, true)
    }

    override val historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()

    override fun copyParams(src: GustosBotLogicParams): GustosBotLogicParams = src.copy()


    override fun prepareBars() {

//        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, 600)
//        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, 1300)
//        macd = XMACDIndicator(shortEma, longEma)
//        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, 450)
//        shortEma.prepare()
//        longEma.prepare()
//        signalEma.prepare()

        levels = GustosVolumeLevel2(0.9998, 0.9, 1.2)
        levelsTrade = LevelsTrader(80, 250, 0.0025)


//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(params.buyWindow!!, params.buyVolumeWindow!!, params.volumeShort!!)//, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        garSell = GustosAverageRecurrent(params.sellWindow!!, params.sellVolumeWindow!!, params.volumeShort!!)//, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        bars.forEach { doBar(it) }
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, _ -> getTrendDecision(index) })
        prepared = true
        lastDecisionIndicator.prepare()
        barsIsPrepared
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
//        if (!upTrend(i)) return false;
        val pbar = getBar(i - 1)
        val bar = getBar(i)
//        if (bar.closePrice<bar.sma2*0.999) return false
        val p = pbar.sma - pbar.sd * params.buyDiv!! / 10
        return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * params.buyBoundDiv!! / 10 && !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
//        if (bar.sd2<0) return true
        val p = bar.smaSell + bar.sdSell * params.sellDiv!! / 10
        return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * params.sellBoundDiv!! / 10 && !rising(i)

    }

    private fun upTrend(index: Int): Boolean {
        return macd.getValue(index) > signalEma.getValue(index)
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

    fun doBar(b: XExtBar) {
        val (sma, sd) = garBuy.feed(b.closePrice.toDouble(), b.volume.toDouble())
        b.sma = sma!!.toFloat()
        b.sd = sd!!.toFloat()
        val (sma2, sd2) = garSell.feed(b.closePrice.toDouble(), b.volume.toDouble())
        b.smaSell = sma2!!.toFloat()
        b.sdSell = sd2!!.toFloat()

        b.sma2 = levels.feed(b).toFloat()
        levelsTrade.feed(b, b.sma2.toDouble())
        b.sdSell2 = levelsTrade.sd().toFloat()
        b.sd2 = if (levelsTrade.high()) 1.0f else -1.0f
        b.avrVolume = levelsTrade.longVolume().toFloat()
        b.avrVolume2 = levelsTrade.shortVolume().toFloat()

    }

    override fun insertBar(bar: XBar, marketBar: MarketBar?) {
        synchronized(this) {
            val b = XExtBar(bar)
            doBar(b)
            bars.add(b)
        }
    }

    override fun metrica(params: GustosBotLogicParams, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toFloat(), 50.0f, 2.0f))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0f))
    }

    override fun indicators(): Map<String, XIndicator> {
        return emptyMap()
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