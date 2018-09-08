package ru.efreet.trading.logic

import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import kotlin.reflect.KClass

/**
 * Created by fluder on 20/02/2018.
 */
abstract class AbstractXExtBarBotLogic<P : Any>(name: String,
                                                paramsCls: KClass<P>,
                                                instrument: Instrument,
                                                barInterval: BarInterval) : BarsAwareAbstractBotLogic<P, XExtBar>(name, paramsCls, instrument, barInterval) {

    override fun resetBars() {
        for (i in 0 until bars.size) {
            bars[i] = XExtBar(bars[i].bar)
        }
        barsIsPrepared = false
    }


    override fun insertBar(bar: XBar, marketBar: MarketBar?) {
        synchronized(this) {
            bars.add(XExtBar(bar))

            while (bars.size > historyBars) {
                bars.removeAt(0)
            }
        }
    }
}