package ru.gustos.trading

import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBarList
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.global.DecisionManager
import ru.gustos.trading.global.InstrumentData
import java.time.Duration

open class GustosBotLogic3(name: String, instrument: Instrument, barInterval: BarInterval, val simulate: Boolean) : AbstractBotLogic<GustosBotLogicParams3, XBar>(name, GustosBotLogicParams3::class, instrument, barInterval) {

    private val log = LoggerFactory.getLogger(GustosBotLogic3::class.java)

    final override val bars: MutableList<XBar> = XBarList(historyBars.toInt())

    val closePrice = XClosePriceIndicator(bars)

    lateinit var calc: DecisionManager

    override fun newInitParams(): GustosBotLogicParams3 = GustosBotLogicParams3()


    override fun onInit() {

    }

    final override val historyBars: Long
        get() = Duration.ofDays(180).toMillis() / barInterval.duration.toMillis()

    override fun copyParams(src: GustosBotLogicParams3): GustosBotLogicParams3 = src.copy()


    override fun prepareBarsImpl() {

        synchronized(this) {
            calc = DecisionManager(null, InstrumentData(null, instrument, bars, null,true,false), 0, false, 0)
            calc.checkNeedRenew(false)

//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
        }

    }

    override fun insertBar(bar: XBar, marketBar: MarketBar?) {
        synchronized(this) {

            if (marketBar == null){
                log.warn("No MarketBar for {}", bar.endTime)
            }

            bars.add(bar)
            if (barsIsPrepared) {
                calc.addBar(bar)
                calc.checkNeedRenew(!simulate)
            }
        }
    }

    override fun resetBars() {
        barsIsPrepared = false
    }

    override fun metrica(params: GustosBotLogicParams3, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toFloat(), 50.0f, 2.0f))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0f))
    }

    override fun indicators(): Map<String, XIndicator> {
        return mapOf(Pair("price", closePrice))
    }

    override fun getBotAdviceImpl(index: Int, fillIndicators: Boolean): BotAdvice {

        synchronized(this) {

            val indicators = if (fillIndicators) getIndicators(index) else null

            val bar = getBar(index)
            val decision = calc.decision()
            val decisionArgs = emptyMap<String, String>()


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