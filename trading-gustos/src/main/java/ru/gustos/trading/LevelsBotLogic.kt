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
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.ta.indicators.XLastDecisionIndicator
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.book.indicators.GustosVolumeLevel2
import ru.gustos.trading.book.indicators.LevelsTrader
import java.time.Duration

open class LevelsBotLogic(name: String, instrument: Instrument, barInterval: BarInterval) : AbstractXExtBarBotLogic<LevelsLogicParams>(name, LevelsLogicParams::class, instrument, barInterval) {

    val closePrice = XClosePriceIndicator(bars)

    var prepared: Boolean = false
    lateinit var levels: GustosVolumeLevel2
    lateinit var levelsTrade: LevelsTrader
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

    override fun newInitParams(): LevelsLogicParams = LevelsLogicParams()

    init {

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

    override val historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()


    override fun copyParams(src: LevelsLogicParams): LevelsLogicParams = src.copy()


    override fun prepareBars() {
        levels = GustosVolumeLevel2(1 - params.descendK!! * 0.00001, 1 - params.substK!! * 0.001, params.fPow!! * 0.1)
        levelsTrade = LevelsTrader(params.fixTime!!, params.sellSdTimeFrame!!, params.fixAmp!! * 0.0001)


        bars.forEach { doBar(it) }

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

    private fun shouldBuy(i: Int): Boolean {
        val bar = getBar(i)
        val sma = bar.sma//*1.0005
        return bar.sd2 > 0 && bar.minPrice < sma && bar.closePrice > sma && bar.closePrice < sma + bar.sd / 3// && bar.avrVolume2>bar.avrVolume*getParams().lowVolumeK!!*0.1
//        return !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
//        return (bar.closePrice>bar.sma+bar.sd*getParams().diviation!!*0.1 || bar.sd2<0 || (bar.closePrice<bar.sma && bar.isBearish() && bar.volume>bar.avrVolume*getParams().highVolumeK!!*0.1))&& !rising(i)

        return ((bar.closePrice > bar.sma + bar.sd) || bar.sd2 < 0) && !rising(i)

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

    private fun doBar(b: XExtBar) {
        b.sma = levels.feed(b).toFloat()
        levelsTrade.feed(b, b.sma.toDouble());
        b.sd = levelsTrade.sd().toFloat();
        b.sd2 = if (levelsTrade.high()) 1.0f else -1.0f;
        b.avrVolume = levelsTrade.longVolume().toFloat()
        b.avrVolume2 = levelsTrade.shortVolume().toFloat()
        b.stohastic = levelsTrade.stohastic()

    }

    override fun insertBar(bar: XBar, marketBar: MarketBar?) {
        synchronized(this) {
            val b = XExtBar(bar)
            doBar(b)
            bars.add(b)
        }
    }

    override fun metrica(params: LevelsLogicParams, stats: TradesStats): Metrica {

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