package ru.efreet.trading.ta.indicators

class XPlusKIndicator<B>(val a: XIndicator,
                         val b: XIndicator,
                         val k: Float) : XIndicator {
    override fun getValue(index: Int): Float {
        return a.getValue(index) + k*b.getValue(index)
    }
}