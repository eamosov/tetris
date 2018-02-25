package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
abstract class AbstractEMAIndicator<B>(bars: List<B>,
                                       prop: BarGetterSetter<B>,
                                       val indicator: XIndicator<B>,
                                       val timeFrame: Int,
                                       val multiplier: Double) : XCachedIndicator<B>(bars, prop) {

    override fun prepare() {
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }

    override fun calculate(index: Int, bar: B): Double {
        if (index == 0) {
            return indicator.getValue(0, bar)
        }
        val prevValue = getValue(index - 1, bars[index - 1])
        return ((indicator.getValue(index, bar) - prevValue) * multiplier) + prevValue
    }
}