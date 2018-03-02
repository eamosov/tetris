package ru.efreet.trading.logic.impl.sd3

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.*
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
    lateinit var daySignal2Ema: XEMAIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                short = 50,
                long = 97,
                signal = 108,
                deviationTimeFrame = 10,
                deviation = 10,
                dayShort = 283,
                dayLong = 1749,
                daySignal = 540,
                daySignal2 = 2806,
                stopLoss = 5.72
        )

        of(SimpleBotLogicParams::deviation, "logic.sd3.deviation", 8, 12, 1, false)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sd3.deviationTimeFrame", Duration.ofMinutes(8), Duration.ofMinutes(12), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::short, "logic.sd3.short", Duration.ofMinutes(40), Duration.ofMinutes(60), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::long, "logic.sd3.long", Duration.ofMinutes(77), Duration.ofMinutes(117), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::signal, "logic.sd3.signal", Duration.ofMinutes(86), Duration.ofMinutes(130), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::dayShort, "logic.sd3.dayShort", Duration.ofMinutes(226), Duration.ofMinutes(340), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::dayLong, "logic.sd3.dayLong", Duration.ofMinutes(1399), Duration.ofMinutes(2099), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::daySignal, "logic.sd3.daySignal", Duration.ofMinutes(432), Duration.ofMinutes(648), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::daySignal2, "logic.sd3.daySignal2", Duration.ofMinutes(2244), Duration.ofMinutes(3368), Duration.ofMinutes(1), false)

        of(SimpleBotLogicParams::stopLoss, "logic.sd3.stopLoss", 4.0, 7.0, 0.25, true)
    }

    override fun metrica(stats: TradesStats): Double {
        val hours = Duration.between(stats.start, stats.end).toHours()
        return BotLogic.fine(stats.trades.toDouble(), hours / 6.0, 4.0) + BotLogic.fine(stats.goodTrades, 0.6, 10.0) + BotLogic.fine(stats.sma10, 0.7, 10.0) + BotLogic.fine(stats.profit, 1.0) + stats.profit
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams = orig.copy()

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
        daySignal2Ema = XEMAIndicator(bars, XExtBar._daySignal2Ema, dayMacd, _params.daySignal2!!)

        shortEma.prepare()
        longEma.prepare()
        sma.prepare()
        sd.prepare()
        signalEma.prepare()

        dayShortEma.prepare()
        dayLongEma.prepare()
        daySignalEma.prepare()
        daySignal2Ema.prepare()
    }

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {

        val sd = sd.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val price = closePrice.getValue(index, bar)
        val macd = macd.getValue(index, bar)
        val signalEma = signalEma.getValue(index, bar)

        val dayMacd = dayMacd.getValue(index, bar)
        val daySignal = daySignalEma.getValue(index, bar)
        val daySignal2 = daySignal2Ema.getValue(index, bar)

//        return when {
//            price < sma - sd * _params.deviation!! / 10.0 && macd > signalEma && dayMacd < daySignal -> OrderSide.BUY
//            (price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma) || dayMacd > daySignal -> OrderSide.SELL
//            else -> null
//        }

        if (dayMacd > daySignal2) {
            return OrderSide.BUY
        }else{

            return when {
                price < sma - sd * _params.deviation!! / 10.0 && macd > signalEma && dayMacd < daySignal -> OrderSide.BUY
                price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma -> OrderSide.SELL
                else -> null
            }

        }

//            return when {
//                (dayMacd > daySignal2) -> OrderSide.BUY
//                (dayMacd < daySignal2) -> OrderSide.SELL
//                else -> null
//            }

//        } else {
//
//            return when {
//                price < sma - sd * _params.deviation!! / 10.0 && macd > signalEma -> OrderSide.BUY
//                price > sma + sd * _params.deviation!! / 10.0 && macd < signalEma -> OrderSide.SELL
//                else -> null
//            }
//
//        }


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