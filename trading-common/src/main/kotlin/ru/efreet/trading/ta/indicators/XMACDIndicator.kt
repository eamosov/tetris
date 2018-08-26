package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 20/02/2018.
 */
class XMACDIndicator<B>(val short: XIndicator, val long: XIndicator) : XIndicator {

    override fun getValue(index: Int): Float {
        return short.getValue(index) - long.getValue(index)
    }
}