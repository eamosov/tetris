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
                        val tradeProfit = trade.fundsAfter!! / history.trades[i - 1].fundsAfter!!
                        if (tradeProfit > 1)
                            tradesWithProfit++

                        profits.add(Pair(trade.time!!, tradeProfit))
                        funds.add(Pair(trade.time!!, trade.fundsAfter))
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