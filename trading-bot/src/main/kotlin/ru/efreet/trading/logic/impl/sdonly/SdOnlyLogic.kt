package ru.efreet.trading.logic.impl.sdonly

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.ta.indicators.XClosePriceIndicator
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.ta.indicators.XSMAIndicator
import ru.efreet.trading.ta.indicators.XStandardDeviationIndicator
import java.time.Duration

/**
 * Created by fluder on 20/02/2018.
 */
class SdOnlyLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : AbstractBotLogic<SimpleBotLogicParams>(name, SimpleBotLogicParams::class, instrument, barInterval, bars) {

    val closePrice = XClosePriceIndicator(bars)
    lateinit var sma: XSMAIndicator<XExtBar>
    lateinit var sd: XStandardDeviationIndicator<XExtBar>

    init {
        of(SimpleBotLogicParams::deviation, "logic.sdonly.deviation", 10, 40, 1, true)
        of(SimpleBotLogicParams::deviationTimeFrame, "logic.sdonly.deviationTimeFrame", Duration.ofSeconds(1), Duration.ofMinutes(400), Duration.ofSeconds(1), true)
    }


    override fun copyParams(orig: SimpleBotLogicParams): SimpleBotLogicParams {
        return orig.copy()
    }

    override fun prepare() {

        val sma = XSMAIndicator(bars, XExtBar._sma, closePrice, _params.deviationTimeFrame!!)
        val sd = XStandardDeviationIndicator(bars, XExtBar._sd, closePrice, sma, _params.deviationTimeFrame!!)

        sma.prepare()
        sd.prepare()
    }

    override fun getAdvice(index: Int, bar: XExtBar): OrderSide? {
        val price = closePrice.getValue(index, bar)
        val sma = sma.getValue(index, bar)
        val sd = sd.getValue(index, bar)

        return when {
            price < sma - sd * _params.deviation!! / 10.0 -> OrderSide.BUY
            price > sma + sd * _params.deviation!! / 10.0 -> OrderSide.SELL
            else -> null
        }

    }

    override fun indicators(): Map<String, XIndicator<XExtBar>> {
        return mapOf(Pair("sma", sma),
                Pair("sd", sd),
                Pair("price", closePrice))
    }
}