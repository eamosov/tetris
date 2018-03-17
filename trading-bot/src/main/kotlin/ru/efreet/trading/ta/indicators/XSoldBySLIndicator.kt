package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.OrderSide

/**
 * Created by fluder on 17/03/2018.
 */
class XSoldBySLIndicator<B : XExtBar>(bars: List<B>,
                                      prop: BarGetterSetter2<B,Boolean>,
                                      val xLastTrendIndicator: XLastTrendIndicator<B>,
                                      val xTslIndicator: XTslIndicator<B>,
                                      val xTrendStartIndicator: XTrendStartIndicator<B>,
                                      val sl: Double,
                                      val tsl: Double) : XCachedIndicator2<B, Boolean>(bars, prop) {

    override fun calculate(index: Int, bar: B): Boolean {

        val trend = xLastTrendIndicator.getValue(index, bar)
        if (trend.side == OrderSide.SELL)
            return false

        val prevValue = if (index > 0) getValue(index - 1, bars[index - 1]) else false
        if (prevValue == true)
            return prevValue

        if (bar.closePrice < xTrendStartIndicator.getValue(index, bar).closePrice * (1.0 - sl / 100.0))
            return true

        if (bar.closePrice < xTslIndicator.getValue(index, bar) * (1.0 - tsl / 100.0))
            return true

        return false
    }

    override fun prepare() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}