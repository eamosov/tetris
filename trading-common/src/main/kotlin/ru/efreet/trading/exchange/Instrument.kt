package ru.efreet.trading.exchange

import java.io.Serializable

/**
 * Created by fluder on 08/02/2018.
 */
data class Instrument(var asset: String?, var base: String?) : Serializable {
    constructor() : this(null, null)

    override fun toString(): String {
        return "${asset}_${base}"
    }

    companion object {
        @JvmStatic
        val BTC_USDT = Instrument("BTC", "USDT")

        fun parse(name: String): Instrument {
            val n = name.split("_")
            return Instrument(n[0], n[1])
        }
    }
}