package ru.efreet.trading.bot

import ru.efreet.trading.exchange.TradeRecord
import java.io.Serializable
import java.time.ZonedDateTime

/**
 * Created by fluder on 20/02/2018.
 */

data class TradeHistory(val startUsd: Double,
                        val startAsset: Double,
                        val startFunds: Double,
                        val endUsd: Double,
                        val endAsset: Double,
                        val endFunds: Double,
                        val trades: List<TradeRecord>,
                        val indicators: Map<String, List<Pair<ZonedDateTime, Double>>>,
                        val cash: List<Pair<ZonedDateTime, Double>>,

                        val startPrice: Double,
                        val endPrice: Double,
                        val minPrice: Double,
                        val maxPrice: Double,
                        val start: ZonedDateTime,
                        val end: ZonedDateTime) : Serializable {

}

