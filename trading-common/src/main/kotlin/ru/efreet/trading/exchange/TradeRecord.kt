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
                       val long: Boolean,
                       val type: OrderType,
                       val amount: Double, /*of asset*/
                       val fee: Double = 0.0, /*of asset*/
                       val usdBefore: Double = 0.0,
                       val assetBefore: Double = 0.0,
                       val usdAfter: Double = 0.0,
                       val assetAfter: Double = 0.0,
                       val fundsAfter: Double = 0.0) : Serializable {

}