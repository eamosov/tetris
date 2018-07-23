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
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.global.InstrumentData
import ru.gustos.trading.global.StandardInstrumentCalc
import java.time.Duration

open class GustosBotLogic3(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<GustosBotLogicParams3>(name, GustosBotLogicParams3::class, instrument, barInterval, bars) {

    var prepared: Boolean = false

    lateinit var calc : StandardInstrumentCalc

    override fun newInitParams(): GustosBotLogicParams3 = GustosBotLogicParams3()

    override fun onInit() {

    }

    override var historyBars: Long
        get() = Duration.ofDays(120).toMillis() / barInterval.duration.toMillis()
        set(value) {}


    override fun copyParams(src: GustosBotLogicParams3): GustosBotLogicParams3 = src.copy()


    override fun prepareBarsImpl() {

        synchronized(this) {
            calc = StandardInstrumentCalc(InstrumentData(null, instrument, bars, null))
            calc.checkNeedRenew(false)

//        println("timeframe1 ${_params.buyWindow} timeframe2 ${_params.buyVolumeWindow} bars ${bars.size}")
            prepared = true
        }

    }

    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val b = XExtBar(bar)
            bars.add(b)
            calc.addBar(b)
            calc.checkNeedRenew(true)
        }
    }

    override fun metrica(params: GustosBotLogicParams3, stats: TradesStats): Metrica {

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
            val decision = calc.decision()
            val decisionArgs = emptyMap<String,String>()


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
                        null)
            }

            if (decision == Decision.BUY) {
                return BotAdvice(bar.endTime,
                        decision,
                        decisionArgs,
                        instrument,
                        bar.closePrice,
                        trader?.let { it.availableUsd(instrument) / bar.closePrice } ?: 0.0,
                        bar,
                        null)
            }

            return BotAdvice(bar.endTime,
                    Decision.NONE,
                    decisionArgs,
                    instrument,
                    bar.closePrice,
                    0.0,
                    bar,
                    null)
        }

    }


}