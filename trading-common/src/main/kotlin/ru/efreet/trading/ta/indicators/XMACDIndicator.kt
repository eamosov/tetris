package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 20/02/2018.
 */
class XMACDIndicator<B>(val short: XIndicator<B>, val long: XIndicator<B>) : XIndicator<B> {

    override fun getValue(index: Int, bar:B): Double {
        return short.getValue(index, bar) - long.getValue(index, bar)
    }
}