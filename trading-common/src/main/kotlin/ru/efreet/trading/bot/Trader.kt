package ru.efreet.trading.bot

import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.*
import ru.efreet.trading.exchange.impl.cache.FakeExchange
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.roundAmount
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 23/02/2018.
 */
class Trader(val tradeRecordDao: TradeRecordDao?,
             val exchange: Exchange,
             val limit: Double,
             val bet: Double,
             val instruments: List<Instrument>
) {

    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    private val iTradeHistory: MutableMap<Instrument, ITradeHistory> = mutableMapOf()
    private val cash: MutableList<Pair<ZonedDateTime, Double>> = arrayListOf()


    private fun iTradeHistory(instrument: Instrument): ITradeHistory {
        return iTradeHistory.computeIfAbsent(instrument) { ITradeHistory() }
    }

    private val startUsd: Double
    private val startDeposit: Double

    val usd: Double get() = balance("USDT")

    private lateinit var ticker: Map<Instrument, Ticker>
    private lateinit var balances: Map<String, Double>

    init {

        //Balances in USD
        updateTicker()
        updateBalance()

        startUsd = usd
        startDeposit = deposit()

        instruments.forEach {
            iTradeHistory(it).startAsset = balance(it)
            iTradeHistory(it).startPrice = price(it)
        }
    }

    private fun updateTicker() {
        ticker = exchange.getTicker()
    }

    private fun updateBalance() {
        balances = exchange.getBalancesMap()
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

    fun balance(instrument: Instrument): Double {
        return balance(instrument.asset)
    }

    private fun balance(currency: String): Double {
        return balances[currency] ?: 0.0
    }

    fun executeAdvice(advice: BotAdvice): TradeRecord? {

        if (exchange is FakeExchange) {
            exchange.setTicker(advice.instrument, advice.bar)
        }

        updateTicker()
        updateBalance()

        val td = iTradeHistory(advice.instrument)

        if (!td.isStartInitialized())
            td.startTime = advice.bar.endTime

        td.endTime = advice.bar.endTime

        td.closePrice = advice.bar.closePrice

        if (td.startPrice == 0.0) {
            td.startPrice = td.closePrice
        }

        advice.indicators?.let {
            for ((n, v) in it) {
                td.indicators.computeIfAbsent(n) { mutableListOf() }.add(Pair(advice.time, v))
            }
        }

        if (td.closePrice < td.minPrice)
            td.minPrice = td.closePrice
        else if (td.closePrice > td.maxPrice)
            td.maxPrice = td.closePrice

        cash.add(Pair(advice.bar.endTime, deposit()))


        if (advice.decision == Decision.BUY) {

            if (cancelAllOrders(advice.instrument))
                updateBalance()

            //максимальный размер ставки
            val maxBet = deposit() * limit * bet

            //текущая ставка
            val myBet = balance(advice.instrument) * price(advice.instrument)

            val asset = minOf(maxBet - myBet, usd) / advice.price

            if (balance(advice.instrument) < 10 && asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val order = exchange.buy(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                if (order != null) {
                    val trade = TradeRecord(order.orderId, order.time, exchange.getName(), order.instrument.toString(), order.price,
                            advice.decision, advice.decisionArgs, order.type, order.asset,
                            exchange.getFee() / 200.0,
                            usdBefore,
                            assetBefore,
                            balance(advice.instrument.base),
                            balance(advice.instrument.asset))

                    iTradeHistory(advice.instrument).trades.add(trade)
                    tradeRecordDao?.create(trade)
                    return trade
                }
            }
        } else if (advice.decision == Decision.SELL) {

            if (cancelAllOrders(advice.instrument))
                updateBalance()

            var asset = balance(advice.instrument)

            //keep 2 BNB
            if (advice.instrument == Instrument.BNB_BTC) asset -= 2.0

            if (asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val order = exchange.sell(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT)

                updateBalance()

                if (order != null) {
                    val trade = TradeRecord(order.orderId, order.time, exchange.getName(), order.instrument.toString(), order.price,
                            advice.decision, advice.decisionArgs, order.type, order.asset,
                            exchange.getFee() / 200.0,
                            usdBefore,
                            assetBefore,
                            balance(advice.instrument.base),
                            balance(advice.instrument.asset))

                    iTradeHistory(advice.instrument).trades.add(trade)
                    tradeRecordDao?.create(trade)
                    return trade
                }
            }
        }

        return null
    }

    fun deposit(): Double {
        return instruments.map { instrument ->
            price(instrument) * (balance(instrument) + getOpenOrders(instrument).filter { it.side == Decision.SELL }.map { it.asset }.sum()) + getOpenOrders(instrument).filter { it.side == Decision.BUY }.map { it.asset * it.price }.sum()
        }.sum() + usd
    }

    fun price(instrument: Instrument): Double {
        return ticker[instrument]?.highestBid ?: 0.0
    }

    fun getOpenOrders(instrument: Instrument): List<Order> {
        return exchange.getOpenOrders(instrument)
    }

    fun cancelAllOrders(instrument: Instrument): Boolean {
        var isCancelled = false
        exchange.getOpenOrders(instrument).forEach {
            isCancelled = true
            log.warn("Cancel order: {}", it)
            exchange.cancelOrder(it)
        }
        return isCancelled
    }

    fun history(): TradeHistory {
        return TradeHistory(startUsd,
                startDeposit,
                usd,
                deposit(),
                iTradeHistory,
                cash,
                iTradeHistory.values.first().startTime,
                iTradeHistory.values.first().endTime)
    }

    fun logBalance() {

        for (i in instruments) {
            log.info("{}: {} ({} USDT), orders={}", i.asset, balance(i), (balance(i) * price(i)).round2(), getOpenOrders(i))
        }

        log.info("total: {}", deposit().round2())
    }

    companion object {
        fun fakeTrader(feeP: Double, interval: BarInterval, instrument: Instrument): Trader {
            return Trader(null, FakeExchange("fake", feeP, interval), 1.0, 1.0, listOf(instrument))
        }

        val log = LoggerFactory.getLogger(Trader.javaClass)
    }
}