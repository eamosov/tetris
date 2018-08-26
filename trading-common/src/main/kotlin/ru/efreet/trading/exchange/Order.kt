package ru.efreet.trading.exchange

import ru.efreet.trading.Decision
import java.time.ZonedDateTime

data class Order(val orderId: String, val instrument: Instrument, val price: Float, val asset: Float, val type: OrderType, val side: Decision, val time: ZonedDateTime) {}