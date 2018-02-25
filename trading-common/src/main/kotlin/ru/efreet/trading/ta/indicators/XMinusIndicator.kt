package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 21/02/2018.
 */
class XMinusIndicator<B>(val a: XIndicator<B>,
                         val b: XIndicator<B>) : XIndicator<B> {
    override fun getValue(index: Int, bar: B): Double {
        return a.getValue(index, bar) - b.getValue(index, bar)
    }
}