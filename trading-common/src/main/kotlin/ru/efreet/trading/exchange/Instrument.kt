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
        val IOTX_BTC = Instrument("IOTX", "BTC")
        val XLM_BTC = Instrument("XLM", "BTC")
        val BNB_BTC = Instrument("BNB", "BTC")
        val THETA_BTC = Instrument("THETA", "BTC")
        val BCC_USDT = Instrument("BCC", "USDT")
        val BNB_USDT = Instrument("BNB", "USDT")

        fun parse(name: String): Instrument {
            val n = name.split("_")
            return Instrument(n[0], n[1])
        }
    }
}