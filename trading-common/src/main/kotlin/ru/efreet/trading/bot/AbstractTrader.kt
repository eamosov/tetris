package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecord
import java.time.ZonedDateTime

data class TraderInstrumentData(
        val trades: MutableList<TradeRecord> = mutableListOf(),
        val indicators: MutableMap<String, MutableList<Pair<ZonedDateTime, Double>>> = mutableMapOf(),
        var startAsset: Double = 0.0,
        var asset: Double = 0.0) {

    var startPrice: Double = 0.0
    var minPrice: Double = Double.MAX_VALUE
    var maxPrice: Double = 0.0
    var endPrice: Double = 0.0

    lateinit var start: ZonedDateTime
    lateinit var end: ZonedDateTime

    fun isStartInitialized(): Boolean {
        return this::start.isInitialized
    }

}


/**
 * Created by fluder on 23/02/2018.
 */
abstract class AbstractTrader(val exchangeName: String) : Trader {

    protected val tradeData: MutableMap<Instrument, TraderInstrumentData> = mutableMapOf()

    protected val cash: MutableList<Pair<ZonedDateTime, Double>> = arrayListOf()


    protected fun tradeData(instrument: Instrument): TraderInstrumentData {
        return tradeData.computeIfAbsent(instrument) { TraderInstrumentData() }
    }

    abstract var usd: Double

    abstract val startUsd: Double

    override fun executeAdvice(advice: BotAdvice): TradeRecord? {

        val td = tradeData(advice.instrument)

        if (!td.isStartInitialized())
            td.start = advice.time

        td.end = advice.time

        td.endPrice = advice.bar.closePrice

        if (td.startPrice == 0.0) {
            td.startPrice = td.endPrice
        }

        advice.indicators?.let {
            for ((n, v) in it) {
                td.indicators.computeIfAbsent(n) { mutableListOf() }.add(Pair(advice.time, v))
            }
        }

        if (td.endPrice < td.minPrice)
            td.minPrice = td.endPrice
        else if (td.endPrice > td.maxPrice)
            td.maxPrice = td.endPrice

        var _cash = usd
        tradeData.forEach { _, it -> _cash += it.endPrice * it.asset }
        cash.add(Pair(advice.time, _cash))

        return null
    }

    fun funds():Double {
        return usd + tradeData.values.map { it.asset * it.endPrice }.sum()
    }

    override fun history(): TradeHistory {
        return TradeHistory(startUsd,
                startUsd + tradeData.values.map { it.startAsset * it.startPrice }.sum(),
                usd,
                funds(),
                tradeData,
                cash,
                tradeData.values.first().start,
                tradeData.values.first().end)
    }

}