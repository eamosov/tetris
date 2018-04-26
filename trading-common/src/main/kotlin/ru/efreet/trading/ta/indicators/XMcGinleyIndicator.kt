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
            return indicator.getValue(0)
        }

        val md = getValue(index - 1)
        val value = indicator.getValue(index)
        return md + (value - md) / (0.6 * timeFrame * Math.pow(value / md, 4.0))
    }

    override fun prepare() {
        if (bars.isEmpty())
            return

        var md = indicator.getValue(0)
        setPropValue(bars[0], md)

        for (i in 1 until bars.size){
            val value = indicator.getValue(i)
            md += (value - md) / (0.6 * timeFrame * Math.pow(value / md, 4.0))
            setPropValue(bars[i], md)
        }
    }

}