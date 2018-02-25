package ru.efreet.trading.ta.indicators

import ru.efreet.trading.utils.pow2

/**
 * Created by fluder on 20/02/2018.
 */
class XStandardDeviationIndicator<B>(bars: List<B>,
                                     prop: BarGetterSetter<B>,
                                     val indicator: XIndicator<B>,
                                     val smaIndicator: XSMAIndicator<B>,
                                     val timeFrame: Int) : XCachedIndicator<B>(bars, prop) {

    private val variance = XVarianceIndicator(bars, indicator, smaIndicator, timeFrame)

    override fun prepare() {

        var sums2 = DoubleArray(bars.size, { 0.0 })
        for (index in 0 until bars.size) {
            val bar = bars[index]
            sums2[index] = indicator.getValue(index, bar).pow2()
            if (index > 0) {
                sums2[index] += sums2[index - 1]
            }
            if (index >= timeFrame) {
                sums2[index] -= indicator.getValue(index - timeFrame, bars[index - timeFrame]).pow2()
            }
            val realTimeFrame = minOf(timeFrame, index + 1)
            val value = sums2[index] / realTimeFrame // среднее квадратов

            setValue(bar, Math.sqrt(value - smaIndicator.getValue(index, bar).pow2()))
        }

//        for (i in 0 until bars.size)
//            calculate(i)
    }

    override fun calculate(index: Int, bar: B): Double {
        return Math.sqrt(variance.getValue(index, bar))
    }
}