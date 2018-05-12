package ru.efreet.trading.bot

import ru.efreet.trading.exchange.TradeRecord
import java.io.Serializable
import java.lang.Math.pow
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

    companion object {

        @JvmStatic
        fun loadFromJson(path: String): TradeHistory {
            return ru.efreet.trading.utils.loadFromJson(path)
        }
    }

    fun profitPerDay(): Double {
        return pow(endUsd / startUsd, (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))
    }

    fun profitPerDayToGrow(): Double {
        return pow((endUsd / startUsd) / (endPrice / startPrice), (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))
    }
}

