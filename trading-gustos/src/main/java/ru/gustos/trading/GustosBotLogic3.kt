package ru.gustos.trading

import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.global.DecisionManager
import ru.gustos.trading.global.InstrumentData
import java.time.Duration

open class GustosBotLogic3(name: String, instrument: Instrument, barInterval: BarInterval, val simulate: Boolean) : AbstractBotLogic<GustosBotLogicParams3>(name, GustosBotLogicParams3::class, instrument, barInterval) {

    private val log = LoggerFactory.getLogger(GustosBotLogic3::class.java)

    override var params: GustosBotLogicParams3 = newInitParams()

    lateinit var calc: DecisionManager

    final override fun newInitParams(): GustosBotLogicParams3 = GustosBotLogicParams3()

    final override val historyBars: Long
        get() = Duration.ofDays(180).toMillis() / barInterval.duration.toMillis()

    override fun copyParams(src: GustosBotLogicParams3): GustosBotLogicParams3 = src.copy()

    override fun setHistory(bars: List<XBar>, marketBars: List<MarketBar>?) {
        calc = DecisionManager(null, null, InstrumentData(null, instrument, bars, marketBars, true, false), 0, false, 0)
        calc.checkNeedRenew(false)
    }

    override fun metrica(params: GustosBotLogicParams3, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toFloat(), 50.0f, 2.0f))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0f))
    }

    override fun getAdvice(nextBar: XBar, nextMarketBar: MarketBar?): BotAdvice {

        if (nextMarketBar == null) {
            log.warn("No MarketBar for {}", nextBar.endTime)
        }

        calc.addBar(nextBar, nextMarketBar)
        calc.checkNeedRenew(!simulate)

        val indicators = listOf("price" to nextBar.closePrice).toMap()

        val decision = calc.decision()
        val decisionArgs = emptyMap<String, String>()


        //Если SELL, то безусловно продаем
        if (decision == Decision.SELL) {

            //println("${bar.endTime} SELL ${bar.closePrice}")


            return BotAdvice(nextBar.endTime,
                    decision,
                    decisionArgs,
                    instrument,
                    nextBar.closePrice,
                    nextBar,
                    indicators)
        }

        if (decision == Decision.BUY) {

            return BotAdvice(nextBar.endTime,
                    decision,
                    decisionArgs,
                    instrument,
                    nextBar.closePrice,
                    nextBar,
                    indicators)
        }

        return BotAdvice(nextBar.endTime,
                Decision.NONE,
                decisionArgs,
                instrument,
                nextBar.closePrice,
                nextBar,
                indicators)


    }


}