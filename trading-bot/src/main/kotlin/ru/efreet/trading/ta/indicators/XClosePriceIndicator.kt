package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 19/02/2018.
 */
class XClosePriceIndicator(bars: List<XBar>) : XAbstractIndicator<XBar>(bars) {

    override fun getValue(index: Int): Float {
        return bars[index].closePrice
    }
}