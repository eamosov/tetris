package ru.efreet.trading.bot

import ru.efreet.trading.Decision
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
        fun loadFromJson(path: String): TradeHistory = ru.efreet.trading.utils.loadFromJson(path)
    }

    val profit: Double get() = endFunds / startFunds

    val profitPerDay: Double get() = pow(endFunds / startFunds, (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))

    val profitPerDayToGrow: Double get() = pow((endFunds / startFunds) / (endPrice / startPrice), (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))

    fun profitBeforeExtended(time : ZonedDateTime) : Double {
        val start = startUsd
        var end = start
        for (i in 0..trades.size-1){
            if (trades[i].decision==Decision.BUY){
                if (trades[i].time!!.isAfter(time)) return end/ start
            } else if (trades[i].decision==Decision.SELL){
                end = trades[i].usdAfter!!
                if (trades[i].time!!.isAfter(time)) return end/ start
            }
        }
        return end/ start
    }

    fun profitBefore(time : ZonedDateTime) : Double {
        val start = startUsd
        var end = start
        for (i in 0..trades.size-1){
            if (trades[i].time!!.isAfter(time)) return end/ start
            if (trades[i].decision==Decision.SELL)
                end = trades[i].usdAfter!!

        }
        return end/ start
    }

    fun worstInterval(len : Int) : Double {
        return when {
            trades.size>len -> return (len..trades.size-1).map { trades.get(it).after()/trades.get(it - len).before()}.min()!!
            trades.size>0 -> trades.get(trades.size-1).after()/trades.get(0).before()
            else -> 1.0
        }
    }
}

