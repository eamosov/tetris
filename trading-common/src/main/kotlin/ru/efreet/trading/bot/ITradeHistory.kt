package ru.efreet.trading.bot

import ru.efreet.trading.exchange.TradeRecord
import java.time.ZonedDateTime

data class ITradeHistory(
        val trades: MutableList<TradeRecord> = mutableListOf(),
        val indicators: MutableMap<String, MutableList<Pair<ZonedDateTime, Float>>> = mutableMapOf(),
        var startAsset: Float = 0.0F,
        var asset: Float = 0.0F) {

    var startPrice: Float = 0.0F
    var minPrice: Float = Float.MAX_VALUE
    var maxPrice: Float = 0.0F
    var closePrice: Float = 0.0F

    lateinit var startTime: ZonedDateTime
    lateinit var endTime: ZonedDateTime

    fun isStartInitialized(): Boolean {
        return this::startTime.isInitialized
    }
}