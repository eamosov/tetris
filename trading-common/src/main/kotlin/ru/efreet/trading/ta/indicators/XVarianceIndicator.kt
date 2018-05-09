package ru.efreet.trading.ta.indicators

import ru.efreet.trading.utils.pow2

/**
 * Created by fluder on 19/02/2018.
 */
class XVarianceIndicator<B>(val bars: List<B>,
                         val indicator: XIndicator<B>,
                         val smaIndicator:XIndicator<B>,
                         val timeFrame: Int) : XIndicator<B> {


    override fun getValue(index: Int): Double {

        val startIndex = Math.max(0, index - timeFrame + 1)
        val numberOfObservations = index - startIndex + 1
        val average = smaIndicator.getValue(index)
        var variance = (startIndex..index).sumByDouble { (indicator.getValue(it) - average).pow2() }
        variance /= numberOfObservations

        return variance
    }
}