package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecord
import java.io.Serializable
import java.lang.Math.pow
import java.time.ZonedDateTime

/**
 * Created by fluder on 20/02/2018.
 */

data class TradeHistory(val startUsd: Double,
                        val startFunds: Double,
                        val endUsd: Double,
                        val endFunds: Double,
                        val instruments: Map<Instrument, ITradeHistory>,
                        val cash: List<Pair<ZonedDateTime, Double>>,
                        val start: ZonedDateTime,
                        val end: ZonedDateTime) : Serializable {

    companion object {

        @JvmStatic
        fun loadFromJson(path: String): TradeHistory = ru.efreet.trading.utils.loadFromJson(path)
    }

    val profit: Double get() = endFunds / startFunds

    val profitPerDay: Double get() = pow(endFunds / startFunds, (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))

    val profitPerDayToGrow: Double get() = profitPerDayToGrow(instruments.keys.first())

    fun profitPerDayToGrow(instrument: Instrument): Double {
        return pow((endFunds / startFunds) / (instruments[instrument]!!.closePrice / instruments[instrument]!!.startPrice), (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond()))
    }

    fun profitBeforeExtended(time: ZonedDateTime): Double {
        return profitBeforeExtended(instruments.keys.first(), time)
    }

    fun profitBeforeExtended(instrument: Instrument, time: ZonedDateTime): Double {
        val start = startUsd
        var end = start
        val ih = instruments[instrument]
        for (i in 0 until instruments.size) {
            if (ih!!.trades[i].decision == Decision.BUY) {
                if (ih.trades[i].time!!.isAfter(time)) return end / start
            } else if (ih.trades[i].decision == Decision.SELL) {
                end = ih.trades[i].usdAfter!!
                if (ih.trades[i].time!!.isAfter(time)) return end / start
            }
        }
        return end / start
    }

    fun profitBefore(time: ZonedDateTime): Double {
        return profitBefore(instruments.keys.first(), time)
    }

    fun profitBefore(instrument: Instrument, time: ZonedDateTime): Double {
        val start = startUsd
        var end = start
        val ih = instruments[instrument]
        for (i in 0 until instruments.size) {
            if (ih!!.trades[i].time!!.isAfter(time)) return end / start
            if (ih.trades[i].decision == Decision.SELL)
                end = ih.trades[i].usdAfter!!

        }
        return end / start
    }

    fun profitString(): String {
        return profitString(instruments.keys.first())
    }

    fun profitString(instrument: Instrument): String {
        val ih = instruments[instrument]

        val sb = StringBuilder()
        for (i in 0 until instruments.size step 2)
            sb.append(when {
                ih!!.trades[i + 1].after() > ih.trades[i].before() * 1.03 -> "\u2795"
                ih.trades[i + 1].after() > ih.trades[i].before() -> "+"
                ih.trades[i + 1].after() < ih.trades[i].before() * .97 -> "\u2796"
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
        return worstInterval(instruments.keys.first(), len)
    }

    fun worstInterval(instrument: Instrument, len: Int): Double {

        val ih = instruments[instrument]

        return when {
            instruments.size > len -> return (len until instruments.size).map { ih!!.trades[it].after() / ih.trades[it - len].before() }.min()!!
            instruments.isNotEmpty() -> ih!!.trades[instruments.size - 1].after() / ih.trades[0].before()
            else -> 1.0
        }
    }

    fun relWorstInterval(len: Int): Double {
        return relWorstInterval(instruments.keys.first(), len)
    }

    fun relWorstInterval(instrument: Instrument, len: Int): Double {

        val ih = instruments[instrument]

        if (instruments.isEmpty())
            return 1.0

        val y1 = 0.0
        val y2 = 3.0
        val x1 = 0.0
        val x2 = 1.0
        val exp = 5.0

        val a = (y1 - y2) / (Math.pow(exp, x1) - Math.pow(exp, x2))
        val b = y1 - a * Math.pow(exp, x1)

        fun y(x: Double): Double = a * Math.pow(exp, x) + b

        val endEpoch = ih!!.trades.last().time!!.toEpochSecond()
        val startEpoch = ih.trades.first().time!!.toEpochSecond()

        return when {
            instruments.size > len -> return (len until instruments.size).map {
                val k = y((ih.trades[it].time!!.toEpochSecond() - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble())
                k * ih.trades[it].after() / ih.trades[it - len].before()
            }.min()!!
            else -> ih.trades[instruments.size - 1].after() / ih.trades[0].before()
        }
    }

    fun getTrades() : List<TradeRecord> {
        return instruments.values.first().trades
    }

}

