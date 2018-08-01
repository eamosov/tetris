package ru.efreet.trading.exchange.impl.cache

import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.*
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by fluder on 11/02/2018.
 */
open class FakeExchange(val _name: String, val _fee: Double, val interval: BarInterval) : Exchange {

    private val log = LoggerFactory.getLogger(FakeExchange::class.java)

    private val balances = mutableMapOf<String, Double>()
    private val ticker = mutableMapOf<Instrument, Ticker>()
    private val orders = mutableMapOf<String, Order>()

    init {
        setBalance("USDT", 1000.0)
    }

    fun setBalance(currency: String, value: Double) {
        balances[currency] = value
    }

    fun setTicker(instrument: Instrument, bar: XBar) {
        ticker[instrument] = Ticker(instrument, bar.closePrice, bar.closePrice)

        val it = orders.iterator()
        while (it.hasNext()) {
            val order = it.next().value

            if (order.side == Decision.BUY && order.price > bar.minPrice) {
                execOrder(order)
                it.remove()
            } else if (order.side == Decision.SELL && order.price < bar.maxPrice) {
                execOrder(order)
                it.remove()
            }
        }
    }

    override fun getName(): String {
        return _name
    }

    override fun getBalancesMap(): Map<String, Double> {
        return balances
    }

    override fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType, now:ZonedDateTime): Order? {

        val base = balances[instrument.base] ?: 0.0
        val cost = price * asset
        if (cost > base || asset <= 0.0 || price <= 0.0) {
            log.error("Couldn't buy {} {} for {}, not enough {} ({})", instrument.asset, asset, price, instrument.base, base)
            return null
        }

        setBalance(instrument.base, base - cost)
        //setBalance(instrument.asset, (balances[instrument.asset] ?: 0.0) + asset * (1.0 - getFee() / 200.0))
        val order = Order(UUID.randomUUID().toString(), instrument, price, asset, type, Decision.BUY, now)

        if (type == OrderType.MARKET)
            execOrder(order)
        else
            orders[order.orderId] = order

        return order
    }

    override fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType, now:ZonedDateTime): Order? {

        val myAsset = balances[instrument.asset] ?: 0.0
        if (asset > myAsset || asset <= 0.0 || price <= 0.0) {
            log.error("Couldn't sell {} {} for {}, not enough {} ({})", instrument.asset, asset, price, instrument.asset, myAsset)
            return null
        }

        //setBalance(instrument.base, (balances[instrument.base] ?: 0.0) + price * asset * (1.0 - getFee() / 200.0))
        setBalance(instrument.asset, myAsset - asset)
        val order = Order(UUID.randomUUID().toString(), instrument, price, asset, type, Decision.SELL, now)

        if (type == OrderType.MARKET)
            execOrder(order)
        else
            orders[order.orderId] = order

        return order
    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> {
        return arrayListOf()
    }

    override fun getFee(): Double {
        return _fee
    }

    override fun getIntervals(): List<BarInterval> {
        return arrayListOf(interval)
    }

    override fun getTicker(): Map<Instrument, Ticker> {
        return ticker
    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        return orders.values.filter { it.instrument == instrument } .toList()
    }

    override fun cancelOrder(order: Order) {
        val order = orders.remove(order.orderId) ?: return
        if (order.side == Decision.BUY) {
            setBalance(order.instrument.base, balances[order.instrument.base]!! + order.price * order.asset)
        } else if (order.side == Decision.SELL) {
            setBalance(order.instrument.asset, balances[order.instrument.asset]!! + order.asset)
        }
    }

    private fun execOrder(order: Order) {
        if (order.side == Decision.BUY) {
            setBalance(order.instrument.asset,
                    (balances[order.instrument.asset] ?: 0.0) + order.asset * (1.0 - getFee() / 200.0))
        } else if (order.side == Decision.SELL) {
            setBalance(order.instrument.base,
                    (balances[order.instrument.base] ?: 0.0) + order.price * order.asset * (1.0 - getFee() / 200.0))
        }
    }
}