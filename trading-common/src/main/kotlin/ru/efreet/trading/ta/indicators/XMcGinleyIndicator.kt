package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 25/04/2018.
 */
class XMcGinleyIndicator<B>(bars: List<B>,
                            prop: BarGetterSetter<B>,
                            val indicator: XIndicator<B>,
                            val timeFrame: Int) : XCachedIndicator<B>(bars, prop) {

    override fun calculate(index: Int, bar: B): Double {

        if (index == 0) {
            return indicator.getValue(0, bar)
        }

        val pMD = getValue(index - 1, bars[index - 1])
        val value = indicator.getValue(index, bar)
        return pMD + (value - pMD) / (0.6 * timeFrame * Math.pow(value / pMD, 4.0))
    }
}