package ru.efreet.trading.bot

import org.slf4j.LoggerFactory
import ru.efreet.telegram.Telegram
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.*
import ru.efreet.trading.exchange.impl.cache.FakeExchange
import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.roundAmount
import java.time.ZonedDateTime

/**
 * Created by fluder on 23/02/2018.
 */
class Trader(val tradeRecordDao: TradeRecordDao?,
             val exchange: Exchange,
             val limit: Double,
             val bet: Double,
             val instruments: List<Instrument>,
             val telegram: Telegram? = null
) {

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

    private val lastBuy = mutableMapOf<Instrument, XBar>()

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

    fun balance(instrument: Instrument): Double {
        return balance(instrument.asset)
    }

    private fun balance(currency: String): Double {
        val value = balances[currency] ?: 0.0
        return if (currency == "BNB")
            maxOf(0.0, value - 1.0)
        else
            value
    }

    fun executeAdvice(logicAdvice: BotAdvice): TradeRecord? {

        var advice = logicAdvice;

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
                td.indicators.computeIfAbsent(n) { mutableListOf() }.add(Pair(advice.bar.endTime, v))
            }
        }

        if (td.closePrice < td.minPrice)
            td.minPrice = td.closePrice
        else if (td.closePrice > td.maxPrice)
            td.maxPrice = td.closePrice

        cash.add(Pair(advice.bar.endTime, deposit()))

//        if (advice.decision == Decision.NONE &&
//                lastBuy.containsKey(advice.instrument) &&
//                advice.bar.closePrice < lastBuy[advice.instrument]!!.closePrice * 0.995) {
//
//            advice = BotAdvice(advice.time, Decision.BUY, mapOf(Pair("step", "1")), advice.instrument, advice.bar.closePrice, advice.bar, advice.indicators)
//        }


        if (advice.decision == Decision.BUY) {

            if (cancelAllOrders(advice.instrument))
                updateBalance()

            //usd + все наблюдаемые валюты
            val deposit = deposit(false)

            //сколько боту осталось доступно USD
            val availableUsd = usd - deposit * (1.0 - limit)

            //максимальный размер ставки на одну валюту
            val maxBet = deposit * limit * bet

            //сколько уже поставили на одну валюту
            val myBet = balance(advice.instrument) * price(advice.instrument)

            //сколько ещё можем доставить?
            //val asset = minOf(maxBet - myBet, availableUsd, maxBet * 0.5) / advice.price
            val asset = minOf(maxBet - myBet, availableUsd) / advice.price

            if (balance(advice.instrument) < 10 && asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val order = exchange.buy(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT, advice.time)

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

                    lastBuy[advice.instrument] = advice.bar

                    try {
                        telegram?.sendMessage("BUY ${trade.amount} ${trade.instrument} for ${trade.price}")
                    } catch (e: Exception) {
                        log.error("Error sending message to telegram", e)
                    }
                    return trade
                }
            }
        } else if (advice.decision == Decision.SELL) {

            lastBuy.remove(advice.instrument)

            if (cancelAllOrders(advice.instrument))
                updateBalance()

            val asset = balance(advice.instrument)

            if (asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val order = exchange.sell(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT, advice.time)

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
                    try {
                        telegram?.sendMessage("SELL ${trade.amount} ${trade.instrument} for ${trade.price}")
                    } catch (e: Exception) {
                        log.error("Error sending message to telegram", e)
                    }
                    return trade
                }
            }
        }

        return null
    }

    fun deposit(checkOrders: Boolean = true): Double {
        return instruments.map { instrument ->
            var i = price(instrument) * balance(instrument)
            if (checkOrders) {
                val openOrders = getOpenOrders(instrument)
                i += price(instrument) * openOrders.filter { it.side == Decision.SELL }.map { it.asset }.sum() + openOrders.filter { it.side == Decision.BUY }.map { it.asset * it.price }.sum()
            }
            i
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