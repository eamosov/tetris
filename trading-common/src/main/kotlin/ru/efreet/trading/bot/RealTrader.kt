package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.roundAmount
import java.time.Duration

/**
 * Created by fluder on 23/02/2018.
 */
class RealTrader(val tradeRecordDao: TradeRecordDao, val exchange: Exchange, val limit: Double, exchangeName: String) : AbstractTrader(exchangeName) {

    var balanceResult: Exchange.CalBalanceResult
    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    override val startUsd: Double
    val startFunds: Double
    val baseName = "USDT"

    override var usd: Double = 0.0
    var funds: Double

    var ticker: Map<Instrument, Ticker>

    private var lastTrade: TradeRecord? = null

    init {

        //Balances in USD
        balanceResult = exchange.calcBalance(baseName)

        startUsd = balanceResult.balances[baseName]!!
        startFunds = balanceResult.toBase["total"]!!

        usd = startUsd
        funds = startFunds

        ticker = balanceResult.ticker
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

    override fun executeAdvice(advice: BotAdvice): TradeRecord? {
        super.executeAdvice(advice)

        if (lastTrade != null)
            tradeRecordDao.update(lastTrade!!)

        usd = balanceResult.balances[baseName]!!
        funds = balanceResult.toBase["total"]!!

        if (advice.decision == Decision.BUY && advice.amount > 0) {

            if (advice.amount * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]!!
                val assetBefore = balanceResult.balances[advice.instrument.asset]!!
                val order = exchange.buy(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.MARKET)

                updateBalance(true)

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument, order.price,
                        advice.decision, advice.decisionArgs, order.type, order.amount,
                        exchange.getFee() / 100.0 / 2.0,
                        usdBefore,
                        assetBefore,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!
                )

                tradeData(advice.instrument).trades.add(lastTrade!!)
                tradeRecordDao.create(lastTrade!!)
                return lastTrade
            }
        } else if (advice.decision == Decision.SELL && advice.amount > 0) {
            if (advice.amount * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]
                val assetBefore = balanceResult.balances[advice.instrument.asset]
                val order = exchange.sell(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.MARKET)

                updateBalance(true)

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument, order.price,
                        advice.decision, advice.decisionArgs, order.type, order.amount,
                        exchange.getFee() / 100.0 / 2.0,
                        usdBefore!!,
                        assetBefore!!,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!
                )

                tradeData(advice.instrument).trades.add(lastTrade!!)
                tradeRecordDao.create(lastTrade!!)
                return lastTrade
            }
        }

        return null
    }
}