package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.roundAmount
import java.time.Duration

/**
 * Created by fluder on 23/02/2018.
 */
class RealTrader(val tradeRecordDao: TradeRecordDao,
                 val exchange: Exchange,
                 val limit: Double,
                 exchangeName: String,
                 override val instruments: List<Instrument>
) : AbstractTrader(exchangeName) {

    lateinit var balanceResult: Exchange.CalBalanceResult
    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    override val startUsd: Double
    val startFunds: Double
    val baseName = "USDT"

    override val usd: Double get() = balanceResult.balances[baseName]!!

    val funds: Double get() = balanceResult.toBase["total"]!!

    val ticker: Map<Instrument, Ticker> get() = balanceResult.ticker

    private var lastTrade: TradeRecord? = null

    init {

        //Balances in USD
        updateBalance()

        startUsd = usd
        startFunds = funds
    }

    override fun updateBalance(force: Boolean) {
        balanceUpdatedTimer.invoke({
            balanceResult = exchange.calcBalance(baseName)
        }, force)
    }

//    override fun availableUsd(instrument: Instrument): Double {
//
//        updateBalance()
//
//        //на сколько куплено валюты
//        val entered = balanceResult.toBase[instrument.asset]!!
//
//        //полный размер депозита
//        val total = balanceResult.toBase["total"]!!
//
//        //осталось USD
//        val free = balanceResult.balances[baseName]!!
//
//        return minOf(total * limit - entered, free)
//    }

    override fun availableAsset(instrument: Instrument): Double {

        //TODO надо периодически обновлять баланс??
        //updateBalance()

        return balanceResult.balances[instrument.asset]!!
    }

    override fun executeAdvice(advice: BotAdvice): TradeRecord? {
        super.executeAdvice(advice)

        if (lastTrade != null)
            tradeRecordDao.update(lastTrade!!)

        if (advice.decision == Decision.BUY && advice.amount > 0) {

            if (advice.amount * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]!!
                val assetBefore = balanceResult.balances[advice.instrument.asset]!!
                val order = exchange.buy(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument.toString(), order.price,
                        advice.decision, advice.decisionArgs, order.type, order.asset,
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
                val order = exchange.sell(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument.toString(), order.price,
                        advice.decision, advice.decisionArgs, order.type, order.asset,
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
        } else {
            updateBalance(false)
        }

        return null
    }

    override fun price(instrument: Instrument): Double {
        return ticker[instrument]?.highestBid ?: Double.NaN;
    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        return exchange.getOpenOrders(instrument)
    }

    override fun cancelAllOrders(instrument: Instrument){
        exchange.getOpenOrders(instrument).forEach {exchange.cancelOrder(it)}
    }
}