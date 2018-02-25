package ru.efreet.trading.logic.impl.macd

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.bars.XExtBar
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
//        add(SimpleBotLogicParams::short, 135, 406, 1, false)
//        add(SimpleBotLogicParams::long, 197, 592, 1, false)
//        add(SimpleBotLogicParams::signal, 11, 34, 1, false)

        of(SimpleBotLogicParams::short, "logic.macd.short", Duration.ofSeconds(1), Duration.ofDays(14), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::long, "logic.macd.long", Duration.ofSeconds(1), Duration.ofDays(14), Duration.ofSeconds(1), true)
        of(SimpleBotLogicParams::signal, "logic.macd.signal", Duration.ofSeconds(1), Duration.ofDays(14), Duration.ofSeconds(1), true)
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

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {
        return when {
            macd.getValue(index, bar) > emaMacd.getValue(index, bar) -> OrderSide.BUY
            else -> OrderSide.SELL
        }
    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("price", closePrice), Pair("shortEma", shortEma), Pair("longEma", longEma))
    }

}