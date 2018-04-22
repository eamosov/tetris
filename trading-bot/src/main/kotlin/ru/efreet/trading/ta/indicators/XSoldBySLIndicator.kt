package ru.efreet.trading.ta.indicators

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 17/03/2018.
 */
class XSoldBySLIndicator<B : XExtBar>(bars: List<B>,
                                      prop: BarGetterSetter2<B, Boolean>,
                                      val xLastTrendIndicator: XLastTrendIndicator<B>,
                                      val xTslIndicator: XTslIndicator<B>,
                                      val xTrendStartIndicator: XTrendStartIndicator<B>,
                                      val sl: Double,
                                      val tsl: Double,
                                      val tp: Double,
                                      val ttp: Double) : XCachedIndicator2<B, Boolean>(bars, prop) {

    override fun calculate(index: Int, bar: B): Boolean {

        val trend = xLastTrendIndicator.getValue(index, bar)
        if (trend.first == Decision.SELL)
            return false

        val prevValue = if (index > 0) getValue(index - 1, bars[index - 1]) else false
        if (prevValue)
            return prevValue

        //цена покупки
        val buyPrice = xTrendStartIndicator.getValue(index, bar).closePrice

        //если текущая цена опустилась на sl меньше, чем цена покупки, то выход по SL
        if (bar.closePrice < buyPrice * (1.0 - sl / 100.0))
            return true

        //если мы в плюсе
        if (bar.closePrice > buyPrice * 0.9995) {

            val maxPrice = xTslIndicator.getValue(index, bar)

            val _tsl = if (maxPrice > buyPrice * (1.0 + tp / 100.0))
            //если цена достигла tp
                ttp
            else
                tsl

            if (bar.closePrice < maxPrice * (1.0 - _tsl / 100.0))
                return true

        }

        return false
    }

    override fun prepare() {
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }
}