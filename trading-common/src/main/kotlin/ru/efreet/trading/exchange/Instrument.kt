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

        @JvmStatic
        val BCC_USDT = Instrument("BCC", "USDT")

        @JvmStatic
        val BNB_USDT = Instrument("BNB", "USDT")

        @JvmStatic
        val ETH_USDT = Instrument("ETH", "USDT")

        @JvmStatic
        val LTC_USDT = Instrument("LTC", "USDT")

        @JvmStatic
        val KEY_BTC = Instrument("KEY", "BTC")
        val IOTX_BTC = Instrument("IOTX", "BTC")
        val XLM_BTC = Instrument("XLM", "BTC")
        val BNB_BTC = Instrument("BNB", "BTC")
        val THETA_BTC = Instrument("THETA", "BTC")

        fun parse(name: String): Instrument {
            val n = name.split("_")
            return Instrument(n[0], n[1])
        }
    }
}