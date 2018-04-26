package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
abstract class XCachedIndicator<B>(bars: List<B>, val prop: BarGetterSetter<B>) : XAbstractIndicator<B>(bars) {

    abstract fun calculate(index: Int, bar: B): Double

    open fun prepare() {
        for (i in 0 until bars.size)
            getValue(i)
    }

    protected fun setPropValue(bar: B, value: Double) {
        prop.set(bar, value)
    }

    protected fun getPropValue(bar: B): Double {
        return prop.get(bar)
    }

    override fun getValue(index: Int): Double {
        val bar = bars[index]
        var v = getPropValue(bar)
        if (v == Double.MAX_VALUE) {
            v = calculate(index, bar)
            setPropValue(bar, v)
        }
        return v
    }
}