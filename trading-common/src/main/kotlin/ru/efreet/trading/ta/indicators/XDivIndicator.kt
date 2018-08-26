package ru.efreet.trading.ta.indicators

class XDivIndicator<B>(val a: XIndicator,
                       val b: XIndicator) : XIndicator {
    override fun getValue(index: Int): Float {
        return a.getValue(index)/b.getValue(index)-1
    }
}