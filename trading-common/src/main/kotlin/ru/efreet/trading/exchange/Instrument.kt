package ru.efreet.trading.exchange

/**
 * Created by fluder on 08/02/2018.
 */
data class Instrument(var asset: String?, var base: String?) {
    constructor() : this(null, null)

    override fun toString(): String {
        return "${asset}_${base}"
    }

    companion object {
        val BTC_USDT = Instrument("BTC", "USDT")
    }
}