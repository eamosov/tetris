package ru.efreet.trading.ta.indicators

class XDivIndicator<B>(val a: XIndicator<B>,
                       val b: XIndicator<B>) : XIndicator<B> {
    override fun getValue(index: Int): Double {
        return a.getValue(index)/b.getValue(index)-1
    }
}