package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bot.OrderSideExt
import ru.efreet.trading.exchange.OrderSide

/**
 * Created by fluder on 17/03/2018.
 */
class XLastTrendIndicator<B>(bars: List<B>, prop: BarGetterSetter2<B, OrderSideExt>, val trend: (index: Int, bar: B) -> OrderSideExt?) : XCachedIndicator2<B, OrderSideExt>(bars, prop) {

    override fun calculate(index: Int, bar: B): OrderSideExt {

        val s = trend(index, bar)
        return if (s != null)
            s
        else if (index == 0)
            OrderSideExt(OrderSide.SELL, false)
        else
            getValue(index - 1, bars[index - 1])

    }

    override fun prepare() {
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }
}