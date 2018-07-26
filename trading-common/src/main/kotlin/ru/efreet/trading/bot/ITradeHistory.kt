package ru.efreet.trading.bot

import ru.efreet.trading.exchange.TradeRecord
import java.time.ZonedDateTime

data class ITradeHistory(
        val trades: MutableList<TradeRecord> = mutableListOf(),
        val indicators: MutableMap<String, MutableList<Pair<ZonedDateTime, Double>>> = mutableMapOf(),
        var startAsset: Double = 0.0,
        var asset: Double = 0.0) {

    var startPrice: Double = 0.0
    var minPrice: Double = Double.MAX_VALUE
    var maxPrice: Double = 0.0
    var closePrice: Double = 0.0

    lateinit var startTime: ZonedDateTime
    lateinit var endTime: ZonedDateTime

    fun isStartInitialized(): Boolean {
        return this::startTime.isInitialized
    }
}