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
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import ru.gustos.trading.book.indicators.GustosVolumeLevel
import ru.gustos.trading.book.indicators.GustosVolumeLevel2
import ru.gustos.trading.book.indicators.LevelsTrader
import java.time.Duration

open class GustosBotLogicTest2(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<GustosBotLogicParams>(name, GustosBotLogicParams::class, instrument, barInterval, bars) {

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
    lateinit var levelsTrade : LevelsTrader


    override fun newInitParams(): GustosBotLogicParams = GustosBotLogicParams()

    override fun onInit() {

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

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: GustosBotLogicParams): GustosBotLogicParams = src.copy()


    override fun prepareBarsImpl() {

//        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, 600)
//        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, 1300)
//        macd = XMACDIndicator(shortEma, longEma)
//        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, 450)
//        shortEma.prepare()
//        longEma.prepare()
//        signalEma.prepare()

        levels = GustosVolumeLevel2(0.9998,0.9,1.2)
        levelsTrade = LevelsTrader(80,250,0.0025)


//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(getParams().buyWindow!!, getParams().buyVolumeWindow!!, getParams().volumeShort!!)//, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        garSell = GustosAverageRecurrent(getParams().sellWindow!!, getParams().sellVolumeWindow!!, getParams().volumeShort!!)//, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        bars.forEach { doBar(it) }
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
//        if (!upTrend(i)) return false;
        val pbar = getBar(i - 1)
        val bar = getBar(i)
//        if (bar.closePrice<bar.sma2*0.999) return false
        val p = pbar.sma - pbar.sd * getParams().buyDiv!! / 10
        return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * getParams().buyBoundDiv!! / 10 && !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
//        if (bar.sd2<0) return true
        val p = bar.smaSell + bar.sdSell * getParams().sellDiv!! / 10
        return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * getParams().sellBoundDiv!! / 10 && !rising(i)

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

    fun doBar(b : XExtBar){
        val (sma, sd) = garBuy.feed(b.closePrice, b.volume)
        b.sma = sma
        b.sd = sd
        val (sma2, sd2) = garSell.feed(b.closePrice, b.volume)
        b.smaSell = sma2
        b.sdSell = sd2

        b.sma2 = levels.feed(b)
        levelsTrade.feed(b,b.sma2)
        b.sdSell2 = levelsTrade.sd()
        b.sd2 = if (levelsTrade.high())  1.0 else -1.0
        b.avrVolume = levelsTrade.longVolume()
        b.avrVolume2 = levelsTrade.shortVolume()

    }

    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val b = XExtBar(bar)
            doBar(b)
            bars.add(b)
        }
    }

    override fun metrica(params: GustosBotLogicParams, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toDouble(), 50.0, 2.0))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0))
    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return emptyMap()
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
                        trader?.let { it.usd / bar.closePrice } ?: 0.0,
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