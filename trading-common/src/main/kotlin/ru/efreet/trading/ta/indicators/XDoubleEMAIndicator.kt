package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 22/02/2018.
 */
class XDoubleEMAIndicator<B>(bars: List<B>,
                             emaProp: BarGetterSetter<B>,
                             emaEmaProp: BarGetterSetter<B>,
                             demaProp: BarGetterSetter<B>,
                             indicator: XIndicator<B>,
                             timeFrame: Int) : XCachedIndicator<B>(bars, demaProp) {

    val ema = XEMAIndicator<B>(bars, emaProp, indicator, timeFrame)
    val emaEma = XEMAIndicator<B>(bars, emaEmaProp, ema, timeFrame)

    override fun calculate(index: Int, bar: B): Double {
        return ema.getValue(index, bar) * 2 - emaEma.getValue(index, bar)
    }

    override fun prepare() {
        ema.prepare()
        emaEma.prepare()
        for (i in 0 until bars.size)
            getValue(i, bars[i])
    }
}