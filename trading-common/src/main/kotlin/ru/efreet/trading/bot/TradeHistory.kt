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

    fun profitBeforeExtended(time: ZonedDateTime): Double {
        val start = startUsd
        var end = start
        for (i in 0 until trades.size) {
            if (trades[i].decision == Decision.BUY) {
                if (trades[i].time!!.isAfter(time)) return end / start
            } else if (trades[i].decision == Decision.SELL) {
                end = trades[i].usdAfter!!
                if (trades[i].time!!.isAfter(time)) return end / start
            }
        }
        return end / start
    }

    fun profitBefore(time: ZonedDateTime): Double {
        val start = startUsd
        var end = start
        for (i in 0 until trades.size) {
            if (trades[i].time!!.isAfter(time)) return end / start
            if (trades[i].decision == Decision.SELL)
                end = trades[i].usdAfter!!

        }
        return end / start
    }

    fun profitString() : String {
        val sb = StringBuilder()
        for (i in 0 until trades.size step 2)
            sb.append(when {
                trades[i+1].after()>trades[i].before()*1.03 -> "\u2795"
                trades[i+1].after()>trades[i].before() -> "+"
                trades[i+1].after()<trades[i].before()*.97 -> "\u2796"
                else -> "-"
                })
//            sb.append(when {
//                trades[i+1].after()>trades[i].before()*1.03 -> "+"//"\u2795"
//                trades[i+1].after()>trades[i].before()*1.004 -> "+"
//                trades[i+1].after()<trades[i].before()*.97 -> "\u2796"
//                trades[i+1].after()<trades[i].before()*0.997 -> "-"
//                else -> " "
//                })
        return sb.toString()
    }

    fun worstInterval(len: Int): Double {
        return when {
            trades.size > len -> return (len until trades.size).map { trades[it].after() / trades[it - len].before() }.min()!!
            trades.isNotEmpty() -> trades[trades.size - 1].after() / trades[0].before()
            else -> 1.0
        }
    }

    fun relWorstInterval(len: Int): Double {

        if (trades.isEmpty())
            return 1.0

        val y1 = 0.0
        val y2 = 3.0
        val x1=0.0
        val x2=1.0
        val exp = 5.0

        val a = (y1-y2) / (Math.pow(exp, x1) - Math.pow(exp, x2))
        val b = y1 - a * Math.pow(exp, x1)

        fun y(x:Double):Double = a * Math.pow(exp, x) + b

        val endEpoch = trades.last().time!!.toEpochSecond()
        val startEpoch = trades.first().time!!.toEpochSecond()

        return when {
            trades.size > len -> return (len until trades.size).map {
                val k = y((trades[it].time!!.toEpochSecond() - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble())
                k * trades[it].after() / trades[it - len].before()
            }.min()!!
            else -> trades[trades.size - 1].after() / trades[0].before()
        }
    }

}

