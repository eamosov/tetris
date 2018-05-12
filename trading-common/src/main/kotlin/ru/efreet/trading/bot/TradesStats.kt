package ru.efreet.trading.bot

import java.time.ZonedDateTime

data class TradesStats(var trades: Int = 0,
                       var goodTrades: Double = 0.0,
                       var profit: Double = 0.0,
                       var profitPerTrade: Double = 0.0,
                       var sdProfitPerTrade: Double = 0.0,
                       var sma5: Double = 0.0,
                       var sma10: Double = 0.0,
                       var pearson: Double = 0.0,
                       var start: ZonedDateTime,
                       var end: ZonedDateTime,
                       var profitPerDay: Double = 0.0,
                       var relProfit: Double = 0.0
)