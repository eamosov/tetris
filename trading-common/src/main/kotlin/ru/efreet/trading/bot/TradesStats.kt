package ru.efreet.trading.bot

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import java.time.ZonedDateTime

data class TradesStats(var trades: Int = 0,
                       var goodTrades: Double = 0.0,
                       var profit: Double = 0.0,
                       var avrProfitPerTrade: Double = 0.0,
                       var sdProfitPerTrade: Double = 0.0,
                       var sma5: Double = 0.0,
                       var sma10: Double = 0.0,
                       var pearson:Double = 0.0,
                       var start: ZonedDateTime,
                       var end: ZonedDateTime
)