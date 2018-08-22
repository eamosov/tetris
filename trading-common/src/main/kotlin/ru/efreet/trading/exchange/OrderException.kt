package ru.efreet.trading.exchange

import ru.efreet.trading.Decision

data class OrderException(val instrument: Instrument, val asset: Double, val price: Double, val type: OrderType, val side: Decision, override val cause: Throwable) : Exception(cause) {
    override fun toString(): String {
        return "OrderException(instrument=$instrument, asset=$asset, price=$price, type=$type, side=$side, cause=${cause.message})"
    }
}