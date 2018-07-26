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
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.trainer.Metrica
import ru.gustos.trading.global.InstrumentData
import ru.gustos.trading.global.StandardInstrumentCalc
import java.time.Duration

open class GustosBotLogic3(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>, val simulate: Boolean) : AbstractBotLogic<GustosBotLogicParams3>(name, GustosBotLogicParams3::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var calc: StandardInstrumentCalc

    override fun newInitParams(): GustosBotLogicParams3 = GustosBotLogicParams3()


    override fun onInit() {

        of(GustosBotLogicParams3::bet, "bet", 0.1, 1.0, 0.05, true)
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
        }

    }

    override fun insertBar(bar: XBar) {
        synchronized(this) {
            val b = XExtBar(bar)
            bars.add(b)
            if (barsIsPrepared) {
                calc.addBar(b)
                calc.checkNeedRenew(!simulate)
            }
        }
    }

    override fun metrica(params: GustosBotLogicParams3, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toDouble(), 50.0, 2.0))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0))
    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("price", closePrice))
    }

    override fun getBotAdviceImpl(index: Int, trader: Trader?, fillIndicators: Boolean): BotAdvice {

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
                        trader?.let {
                            trader.cancelAllOrders(instrument)
                            trader.updateBalance(true)
                            trader.availableAsset(instrument)
                        } ?: 0.0,
                        bar,
                        indicators)
            }

            if (decision == Decision.BUY) {

                return BotAdvice(bar.endTime,
                        decision,
                        decisionArgs,
                        instrument,
                        bar.closePrice,
                        trader?.let {

                            trader.cancelAllOrders(instrument)
                            trader.updateBalance(true)

                            //сколько всего USD свободно и вложено в монеты, которыми торгует бот
                            val usd = trader.instruments.map { trader.price(it) * trader.availableAsset(it) }.sum() + trader.usd

                            //размер ставки
                            val sum = minOf(trader.usd, usd * getParams().bet)

                            //сколько купить
                            sum / bar.closePrice
                        } ?: 0.0,
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