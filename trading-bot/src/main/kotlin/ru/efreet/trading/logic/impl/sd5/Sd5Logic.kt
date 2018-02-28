package ru.efreet.trading.logic.impl.sd5

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.utils.IntFunction3
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class Sd5Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)

    lateinit var shortEma: XDoubleEMAIndicator<XExtBar>
    lateinit var longEma: XDoubleEMAIndicator<XExtBar>
    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var signalEma: XDoubleEMAIndicator<XExtBar>

    lateinit var sma: XSMAIndicator<XExtBar>
    lateinit var sd: XStandardDeviationIndicator<XExtBar>


    lateinit var dayShortEma: XEMAIndicator<XExtBar>
    lateinit var dayLongEma: XEMAIndicator<XExtBar>
    lateinit var dayMacd: XMACDIndicator<XExtBar>
    lateinit var daySignalEma: XEMAIndicator<XExtBar>

    init {

        of(SimpleBotLogicParams::deviation, "logic.sd4.deviation", 8, 40, 1, true)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd4.deviationTimeFrame", Duration.ofMinutes(20), Duration.ofMinutes(100), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::short, "logic.sd4.short", Duration.ofMinutes(10), Duration.ofMinutes(60), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::long, "logic.sd4.long", Duration.ofMinutes(20), Duration.ofMinutes(160), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::signal, "logic.sd4.signal", Duration.ofMinutes(10), Duration.ofMinutes(300), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::dayShort, "logic.sd4.dayShort", Duration.ofHours(10), Duration.ofHours(20), Duration.ofMinutes(15), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd4.dayLong", Duration.ofHours(15), Duration.ofHours(30), Duration.ofMinutes(15), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd4.daySignal", Duration.ofHours(13), Duration.ofHours(26), Duration.ofMinutes(15), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd4.stopLoss", 0.1, 10.0, 0.5, true)

        of(SimpleBotLogicParams::f3Index, "logic.sd4.f3Index", -1)
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

        dayShortEma = XEMAIndicator(bars, XExtBar._dayShortEma, closePrice, _params.dayShort!!)
        dayLongEma = XEMAIndicator(bars, XExtBar._dayLongEma, closePrice, _params.dayLong!!)
        dayMacd = XMACDIndicator(dayShortEma, dayLongEma)
        daySignalEma = XEMAIndicator(bars, XExtBar._daySignalEma, dayMacd, _params.daySignal!!)

        shortEma.prepare()
        longEma.prepare()
        sma.prepare()
        sd.prepare()
        signalEma.prepare()

        dayShortEma.prepare()
        dayLongEma.prepare()
        daySignalEma.prepare()
    }

//    override fun seedRandom(size: Int): MutableList<SimpleBotLogicParams> {
//        val list = mutableListOf<SimpleBotLogicParams>()
//        for (i in 0 until IntFunction3.size() - 1) {
//            list.addAll(super.seedRandom(size).map { it.f3Index = i; it })
//        }
//        return list
//    }

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)
        val macd = macd.getValue(index, bar)
        val signalEma = signalEma.getValue(index, bar)

        val dayMacd = dayMacd.getValue(index, bar)
        val daySignal = daySignalEma.getValue(index, bar)

        val _sd = when {
            price < sma - sd * _params.deviation!! / 10.0 -> 0
            price > sma + sd * _params.deviation!! / 10.0 -> 1
            else -> 2
        }

        val _macd = when {
            macd > signalEma -> 0
            else -> 1
        }

        val _dayMacd = when {
            dayMacd > daySignal -> 0
            else -> 1
        }

        val f = IntFunction3.get(_params.f3Index!!, _sd, _macd, _dayMacd).toInt()

        return when (f) {
            0 -> OrderSide.BUY
            1 -> OrderSide.SELL
            else -> null
        }
    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("sma", sma),
                Pair("sd", sd),
                Pair("shortEma", shortEma),
                Pair("longEma", longEma),
                Pair("price", closePrice),
                Pair("macd", XMinusIndicator(macd, signalEma)),
                Pair("dayShortEma", dayShortEma),
                Pair("dayLongEma", dayLongEma))
    }

    override var historyBars: Long
        get() = Duration.ofHours(60).toMillis() / barInterval.duration.toMillis()
        set(value) {}

    override var maxBars: Int
        get() = historyBars.toInt()
        set(value) {}


}