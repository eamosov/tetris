package ru.efreet.trading.exchange

import java.io.Serializable

/**
 * Created by fluder on 08/02/2018.
 */
enum class OrderType : Serializable {
    LIMIT,
    MARKET
}