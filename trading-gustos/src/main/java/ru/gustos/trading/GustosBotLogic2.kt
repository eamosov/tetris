package ru.gustos.trading

import ru.efreet.trading.Decision
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
import ru.gustos.trading.book.indicators.GustosAverageRecurrent
import java.time.Duration

open class GustosBotLogic2(name: String, instrument: Instrument, barInterval: BarInterval) : AbstractXExtBarBotLogic<GustosBotLogicParams>(name, GustosBotLogicParams::class, instrument, barInterval) {

    val closePrice = XClosePriceIndicator(bars)

    var prepared: Boolean = false
    lateinit var garBuy: GustosAverageRecurrent
    lateinit var garSell: GustosAverageRecurrent
    lateinit var lastDecisionIndicator: XLastDecisionIndicator<XExtBar>

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

    override val historyBars: Long
        get() = Duration.ofDays(14).toMillis() / barInterval.duration.toMillis()

    override fun copyParams(src: GustosBotLogicParams): GustosBotLogicParams = src.copy()


    override fun prepareBarsImpl() {


//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        garBuy = GustosAverageRecurrent(getParams().buyWindow!!, getParams().buyVolumeWindow!!, getParams().volumeShort!!, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        bars.forEach { val (sma, sd) = garBuy.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.sma = sma!!.toFloat();it.sd = sd!!.toFloat(); }
        garSell = GustosAverageRecurrent(getParams().sellWindow!!, getParams().sellVolumeWindow!!, getParams().volumeShort!!, getParams().volumePow1!! / 10.0, getParams().volumePow2!! / 10.0)
        bars.forEach { val (sma, sd) = garSell.feed(it.closePrice.toDouble(), it.volume.toDouble()); it.smaSell = sma!!.toFloat();it.sdSell = sd!!.toFloat(); }
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
        val p = pbar.sma - pbar.sd * getParams().buyDiv!! / 10
        return bar.minPrice <= p && bar.maxPrice >= p && bar.closePrice < bar.sma - bar.sd * getParams().buyBoundDiv!! / 10 && !falling(i)
    }

    private fun shouldSell(i: Int): Boolean {
        val bar = getBar(i)
        val p = bar.smaSell + bar.sdSell * getParams().sellDiv!! / 10
        return bar.maxPrice >= p && bar.closePrice > bar.smaSell + bar.sdSell * getParams().sellBoundDiv!! / 10 && !rising(i)

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
            val (sma, sd) = garBuy.feed(bar.closePrice.toDouble(), bar.volume.toDouble())
            b.sma = sma.toFloat()
            b.sd = sd.toFloat()
            val (sma2, sd2) = garSell.feed(bar.closePrice.toDouble(), bar.volume.toDouble())
            b.smaSell = sma2.toFloat()
            b.sdSell = sd2.toFloat()
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

    override fun getBotAdviceImpl(index: Int, fillIndicators: Boolean): BotAdvice {

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


