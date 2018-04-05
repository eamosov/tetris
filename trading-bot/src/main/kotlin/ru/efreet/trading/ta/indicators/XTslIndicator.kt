package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.OrderSide

/**
 * Created by fluder on 17/03/2018.
 */
class XTslIndicator<B : XExtBar>(bars: List<B>,
                                 prop: BarGetterSetter<B>,
                                 val xLastTrendIndicator: XLastTrendIndicator<B>,
                                 val closePriceIndicator: XClosePriceIndicator) : XCachedIndicator<B>(bars, prop) {

    override fun calculate(index: Int, bar: B): Double {
        val cur = xLastTrendIndicator.getValue(index, bar)
        val closePrice = closePriceIndicator.getValue(index, bar)

        if (index == 0)
            return closePrice

        return if (cur.side == OrderSide.BUY) {
            if (xLastTrendIndicator.getValue(index - 1, bars[index - 1]).side == OrderSide.SELL) {
                closePrice
            } else {
                maxOf(getValue(index - 1, bars[index - 1]), closePrice)
            }
        } else {
            closePrice
        }
    }

    override fun prepare() {
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }
}