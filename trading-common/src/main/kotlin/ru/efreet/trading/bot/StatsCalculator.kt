package ru.efreet.trading.bot

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import ru.efreet.trading.Decision
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

        val profits = mutableListOf<Pair<ZonedDateTime, Double>>()
        val funds = mutableListOf<Pair<ZonedDateTime, Double>>()

        for (i in 0 until history.trades.size) {
            val trade = history.trades[i]

            if (trade.decision == Decision.SELL) {
                trades++

                if (i > 0 && history.trades[i - 1].decision == Decision.BUY) {
                    if (trade.fundsAfter != 0.0 && history.trades[i - 1].fundsAfter != 0.0) {
                        val tradeProfit = trade.fundsAfter!! / history.trades[i - 1].usdBefore!!
                        if (tradeProfit > 1)
                            tradesWithProfit++

                        profits.add(Pair(trade.time!!, tradeProfit))
                        funds.add(Pair(trade.time, trade.fundsAfter))
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

        val profitPerTrade = if (profits.size > 0) profits.map { it.second }.sum() / profits.size else 0.0
        val sdProfitPerTrade = if (profits.size > 0) Math.sqrt(profits.map { (it.second - profitPerTrade).pow2() }.sum() / profits.size) else 0.0

        var relProfit = 0.0
        if (profits.size > 1){
            relProfit = 1.0

            val endEpoch = profits.last().first.toEpochSecond()
            val startEpoch = profits.first().first.toEpochSecond()

            val y1 = 0.0
            val y2 = 3.0
            val x1=0.0
            val x2=1.0
            val exp = 5.0

            val a = (y1-y2) / (Math.pow(exp, x1) - Math.pow(exp, x2))
            val b = y1 - a * Math.pow(exp, x1)

            fun y(x:Double):Double = a * Math.pow(exp, x) + b


            profits.forEach {
                //val k = (0.1 + 2.0 * (it.first.toEpochSecond() - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble())
                val k = y((it.first.toEpochSecond() - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble())
                relProfit *= ((it.second - 1.0) * k + 1.0)
            }
        }

        return TradesStats(
                profits.size,
                if (profits.size > 0) tradesWithProfit.toDouble() / profits.size else 0.0,
                profit,
                profitPerTrade,
                sdProfitPerTrade,
                if (profits.size > 0) profits.sma(5).count { it.second > 1.0 }.toDouble() / profits.size else 0.0,
                if (profits.size > 0) profits.sma(10).count { it.second > 1.0 }.toDouble() / profits.size else 0.0,
                if (pearson.isNaN()) 0.0 else pearson,
                history.start,
                history.end,
                history.profitPerDay,
                relProfit
        )
    }
}