package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 21/02/2018.
 */
class XMinusIndicator(val a: XIndicator,
                         val b: XIndicator) : XIndicator {
    override fun getValue(index: Int): Float {
        return a.getValue(index) - b.getValue(index)
    }
}

