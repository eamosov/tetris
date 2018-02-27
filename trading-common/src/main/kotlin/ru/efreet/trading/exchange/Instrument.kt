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

        fun parse(name: String): Instrument {
            val n = name.split("_")
            return Instrument(n[0], n[1])
        }
    }
}