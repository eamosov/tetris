package ru.efreet.trading.bot

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.utils.pow2
import ru.efreet.trading.utils.sma
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 22/02/2018.
 */
class StatsCalculator {


    fun stats(history: TradeHistory): TradesStats {

        val profit = if (history.endFunds != 0.0 && history.startFunds != 0.0)
            history.endFunds / history.startFunds
        else
            0.0

        var trades = 0
        var tradesWithProfit = 0

        var longTrades = 0
        var longTradesWithProfit = 0

        var shorTrades = 0
        var shortTradesWithProfit = 0

        val profits = mutableListOf<Pair<ZonedDateTime, Double>>()
        val funds = mutableListOf<Pair<ZonedDateTime, Double>>()

        for (i in 0 until history.trades.size) {
            val sellTrade = history.trades[i]

            if (sellTrade.side == OrderSide.SELL) {
                trades++

                val buyTrade = history.trades[i - 1]

                if (i > 0 && buyTrade.side == OrderSide.BUY) {

                    if (buyTrade.long!!)
                        longTrades++
                    else
                        shorTrades++

                    if (sellTrade.fundsAfter != 0.0 && buyTrade.fundsAfter != 0.0) {
                        val tradeProfit = sellTrade.fundsAfter!! / buyTrade.fundsAfter!!
                        if (tradeProfit > 1) {
                            tradesWithProfit++

                            if (buyTrade.long) {
                                longTradesWithProfit++
                            } else {
                                shortTradesWithProfit++
                            }
                        }

                        profits.add(Pair(sellTrade.time!!, tradeProfit))
                        funds.add(Pair(sellTrade.time, sellTrade.fundsAfter))
                    }
                }
            }
        }

        val pearson: Double;

        if (funds.size > 2) {
            val (startTime, startFunds) = funds.first()

            //время в часах
            val t = Duration.between(startTime, funds.last().first).toHours()

            //профит в час
            val k = Math.pow(funds.last().second / startFunds, 1.0 / t)

            //Идеальный график распределения профита
            val ideal = funds.map { startFunds * Math.pow(k, Duration.between(startTime, it.first).toHours().toDouble()) }.toDoubleArray()

            pearson = PearsonsCorrelation().correlation(funds.map { it.second }.toDoubleArray(), ideal)
        } else {
            pearson = 0.0
        }

        val avrProfitPerTrade = if (profits.size > 0) profits.map { it.second }.sum() / profits.size else 0.0
        val sdProfitPerTrade = if (profits.size > 0) Math.sqrt(profits.map { (it.second - avrProfitPerTrade).pow2() }.sum() / profits.size) else 0.0

        //println("$shorTrades / ${shortTradesWithProfit.toDouble() / shorTrades.toDouble()}, $longTrades / ${longTradesWithProfit.toDouble() / longTrades.toDouble()}")

        return TradesStats(
                profits.size,
                if (profits.size > 0) tradesWithProfit.toDouble() / profits.size else 0.0,
                profit,
                avrProfitPerTrade,
                sdProfitPerTrade,
                if (profits.size > 0) profits.sma(5).count { it.second > 1.0 }.toDouble() / profits.size else 0.0,
                if (profits.size > 0) profits.sma(10).count { it.second > 1.0 }.toDouble() / profits.size else 0.0,
                pearson,
                history.start,
                history.end
        )
    }
}