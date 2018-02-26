package ru.efreet.trading.logic.impl.sd3

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.utils.BooleanFunction3
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class Sd3Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

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
        /**
         * Best strategy: TrainItem(args=SimpleBotLogicParams(short=59, long=74, signal=35, deviationTimeFrame=48, deviation=26, dayShort=1408, dayLong=842, daySignal=1166, stopLoss=9.314348758746558), result=TradesStats(trades=118, goodTrades=0.8389830508474576, profit=5.538901702193952, avrProfitPerTrade=1.0171860580536856, sdProfitPerTrade=0.028404997651252876, sma=0.9915254237288136, profitStats=null))
        {
        "short": 59,
        "long": 74,
        "signal": 35,
        "deviationTimeFrame": 48,
        "deviation": 26,
        "dayShort": 1408,
        "dayLong": 842,
        "daySignal": 1166,
        "stopLoss": 9.314348758746558
        }
         */

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 8, 40, 1, true)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(20), Duration.ofMinutes(100), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(10), Duration.ofMinutes(60), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(20), Duration.ofMinutes(160), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(10), Duration.ofMinutes(300), Duration.ofSeconds(1), true)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofHours(10), Duration.ofHours(20), Duration.ofMinutes(15), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofHours(15), Duration.ofHours(30), Duration.ofMinutes(15), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofHours(13), Duration.ofHours(26), Duration.ofMinutes(15), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 0.1, 10.0, 0.5, true)
//        add(SimpleBotLogicParams::deviationTimeFrame, 1, 93*5*60, 1, false)

        //add(SimpleBotLogicParams::mainRation, 10, 100, 1, false)
    }

    override fun metrica(stats: TradesStats): Double {
        //foo(stats.sma, 0.8) +

        return foo(stats.trades.toDouble(), 20.0, 4.0) + /*foo(stats.goodTrades, 1.3, 5.0)*/ foo(stats.sma5, 1.0, 5.0) + foo(stats.profit, 1.0) + stats.profit
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

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)
        val macd = macd.getValue(index, bar)
        val signalEma = signalEma.getValue(index, bar)

        val dayMacd = dayMacd.getValue(index, bar)
        val daySignal = daySignalEma.getValue(index, bar)

        return when {
            BooleanFunction3.get(_params.f3Index!!, true, price < sma - sd * _params.deviation!! / 10.0, macd > signalEma, dayMacd > daySignal) -> OrderSide.BUY
            BooleanFunction3.get(_params.f3Index!!, false, price > sma + sd * _params.deviation!! / 10.0, macd > signalEma, dayMacd > daySignal) -> OrderSide.SELL
            else -> null
        }

//        return when {
//            price < sma - sd * _params.deviation!! / 10.0 && macd > signalEma && dayMacd > daySignal -> OrderSide.BUY
//            (price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma) || dayMacd < daySignal -> OrderSide.SELL
//            else -> null
//        }

    }

    override fun seedRandom(size: Int): MutableList<SimpleBotLogicParams> {

        val ret = mutableListOf<SimpleBotLogicParams>()

        for (i in 0 until BooleanFunction3.size()) {
            ret.addAll(super.seedRandom(size).map { it.f3Index = i; it })
        }

        return ret;
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