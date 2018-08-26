package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
abstract class AbstractEMAIndicator<B>(bars: List<B>,
                                       prop: BarGetterSetter<B>,
                                       val indicator: XIndicator,
                                       val timeFrame: Int,
                                       private val multiplier: Float) : XCachedIndicator<B>(bars, prop) {

    override fun calculate(index: Int, bar: B): Float {
        if (index == 0) {
            return indicator.getValue(0)
        }
        val prevValue = getValue(index - 1)
        return ((indicator.getValue(index) - prevValue) * multiplier) + prevValue
    }

    override fun prepare() {
        if (bars.isEmpty())
            return

        var ema = indicator.getValue(0)
        setPropValue(bars[0], ema)

        for (i in 1 until bars.size) {
            ema += ((indicator.getValue(i) - ema) * multiplier)
            setPropValue(bars[i], ema)
        }

    }
}