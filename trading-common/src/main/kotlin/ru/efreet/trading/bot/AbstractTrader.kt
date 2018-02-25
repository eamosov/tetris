package ru.efreet.trading.bot

import ru.efreet.trading.exchange.TradeRecord
import java.time.ZonedDateTime

/**
 * Created by fluder on 23/02/2018.
 */
abstract class AbstractTrader() : Trader {

    protected val trades = mutableListOf<TradeRecord>()
    protected val indicators = mutableMapOf<String, MutableList<Pair<ZonedDateTime, Double>>>()
    protected var minPrice: Double = Double.MAX_VALUE
    protected var maxPrice: Double = 0.0
    protected var startPrice: Double = 0.0

    protected var lastPrice: Double = 0.0
    protected var lastTrade: TradeRecord? = null

    override fun executeAdvice(advice: Advice): TradeRecord? {

        lastPrice = advice.bar.closePrice

        if (startPrice == 0.0) {
            startPrice = lastPrice
        }


        advice.indicators?.let {
            for ((n, v) in it) {
                indicators.computeIfAbsent(n, { mutableListOf() }).add(Pair(advice.time, v))
            }
        }

        if (lastPrice < minPrice)
            minPrice = lastPrice
        else if (lastPrice > maxPrice)
            maxPrice = lastPrice

        return null
    }


    override fun lastTrade(): TradeRecord? {
        return lastTrade
    }
}