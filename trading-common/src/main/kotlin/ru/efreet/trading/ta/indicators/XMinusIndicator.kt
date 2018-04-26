package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 21/02/2018.
 */
class XMinusIndicator<B>(val a: XIndicator<B>,
                         val b: XIndicator<B>) : XIndicator<B> {
    override fun getValue(index: Int): Double {
        return a.getValue(index) - b.getValue(index)
    }
}

