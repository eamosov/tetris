package ru.efreet.trading.logic.impl.sd2

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.ta.indicators.*
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class Sd2Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XDoubleEMAIndicator<XExtBar>
    lateinit var longEma: XDoubleEMAIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XDoubleEMAIndicator<XExtBar>

    lateinit var sma: XSMAIndicator<XExtBar>
    lateinit var sd: XStandardDeviationIndicator<XExtBar>

    init {
//        add(SimpleBotLogicParams::short, 135, 406*5, 1, false)
//        add(SimpleBotLogicParams::long, 197, 592*5, 1, false)
//        add(SimpleBotLogicParams::signal, 11, 34*5, 1, false)
//        add(SimpleBotLogicParams::deviationTimeFrame, 20, 93*5, 1, false)

//        add(SimpleBotLogicParams::short, 135*5, 406*5, 1, false)
//        add(SimpleBotLogicParams::long, 197*5, 592*5, 1, false)
//        add(SimpleBotLogicParams::signal, 11*5, 34*5, 1, false)


        of(SimpleBotLogicParams::deviation, "logic.sd2.deviation", 8, 40, 1, true)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd2.deviationTimeFrame", Duration.ofMinutes(20), Duration.ofMinutes(100), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::short, "logic.sd2.short", Duration.ofMinutes(10), Duration.ofMinutes(60), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::long, "logic.sd2.long", Duration.ofMinutes(20), Duration.ofMinutes(160), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::signal, "logic.sd2.signal", Duration.ofMinutes(10), Duration.ofMinutes(300), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::stopLoss, "logic.sd2.stopLoss", 0.1, 10.0, 0.5, true)
//        add(SimpleBotLogicParams::deviationTimeFrame, 1, 93*5*60, 1, false)

        //add(SimpleBotLogicParams::mainRation, 10, 100, 1, false)
    }

    override fun metrica(stats: TradesStats): Double {
        return /*foo(stats.sma, 0.8) +*/ foo(stats.trades.toDouble(), 20.0, 4.0) + foo(stats.goodTrades, 1.3, 5.0) + foo(stats.profit, 1.0) + stats.profit

    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams {
        return orig.copy()
    }

    override fun prepare() {

        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, _params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, _params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, _params.signal!!)

        sma = XSMAIndicator(bars, XExtBar._sma, closePrice, _params.deviationTimeFrame!!)
        sd = XStandardDeviationIndicator(bars, XExtBar._sd, closePrice, sma, _params.deviationTimeFrame!!)

        shortEma.prepare()
        longEma.prepare()
        sma.prepare()
        sd.prepare()
        signalEma.prepare()
    }

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)
        val macd = macd.getValue(index, bar)
        val signalEma = signalEma.getValue(index, bar)

        return when {
            price < sma - sd * _params.deviation!! / 10.0 && macd > signalEma -> OrderSide.BUY
            price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma -> OrderSide.SELL
            else -> null
        }

    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("sma", sma),
                Pair("sd", sd),
                Pair("shortEma", shortEma),
                Pair("longEma", longEma),
                Pair("price", closePrice),
                Pair("macd", XMinusIndicator(macd, signalEma)))
    }

    override var historyBars: Long
        get() = Duration.ofMinutes(200).toMillis() / barInterval.duration.toMillis()
        set(value) {}

}