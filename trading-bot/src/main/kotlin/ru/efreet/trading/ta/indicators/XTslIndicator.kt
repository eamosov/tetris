package ru.efreet.trading.ta.indicators

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 17/03/2018.
 * Индикатор, возвращающий максимальную цену c момента покупки
 */
class XTslIndicator(bars: List<XExtBar>,
                                 prop: BarGetterSetter<XExtBar>,
                                 val xLastTrendIndicator: XLastDecisionIndicator<XExtBar>,
                                 val closePriceIndicator: XClosePriceIndicator) : XCachedIndicator<XExtBar>(bars, prop) {

    override fun calculate(index: Int, bar: XExtBar): Float {
        val cur = xLastTrendIndicator.getValue(index)
        val closePrice = closePriceIndicator.getValue(index)

        if (index == 0)
            return closePrice

        return if (cur.first == Decision.BUY) {
            if (xLastTrendIndicator.getValue(index - 1).first == Decision.SELL) {
                closePrice
            } else {
                maxOf(getValue(index - 1), closePrice)
            }
        } else {
            closePrice
        }
    }
}