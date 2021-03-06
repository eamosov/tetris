package ru.efreet.trading.bot

import org.slf4j.LoggerFactory
import ru.efreet.telegram.Telegram
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.*
import ru.efreet.trading.exchange.impl.cache.FakeExchange
import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.roundAmount
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 23/02/2018.
 */
class Trader(val tradeRecordDao: TradeRecordDao?,
             val exchange: Exchange,
             val limit: Float,
             val instruments: Map<Instrument, Float> = mapOf(Pair(Instrument.ETH_USDT, 0.5F)),
             val telegram: Telegram? = null,
             val keepBnb: Float = 1.0F,
             val updateTickerTimeout: Int = 60000,
             val updateBalancesTimeout: Int = 60000
) {

    private val iTradeHistory: MutableMap<Instrument, ITradeHistory> = mutableMapOf()
    private val cash: MutableList<Pair<ZonedDateTime, Float>> = arrayListOf()


    private fun iTradeHistory(instrument: Instrument): ITradeHistory {
        return iTradeHistory.computeIfAbsent(instrument) { ITradeHistory() }
    }

    private val startUsd: Float
    private val startDeposit: Float

    val usd: Float get() = balance("USDT")

    private lateinit var ticker: Map<Instrument, Ticker>
    private var lastTicker: ZonedDateTime = ZonedDateTime.now()

    lateinit var balances: MutableMap<String, Float>
    private var lastBalances: ZonedDateTime = ZonedDateTime.now()


    private val lastBuy = mutableMapOf<Instrument, XBar>()

    init {

        //Balances in USD
        updateTicker(true)
        updateBalance(true)

        startUsd = usd
        startDeposit = deposit()

        instruments.forEach { it, _ ->
            iTradeHistory(it).startAsset = balance(it)
            iTradeHistory(it).startPrice = price(it)
        }
    }

    private fun updateTicker(force: Boolean = false) {
        if (force || Duration.between(lastTicker, ZonedDateTime.now()).toMillis() > updateTickerTimeout) {
            ticker = exchange.getTicker()
            lastTicker = ZonedDateTime.now()
        }
    }

    private fun updateBalance(force: Boolean = false) {
        if (force || Duration.between(lastBalances, ZonedDateTime.now()).toMillis() > updateBalancesTimeout) {
            balances = exchange.getBalancesMap().toMutableMap()
            lastBalances = ZonedDateTime.now()
        }
    }

    fun balance(instrument: Instrument): Float {
        return balance(instrument.asset)
    }

    private fun balance(currency: String): Float {
        val value = balances[currency] ?: 0.0F
        return if (currency == "BNB")
            maxOf(0.0F, value - keepBnb)
        else
            value
    }

    fun executeAdvice(logicAdvice: BotAdvice): TradeRecord? {

        var advice = logicAdvice;

        if (exchange is FakeExchange) {
            exchange.setTicker(advice.instrument, advice.bar)
        }

        val needForcedUpdate = logicAdvice.decision == Decision.BUY || (logicAdvice.decision == Decision.SELL && balance(advice.instrument) * advice.price >= 10)
        updateTicker(needForcedUpdate)
        updateBalance(needForcedUpdate)

        val td = iTradeHistory(advice.instrument)

        if (!td.isStartInitialized())
            td.startTime = advice.bar.endTime

        td.endTime = advice.bar.endTime

        td.closePrice = advice.bar.closePrice

        if (td.startPrice == 0.0F) {
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

        cash.add(Pair(advice.bar.endTime, deposit((exchange is FakeExchange) || needForcedUpdate)))

//        if (advice.decision == Decision.NONE &&
//                lastBuy.containsKey(advice.instrument) &&
//                advice.bar.closePrice < lastBuy[advice.instrument]!!.closePrice * 0.995) {
//
//            advice = BotAdvice(advice.time, Decision.BUY, mapOf(Pair("step", "1")), advice.instrument, advice.bar.closePrice, advice.bar, advice.indicators)
//        }


        if (advice.decision == Decision.BUY) {

            if (cancelAllOrders(advice.instrument))
                updateBalance(true)

            val openOrders = getOpenOrders()

            //usd + все наблюдаемые валюты
            val deposit = deposit(openOrders = openOrders)

            //сколько боту осталось доступно USD
            val availableUsd = usd - deposit * (1.0F - limit)

            //максимальный размер ставки на одну валюту
            val maxBet = deposit * limit * instruments[advice.instrument]!!

            //сколько уже поставили на одну валюту
            val myBet = balance(advice.instrument) * price(advice.instrument)

            //сколько ещё можем доставить?
            //val asset = minOf(maxBet - myBet, availableUsd, maxBet * 0.5) / advice.price
            val asset = minOf(maxBet - myBet, availableUsd) / advice.price

            if (asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val quantity = roundAmount(asset, advice.price)
                val order = exchange.buy(advice.instrument, quantity, advice.price, OrderType.LIMIT, advice.time)

                updateBalance(true)
                openOrders[advice.instrument] = getOpenOrders(advice.instrument)

                val trade = TradeRecord(order.orderId, order.time, exchange.getName(), order.instrument.toString(), order.price,
                        advice.decision, advice.decisionArgs, order.type, order.asset,
                        exchange.getFee() / 200.0F,
                        usdBefore,
                        assetBefore,
                        balance(advice.instrument.base),
                        balance(advice.instrument.asset))

                iTradeHistory(advice.instrument).trades.add(trade)
                tradeRecordDao?.create(trade)

                lastBuy[advice.instrument] = advice.bar

                try {
                    val message = "BUY ${trade.amount} ${order.instrument.asset} for ${trade.price} ${order.instrument.base}, total: ${deposit(openOrders = openOrders).round2()}$, BNB: ${balances["BNB"]?.round2()
                            ?: 0.0F}"
                    log.info("telegram: {}", message)
                    telegram?.sendMessage(message)
                } catch (e: Exception) {
                    log.error("Error sending message to telegram", e)
                }
                return trade
            }
        } else if (advice.decision == Decision.SELL) {

            lastBuy.remove(advice.instrument)

            if (cancelAllOrders(advice.instrument))
                updateBalance(true)

            val asset = balance(advice.instrument)

            if (asset * advice.price >= 10) {

                val usdBefore = balance(advice.instrument.base)
                val assetBefore = balance(advice.instrument.asset)
                val order = exchange.sell(advice.instrument, roundAmount(asset, advice.price), advice.price, OrderType.LIMIT, advice.time)

                updateBalance(true)

                val trade = TradeRecord(order.orderId, order.time, exchange.getName(), order.instrument.toString(), order.price,
                        advice.decision, advice.decisionArgs, order.type, order.asset,
                        exchange.getFee() / 200.0F,
                        usdBefore,
                        assetBefore,
                        balance(advice.instrument.base),
                        balance(advice.instrument.asset))

                iTradeHistory(advice.instrument).trades.add(trade)
                tradeRecordDao?.create(trade)
                try {
                    val message = "SELL ${trade.amount} ${order.instrument.asset} for ${trade.price} ${order.instrument.base}, total: ${deposit().round2()}$, BNB: ${balances["BNB"]?.round2()
                            ?: 0.0F}"
                    log.info("telegram: {}", message)

                    telegram?.sendMessage(message)
                } catch (e: Exception) {
                    log.error("Error sending message to telegram", e)
                }
                return trade
            }
        }

        return null
    }

    fun deposit(checkOrders: Boolean = true, openOrders: Map<Instrument, List<Order>>? = null): Float {

        val _openOrders = openOrders ?: if (checkOrders) {
            getOpenOrders()
        } else {
            null
        }

        return instruments.map { (instrument, _) ->
            var i = price(instrument) * balance(instrument)
            if (_openOrders != null) {
                i += price(instrument) * _openOrders[instrument]!!.filter { it.side == Decision.SELL }.map { it.asset }.sum() + _openOrders[instrument]!!.filter { it.side == Decision.BUY }.map { it.asset * it.price }.sum()
            }
            i
        }.sum() + usd
    }

    fun price(instrument: Instrument): Float {
        return ticker[instrument]?.highestBid ?: 0.0F
    }

    fun getOpenOrders(instrument: Instrument): List<Order> {
        return exchange.getOpenOrders(instrument)
    }

    fun getOpenOrders(): MutableMap<Instrument, List<Order>> {
        return instruments.mapValues { exchange.getOpenOrders(it.key) }.toMutableMap()
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

        val openOrders = getOpenOrders()

        for ((i, _) in instruments) {
            log.info("{}: {} ({} USDT), orders={}", i.asset, balance(i), (balance(i) * price(i)).round2(), openOrders[i])
        }

        log.info("total: {}$, BNB: {}", deposit(openOrders = openOrders).round2(), balances["BNB"]?.round2() ?: 0.0F)
    }

    companion object {
        fun fakeTrader(feeP: Float, interval: BarInterval, instrument: Instrument): Trader {
            return Trader(null, FakeExchange("fake", feeP, interval), 1.0F, mapOf(Pair(instrument, 1.0F)), updateBalancesTimeout = -1, updateTickerTimeout = -1)
        }

        val log = LoggerFactory.getLogger(Trader::class.java)
    }
}