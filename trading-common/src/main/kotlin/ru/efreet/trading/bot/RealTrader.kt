package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.*
import ru.efreet.trading.exchange.impl.cache.FakeExchange
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.roundAmount
import java.time.Duration

/**
 * Created by fluder on 23/02/2018.
 */
class RealTrader(val tradeRecordDao: TradeRecordDao?,
                 val exchange: Exchange,
                 val limit: Double,
                 val bet: Double,
                 override val instruments: List<Instrument>
) : AbstractTrader(exchange.getName()) {

    lateinit var balanceResult: Exchange.CalBalanceResult
    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    override val startUsd: Double
    val startFunds: Double
    val baseName = "USDT"

    override val usd: Double get() = balanceResult.balances[baseName]!!

    val funds: Double get() = balanceResult.toBase["total"]!!

    val ticker: Map<Instrument, Ticker> get() = balanceResult.ticker

    init {

        //Balances in USD
        updateBalance()

        startUsd = usd
        startFunds = funds
    }

    private fun updateBalance(force: Boolean = true) {
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
        return balanceResult.balances[instrument.asset]!!
    }

    override fun executeAdvice(advice: BotAdvice): TradeRecord? {

        if (exchange is FakeExchange){
            exchange.setTicker(advice.instrument, advice.bar.closePrice)
        }

        super.executeAdvice(advice)

        if (advice.decision == Decision.BUY) {

            cancelAllOrders(advice.instrument)
            updateBalance(true)

            //сколько всего USD свободно и вложено в монеты, которыми торгует бот
            val deposit = (instruments.map { price(it) * availableAsset(it) }.sum() + usd) * limit

            //размер ставки
            val maxBet = deposit * bet

            val myBet = availableAsset(advice.instrument) * price(advice.instrument)

            val asset = minOf(maxBet - myBet, usd) / advice.price

            if (availableAsset(advice.instrument) < 10 && asset * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]!!
                val assetBefore = balanceResult.balances[advice.instrument.asset]!!
                val order = exchange.buy(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                if (order != null) {
                    val trade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument.toString(), order.price,
                            advice.decision, advice.decisionArgs, order.type, order.asset,
                            exchange.getFee() / 200.0,
                            usdBefore,
                            assetBefore,
                            balanceResult.balances[baseName]!!,
                            balanceResult.balances[advice.instrument.asset]!!)

                    tradeData(advice.instrument).trades.add(trade)
                    tradeRecordDao?.create(trade)
                    return trade
                }
            }
        } else if (advice.decision == Decision.SELL) {

            cancelAllOrders(advice.instrument)
            updateBalance(true)
            val asset = availableAsset(advice.instrument)

            if (asset * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]
                val assetBefore = balanceResult.balances[advice.instrument.asset]
                val order = exchange.sell(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                if (order != null) {
                    val trade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument.toString(), order.price,
                            advice.decision, advice.decisionArgs, order.type, order.asset,
                            exchange.getFee() / 200.0,
                            usdBefore!!,
                            assetBefore!!,
                            balanceResult.balances[baseName]!!,
                            balanceResult.balances[advice.instrument.asset]!!
                    )

                    tradeData(advice.instrument).trades.add(trade)
                    tradeRecordDao?.create(trade)
                    return trade
                }
            }
        } else {
            updateBalance(false)
        }

        return null
    }

    override fun price(instrument: Instrument): Double {
        return ticker[instrument]!!.highestBid
    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        return exchange.getOpenOrders(instrument)
    }

    override fun cancelAllOrders(instrument: Instrument) {
        exchange.getOpenOrders(instrument).forEach { exchange.cancelOrder(it) }
    }

    companion object {
        fun fakeTrader(feeP: Double, interval: BarInterval, instrument: Instrument): RealTrader {
            return RealTrader(null, FakeExchange("fake", feeP, interval), 1.0, 1.0, listOf(instrument))
        }
    }
}