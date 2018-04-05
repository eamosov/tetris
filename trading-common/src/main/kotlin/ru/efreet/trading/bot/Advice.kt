package ru.efreet.trading.bot

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import java.time.ZonedDateTime

data class OrderSideExt (val side:OrderSide, val long: Boolean)

/**
 * Created by fluder on 25/02/2018.
 */
data class Advice(val time: ZonedDateTime,
                  val orderSide: OrderSideExt?,
                  val sellBySl: Boolean,
                  val instrument: Instrument,
                  val price: Double,
                  val amount: Double,
                  val bar: XBar,
                  val indicators: Map<String, Double>?)
