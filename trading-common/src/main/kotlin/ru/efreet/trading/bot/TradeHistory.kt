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

data class TradeHistory(val startUsd: Float,
                        val startFunds: Float,
                        val endUsd: Float,
                        val endFunds: Float,
                        val instruments: Map<Instrument, ITradeHistory>,
                        val cash: List<Pair<ZonedDateTime, Float>>,
                        val start: ZonedDateTime,
                        val end: ZonedDateTime) : Serializable {

    companion object {

        @JvmStatic
        fun loadFromJson(path: String): TradeHistory = ru.efreet.trading.utils.loadFromJson(path)
    }

    val profit: Float get() = endFunds / startFunds

    val profitPerDay: Float get() = pow(endFunds.toDouble() / startFunds.toDouble(), (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond())).toFloat()

    val profitPerDayToGrow: Float get() = profitPerDayToGrow(instruments.keys.first())

    fun profitPerDayToGrow(instrument: Instrument): Float {
        return pow((endFunds.toDouble() / startFunds.toDouble()) / (instruments[instrument]!!.closePrice / instruments[instrument]!!.startPrice), (3600.0 * 24.0) / (end.toEpochSecond() - start.toEpochSecond())).toFloat()
    }

    fun profitBeforeExtended(time: ZonedDateTime): Float {
        return profitBeforeExtended(instruments.keys.first(), time)
    }

    fun profitBeforeExtended(instrument: Instrument, time: ZonedDateTime): Float {
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

    fun profitBefore(time: ZonedDateTime): Float {
        return profitBefore(instruments.keys.first(), time)
    }

    fun profitBefore(instrument: Instrument, time: ZonedDateTime): Float {
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

    fun worstInterval(len: Int): Float {
        return worstInterval(instruments.keys.first(), len)
    }

    fun worstInterval(instrument: Instrument, len: Int): Float {

        val ih = instruments[instrument]

        return when {
            instruments.size > len -> return (len until instruments.size).map { ih!!.trades[it].after() / ih.trades[it - len].before() }.min()!!
            instruments.isNotEmpty() -> ih!!.trades[instruments.size - 1].after() / ih.trades[0].before()
            else -> 1.0F
        }
    }

    fun relWorstInterval(len: Int): Float {
        return relWorstInterval(instruments.keys.first(), len)
    }

    fun relWorstInterval(instrument: Instrument, len: Int): Float {

        val ih = instruments[instrument]

        if (instruments.isEmpty())
            return 1.0F

        val y1 = 0.0
        val y2 = 3.0
        val x1 = 0.0
        val x2 = 1.0
        val exp = 5.0

        val a = (y1 - y2) / (Math.pow(exp.toDouble(), x1.toDouble()) - Math.pow(exp.toDouble(), x2.toDouble()))
        val b = y1 - a * Math.pow(exp.toDouble(), x1.toDouble())

        fun y(x: Double): Double = a * Math.pow(exp, x).toFloat() + b

        val endEpoch = ih!!.trades.last().time!!.toEpochSecond()
        val startEpoch = ih.trades.first().time!!.toEpochSecond()

        return when {
            instruments.size > len -> return (len until instruments.size).map {
                val k = y((ih.trades[it].time!!.toEpochSecond() - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble())
                k * ih.trades[it].after() / ih.trades[it - len].before()
            }.min()!!.toFloat()
            else -> ih.trades[instruments.size - 1].after() / ih.trades[0].before()
        }
    }

    fun getTrades() : List<TradeRecord> {
        return instruments.values.first().trades
    }

}

