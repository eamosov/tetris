package ru.efreet.trading.ta.indicators

import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 17/03/2018.
 */
class XDecisionStartIndicator(bars: List<XExtBar>, prop: BarGetterSetter2<XExtBar, XExtBar>, val xLastDecisionIndicator: XLastDecisionIndicator<XExtBar>) : XCachedIndicator2<XExtBar, XExtBar>(bars, prop) {

    override fun calculate(index: Int, bar: XExtBar): XExtBar {

        val l = xLastDecisionIndicator.getValue(index)
        if (index == 0)
            return bar

        val p = xLastDecisionIndicator.getValue(index - 1)
        return if (l != p)
            bar
        else
            getValue(index - 1)
    }
}