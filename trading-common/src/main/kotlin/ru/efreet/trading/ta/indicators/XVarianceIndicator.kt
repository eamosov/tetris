package ru.efreet.trading.ta.indicators

import ru.efreet.trading.utils.pow2

/**
 * Created by fluder on 19/02/2018.
 */
class XVarianceIndicator<B>(val bars: List<B>,
                         val indicator: XIndicator,
                         val smaIndicator:XIndicator,
                         val timeFrame: Int) : XIndicator {


    override fun getValue(index: Int): Float {

        val startIndex = Math.max(0, index - timeFrame + 1)
        val numberOfObservations = index - startIndex + 1
        val average = smaIndicator.getValue(index)
        var variance = (startIndex..index).sumByDouble { (indicator.getValue(it).toDouble() - average).pow2() }
        variance /= numberOfObservations

        return variance.toFloat()
    }
}