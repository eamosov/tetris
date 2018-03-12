package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.exchange.OrderType
import ru.efreet.trading.exchange.TradeRecord
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by fluder on 23/02/2018.
 */
class FakeTrader(var startUsd: Double = 1000.0,
                 var startAsset: Double = 0.0,
                 val feeP: Double = 0.02,
                 val fillCash: Boolean = false,
                 exchangeName: String,
                 instrument: Instrument) : AbstractTrader(exchangeName, instrument) {

    private val cash = mutableListOf<Pair<ZonedDateTime, Double>>()
    private var lastTrade: TradeRecord? = null

    var usd = startUsd
    var asset = startAsset

    fun funds(usd: Double, asset: Double, price: Double): Double {
        return usd + asset * price
    }

    fun feeRatio(): Double {
        return feeP / 100.0 / 2.0
    }

    fun clearHistory() {

        minPrice = Double.MAX_VALUE
        maxPrice = 0.0

        startUsd = usd
        startPrice = lastPrice
        startAsset = asset


        lastTrade = null
        trades.clear()
        cash.clear()
    }

    override fun executeAdvice(advice: Advice): TradeRecord? {

        super.executeAdvice(advice)

        if (fillCash) {
            cash.add(Pair(advice.time, usd + asset * advice.price))
        }

        if (advice.orderSide == OrderSide.BUY && advice.amount > 0) {
            val fee = advice.amount * feeRatio()
            val usdBefore = usd
            val assetBefore = asset
            usd -= advice.price * advice.amount
            asset += advice.amount - fee
            lastTrade = TradeRecord(UUID.randomUUID().toString(), advice.time, exchangeName, advice.instrument.toString(), advice.price, advice.orderSide, OrderType.LIMIT, advice.amount, fee, usdBefore, assetBefore, usd, asset, usd + asset * advice.price, advice.long, advice.tsl, advice.sellBySl, advice.sellByTsl)
            trades.add(lastTrade!!)
            return lastTrade
        } else if (advice.orderSide == OrderSide.SELL && advice.amount > 0) {
            val fee = advice.amount * feeRatio()
            val usdBefore = usd
            val assetBefore = asset
            usd += advice.price * (advice.amount - fee)
            asset -= advice.amount
            lastTrade = TradeRecord(UUID.randomUUID().toString(), advice.time, exchangeName, advice.instrument.toString(), advice.price, advice.orderSide, OrderType.LIMIT, advice.amount, fee, usdBefore, assetBefore, usd, asset, usd + asset * advice.price, advice.long, advice.tsl, advice.sellBySl, advice.sellByTsl)
            trades.add(lastTrade!!)
            return lastTrade
        } else {
            return null
        }
    }

    override fun history(start: ZonedDateTime, end: ZonedDateTime): TradeHistory {
        return TradeHistory(startUsd,
                startAsset,
                funds(startUsd, startAsset, startPrice),
                usd,
                asset,
                funds(usd, asset, lastPrice),
                trades,
                indicators,
                cash,
                startPrice,
                lastPrice,
                minPrice,
                maxPrice,
                start,
                end)
    }

    override fun availableUsd(instrument: Instrument): Double {
        return usd
    }

    override fun availableAsset(instrument: Instrument): Double {
        return asset
    }

    override fun lastTrade(): TradeRecord? {
        return lastTrade
    }
}