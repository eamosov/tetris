package ru.efreet.trading.bot

import java.time.ZonedDateTime

data class TradesStats(var trades: Int = 0,
                       var goodTrades: Double = 0.0,
                       var profit: Double = 0.0,
                       var avrProfitPerTrade: Double = 0.0,
                       var sdProfitPerTrade: Double = 0.0,
                       var sma: Double = 0.0,
                       var firstTrade:ZonedDateTime?,
                       var lastTrade:ZonedDateTime?)