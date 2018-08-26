package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
abstract class XCachedIndicator<B>(bars: List<B>, val prop: BarGetterSetter<B>) : XAbstractIndicator<B>(bars) {

    abstract fun calculate(index: Int, bar: B): Float

    open fun prepare() {
        for (i in 0 until bars.size)
            getValue(i)
    }

    protected fun setPropValue(bar: B, value: Float) {
        prop.set(bar, value)
    }

    protected fun getPropValue(bar: B): Float {
        return prop.get(bar)
    }

    override fun getValue(index: Int): Float {
        val bar = bars[index]
        var v = getPropValue(bar)
        if (v == Float.MAX_VALUE) {
            v = calculate(index, bar)
            setPropValue(bar, v)
        }
        return v
    }
}