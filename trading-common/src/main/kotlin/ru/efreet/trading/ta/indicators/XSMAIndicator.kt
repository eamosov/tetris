package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
class XSMAIndicator<B>(bars: List<B>,
                       prop: BarGetterSetter<B>,
                       val indicator: XIndicator<B>,
                       val timeFrame: Int) : XCachedIndicator<B>(bars, prop) {

    override fun prepare() {

        var sums = DoubleArray(bars.size, { 0.0 })
        for (index in 0 until bars.size) {
            sums[index] = indicator.getValue(index, bars[index])
            if (index > 0) {
                sums[index] += sums[index - 1]
            }
            if (index >= timeFrame) {
                sums[index] -= indicator.getValue(index - timeFrame, bars[index - timeFrame])
            }
            val realTimeFrame = Math.min(timeFrame, index + 1)
            val value = sums[index] / realTimeFrame
            setValue(bars[index], value)
        }
    }

    override fun calculate(index: Int, bar: B): Double {
        var sum = 0.0
        for (i in Math.max(0, index - timeFrame + 1)..index) {
            sum += indicator.getValue(i, bars[i])
        }

        val realTimeFrame = Math.min(timeFrame, index + 1)
        return sum / realTimeFrame
    }
}