package ru.efreet.trading.ta.indicators

class XPlusKIndicator<B>(val a: XIndicator<B>,
                         val b: XIndicator<B>,
                         val k: Double) : XIndicator<B> {
    override fun getValue(index: Int): Double {
        return a.getValue(index) + k*b.getValue(index)
    }
}