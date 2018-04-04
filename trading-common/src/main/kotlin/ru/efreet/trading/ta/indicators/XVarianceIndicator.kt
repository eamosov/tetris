package ru.efreet.trading.ta.indicators

import ru.efreet.trading.utils.pow2

/**
 * Created by fluder on 19/02/2018.
 */
class XVarianceIndicator<B>(val bars: List<B>,
                         val indicator: XIndicator<B>,
                         val smaIndicator:XSMAIndicator<B>,
                         val timeFrame: Int) : XIndicator<B> {


    override fun getValue(index: Int, bar:B): Double {

        val st = System.currentTimeMillis()

        val startIndex = Math.max(0, index - timeFrame + 1)
        val numberOfObservations = index - startIndex + 1
        var variance = 0.0
        val average = smaIndicator.getValue(index, bar)
        for (i in startIndex..index) {
            variance += (indicator.getValue(i, bars[i]) - average).pow2()
        }
        variance = variance / numberOfObservations

        //println("XVarianceIndicator.calcValue took ${System.currentTimeMillis() - st}")
        return variance
    }
}