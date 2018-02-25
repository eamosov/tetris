package ru.efreet.trading.logic.impl.sd

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.ta.indicators.*
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class SdLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    lateinit var closePrice: XClosePriceIndicator
    lateinit var shortEma: XEMAIndicator<XExtBar>
    lateinit var longEma: XEMAIndicator<XExtBar>

    lateinit var sma: XSMAIndicator<XExtBar>
    lateinit var sd: XStandardDeviationIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XEMAIndicator<XExtBar>

    init {
//        add(SimpleBotLogicParams::short, 135, 406*5, 1, false)
//        add(SimpleBotLogicParams::long, 197, 592*5, 1, false)
//        add(SimpleBotLogicParams::signal, 11, 34*5, 1, false)
//        add(SimpleBotLogicParams::deviationTimeFrame, 20, 93*5, 1, false)

//        add(SimpleBotLogicParams::short, 135*5, 406*5, 1, false)
//        add(SimpleBotLogicParams::long, 197*5, 592*5, 1, false)
//        add(SimpleBotLogicParams::signal, 11*5, 34*5, 1, false)
        of(SimpleBotLogicParams::deviation, "logic.sd.deviation", 10, 40, 1, true)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd.deviationTimeFrame", Duration.ofMinutes(30), Duration.ofMinutes(62), Duration.ofMinutes(1), true)

        of(SimpleBotLogicParams::short, "logic.sd.short", Duration.ofHours(13), Duration.ofHours(23), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::long, "logic.sd.long", Duration.ofHours(21), Duration.ofHours(40), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::signal, "logic.sd.signal", Duration.ofHours(14), Duration.ofHours(27), Duration.ofSeconds(1), true)
//        add(SimpleBotLogicParams::deviationTimeFrame, 1, 93*5*60, 1, false)

        //add(SimpleBotLogicParams::mainRation, 10, 100, 1, false)
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams {
        return orig.copy()
    }

    override fun prepare() {

        closePrice = XClosePriceIndicator(bars)
        shortEma = XEMAIndicator(bars, BarGetterSetter({ o, v -> o.shortEma = v }, { it.shortEma }), closePrice, _params.short!!)
        longEma = XEMAIndicator(bars, BarGetterSetter({ o, v -> o.longEma = v }, { it.longEma }), closePrice, _params.long!!)

        sma = XSMAIndicator(bars, BarGetterSetter({ o, v -> o.sma = v }, { it.sma }), closePrice, _params.deviationTimeFrame!!)
        sd = XStandardDeviationIndicator(bars, BarGetterSetter({ o, v -> o.shortEma = v }, { it.shortEma }), closePrice, sma, _params.deviationTimeFrame!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XEMAIndicator(bars, XExtBar._signalEma, macd, _params.signal!!)

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
                Pair("macd", XMinusIndicator<XExtBar>(macd, signalEma)))
    }
}