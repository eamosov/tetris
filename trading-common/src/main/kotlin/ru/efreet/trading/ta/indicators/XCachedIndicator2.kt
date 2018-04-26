package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
abstract class XCachedIndicator2<B,V>(bars: List<B>, val prop: BarGetterSetter2<B,V>) : XAbstractIndicator2<B,V>(bars) {

    abstract fun calculate(index: Int, bar: B): V

    open fun prepare() {
        for (i in 0 until bars.size)
            getValue(i)
    }

    private fun setValue(bar: B, value: V) {
        prop.set(bar, value)
    }

    private fun getValue(bar: B): V? {
        return prop.get(bar)
    }

    override fun getValue(index: Int): V {
        val bar = bars[index]
        var v = getValue(bar)
        if (v == null) {
            v = calculate(index, bar)
            setValue(bar, v)
        }
        return v!!
    }

}