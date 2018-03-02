package ru.efreet.trading.logic.impl.macd

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XEMAIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.ta.indicators.XMACDIndicator
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class MacdLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    lateinit var closePrice: XClosePriceIndicator
    lateinit var shortEma: XEMAIndicator<XExtBar>
    lateinit var longEma: XEMAIndicator<XExtBar>

    lateinit var macd: XMACDIndicator<XExtBar>
    lateinit var emaMacd: XEMAIndicator<XExtBar>

    init {
        _params = SimpleBotLogicParams(
                short = 322,
                long = 1414,
                signal = 2806,
                stopLoss = 6.75,
                persist = 5
        )
        of(SimpleBotLogicParams::short, "logic.macd.short", Duration.ofMinutes(257), Duration.ofMinutes(387), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::long, "logic.macd.long", Duration.ofMinutes(1131), Duration.ofMinutes(1697), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::signal, "logic.macd.signal", Duration.ofMinutes(2244), Duration.ofMinutes(3368), Duration.ofMinutes(1), false)
        of(SimpleBotLogicParams::persist, "logic.macd.persist", 4, 6, 1, false)
        of(SimpleBotLogicParams::stopLoss, "logic.macd.stopLoss", 5.25, 8.25, 0.25, true)
    }

    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams {
        return orig.copy()
    }

    override fun prepare() {

        closePrice = XClosePriceIndicator(bars)
        shortEma = XEMAIndicator(bars, XExtBar._shortEma, closePrice, _params.short!!)
        longEma = XEMAIndicator(bars, XExtBar._longEma, closePrice, _params.long!!)

        macd = XMACDIndicator(shortEma, longEma)
        emaMacd = XEMAIndicator(bars, XExtBar._signalEma, macd, _params.signal!!)

        shortEma.prepare()
        longEma.prepare()
        emaMacd.prepare()
    }

    fun upTrend(index: Int, bar: XExtBar): Boolean {
        //println("uptrend: $index")
        return macd.getValue(index, bars[index]) > emaMacd.getValue(index, bar)
    }

    fun downTrend(index: Int, bar: XExtBar): Boolean {
        return macd.getValue(index, bars[index]) < emaMacd.getValue(index, bar)
    }

    fun upTrend(index: Int, bar: XExtBar, period: Int): Boolean {
        //println("all uptrend: $index $period")
        return (maxOf(0, index - period + 1)..index).all { upTrend(it, bar) }
    }

    fun downTrend(index: Int, bar: XExtBar, period: Int): Boolean {
        return (maxOf(0, index - period + 1)..index).all { downTrend(it, bar) }
    }

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {
        return when {
            upTrend(index, bar, _params.persist!!) -> OrderSide.BUY
            downTrend(index, bar, _params.persist!!) -> OrderSide.SELL
            else -> null
        }
    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("price", closePrice), Pair("shortEma", shortEma), Pair("longEma", longEma))
    }

}