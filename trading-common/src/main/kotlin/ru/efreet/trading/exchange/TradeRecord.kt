package ru.efreet.trading.exchange

import java.io.Serializable
import java.time.ZonedDateTime

/**
 * Created by fluder on 25/02/2018.
 */
data class TradeRecord(val time: ZonedDateTime,
                       val orderId: String,
                       val instrument: Instrument,
                       val price: Double,
                       val side: OrderSide,
                       val type: OrderType,
                       val amount: Double, /*of asset*/
                       val fee: Double = 0.0, /*of asset*/
                       val usdBefore: Double = 0.0,
                       val assetBefore: Double = 0.0,
                       val usdAfter: Double = 0.0,
                       val assetAfter: Double = 0.0,
                       val fundsAfter: Double = 0.0,
                       val long: Boolean = false, /* is it a long BUY?*/
                       var tsl: Double? = null,
                       val sellByTsl: Boolean = false,
                       val sellBySl: Boolean = false
) : Serializable {

}