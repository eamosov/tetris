package ru.efreet.trading.bot

import java.time.ZonedDateTime

data class TradesStats(var trades: Int = 0,
                       var goodTrades: Float = 0.0F,
                       var profit: Float = 0.0F,
                       var profitPerTrade: Float = 0.0F,
                       var sdProfitPerTrade: Float = 0.0F,
                       var sma5: Float = 0.0F,
                       var sma10: Float = 0.0F,
                       var pearson: Float = 0.0F,
                       var start: ZonedDateTime,
                       var end: ZonedDateTime,
                       var profitPerDay: Float = 0.0F,
                       var relProfit: Float = 0.0F
)

data class TradesStatsShort(var trades: Int = 0,
                       var profit: Float = 0.0F,
                       var pearson: Float = 0.0F
)