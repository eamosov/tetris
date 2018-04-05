package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XExtBar
import java.time.ZonedDateTime

/**
 * Created by fluder on 17/03/2018.
 */
class XTrendStartIndicator<B : XExtBar>(bars: List<B>, prop: BarGetterSetter2<B, XExtBar>, val xLastTrendIndicator: XLastTrendIndicator<B>) : XCachedIndicator2<B, XExtBar>(bars, prop) {

    override fun calculate(index: Int, bar: B): XExtBar {

        val l = xLastTrendIndicator.getValue(index, bar)
        if (index == 0)
            return bar

        val p = xLastTrendIndicator.getValue(index - 1, bars[index - 1])
        return if (l != p)
            bar
        else
            getValue(index - 1, bars[index - 1])
    }

    override fun prepare() {
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }
}