package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 19/02/2018.
 */
class XClosePriceIndicator(bars: List<XExtBar>) : XAbstractIndicator<XExtBar>(bars) {

    override fun getValue(index: Int, bar: XExtBar): Double {
        return bar.closePrice
    }
}