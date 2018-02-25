package ru.efreet.trading.bot

import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.Periodical
import java.time.Duration

/**
 * Created by fluder on 23/02/2018.
 */
class RealTrader(val exchange: Exchange, val limit: Double, val baseName: String, val assetName: String) : AbstractTrader() {

    var balanceResult: Exchange.CalBalanceResult
    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    val startUsd: Double
    val startAsset: Double
    val startFunds: Double

    var usd: Double
    var asset: Double
    var funds: Double

    init {

        //Balances in USD
        balanceResult = exchange.calcBalance(baseName)

        startUsd = balanceResult.balances[baseName]!!
        startFunds = balanceResult.toBase["total"]!!
        startAsset = balanceResult.balances[assetName]!!

        usd = startUsd
        funds = startFunds
        asset = startAsset

        lastPrice = balanceResult.ticker[Instrument(assetName, baseName)]!!.highestBid
        minPrice = lastPrice
        maxPrice = lastPrice
    }

    fun updateBalance(force: Boolean = false) {
        balanceUpdatedTimer.invoke({
            balanceResult = exchange.calcBalance(baseName)
        }, force)
    }

    override fun availableUsd(instrument: Instrument): Double {

        updateBalance()

        //на сколько куплено валюты
        val entered = balanceResult.toBase[instrument.asset]!!

        //полный размер депозита
        val total = balanceResult.toBase["total"]!!

        //осталось USD
        val free = balanceResult.balances[baseName]!!

        return minOf(total * limit - entered, free)
    }

    override fun availableAsset(instrument: Instrument): Double {

        updateBalance()

        balanceUpdatedTimer.invoke({
            balanceResult = exchange.calcBalance(baseName)
        })

        return balanceResult.balances[instrument.asset]!!
    }

    override fun executeAdvice(advice: Advice): TradeRecord? {
        super.executeAdvice(advice)

        usd = balanceResult.balances[baseName]!!
        funds = balanceResult.toBase["total"]!!
        asset = balanceResult.balances[assetName]!!

        if (advice.orderSide == OrderSide.BUY && advice.amount > 0) {

            if (advice.amount >= 0.001) {

                val usdBefore = balanceResult.balances[baseName]!!
                val assetBefore = balanceResult.balances[advice.instrument.asset]!!
                val order = exchange.buy(advice.instrument, advice.amount, advice.price, OrderType.LIMIT)

                updateBalance(true)

                lastTrade = TradeRecord(order.time, order.orderId, order.instrument, order.price, order.side, order.type, order.amount,
                        0.0,
                        usdBefore,
                        assetBefore,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!,
                        balanceResult.toBase["total"]!!
                )

                trades.add(lastTrade!!)
                return lastTrade
            }
        } else if (advice.orderSide == OrderSide.SELL && advice.amount > 0) {
            if (advice.amount > 0.001) {

                val usdBefore = balanceResult.balances[baseName]
                val assetBefore = balanceResult.balances[advice.instrument.asset]
                val order = exchange.sell(advice.instrument, advice.amount, advice.price, OrderType.LIMIT)

                updateBalance(true)

                lastTrade = TradeRecord(order.time, order.orderId, order.instrument, order.price, order.side, order.type, order.amount,
                        0.0,
                        usdBefore!!,
                        assetBefore!!,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!,
                        balanceResult.toBase["total"]!!
                )

                trades.add(lastTrade!!)
                return lastTrade
            }
        }

        return null
    }

    override fun history(): TradeHistory {
        return TradeHistory(startUsd, startAsset, startFunds, usd, asset, funds, trades, indicators, arrayListOf(),
                startPrice,
                lastPrice,
                minPrice,
                maxPrice)
    }
}