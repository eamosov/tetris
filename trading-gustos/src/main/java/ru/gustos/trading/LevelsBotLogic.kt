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
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.ta.indicators.XLastDecisionIndicator
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import ru.gustos.trading.book.indicators.GustosVolumeLevel
import ru.gustos.trading.book.indicators.GustosVolumeLevel2
import ru.gustos.trading.book.indicators.LevelsTrader
import java.time.Duration

open class LevelsBotLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<LevelsLogicParams>(name, LevelsLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    var prepared: Boolean = false
    lateinit var levels: GustosVolumeLevel2
    lateinit var levelsTrade : LevelsTrader
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    override fun newInitParams(): LevelsLogicParams = LevelsLogicParams()

    override fun onInit() {

        of(LevelsLogicParams::descendK, "descendK", 1, 100, 1, true)
        of(LevelsLogicParams::substK, "substK", 1, 200, 1, true)
        of(LevelsLogicParams::fPow, "fPow", 0, 20, 1, true)
        of(LevelsLogicParams::fixTime, "fixTime", 10, 1000, 1, false)
        of(LevelsLogicParams::fixAmp, "fixAmp", 1, 100, 1, false)
        of(LevelsLogicParams::sellSdTimeFrame, "sellSdTimeFrame", 1, 300, 1, false)
        of(LevelsLogicParams::diviation, "diviation", 1, 100, 1, false)
        of(LevelsLogicParams::lowVolumeK, "lowVolumeK", 1, 100, 1, false)
        of(LevelsLogicParams::highVolumeK, "highVolumeK", 1, 100, 1, false)

    }

    override var historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: LevelsLogicParams): LevelsLogicParams = src.copy()


    override fun prepareBarsImpl() {
        levels = GustosVolumeLevel2(1-getParams().descendK!!*0.00001,1-getParams().substK!!*0.001,getParams().fPow!!*0.1)
        levelsTrade = LevelsTrader(getParams().fixTime!!,getParams().sellSdTimeFrame!!,getParams().fixAmp!!*0.0001)


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
        val bar = getBar(i)
        val sma = bar.sma//*1.0005
        return bar.sd2>0 && bar.minPrice< sma && bar.closePrice> sma && bar.closePrice<sma+bar.sd/3// && bar.avrVolume2>bar.avrVolume*getParams().lowVolumeK!!*0.1
//        return !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
//        return (bar.closePrice>bar.sma+bar.sd*getParams().diviation!!*0.1 || bar.sd2<0 || (bar.closePrice<bar.sma && bar.isBearish() && bar.volume>bar.avrVolume*getParams().highVolumeK!!*0.1))&& !rising(i)

        return ((bar.closePrice>bar.sma+bar.sd) || bar.sd2<0)&& !rising(i)

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

    private fun doBar(b : XExtBar){
        b.sma = levels.feed(b)
        levelsTrade.feed(b,b.sma);
        b.sd = levelsTrade.sd();
        b.sd2 = if (levelsTrade.high())  1.0 else -1.0;
        b.avrVolume = levelsTrade.longVolume()
        b.avrVolume2 = levelsTrade.shortVolume()
        b.stohastic = levelsTrade.stohastic()

    }

    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val b = XExtBar(bar)
            doBar(b)
            bars.add(b)
        }
    }

    override fun metrica(params: LevelsLogicParams, stats: TradesStats): Metrica {

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