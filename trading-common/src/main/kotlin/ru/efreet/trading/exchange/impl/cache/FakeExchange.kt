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

    init {
        setBalance("USDT", 1000.0)
    }

    fun setBalance(currency: String, value: Double) {
        balances[currency] = value
    }

    fun setTicker(instrument: Instrument, value: Double) {
        ticker[instrument] = Ticker(instrument, value, value)
    }

    override fun getName(): String {
        return _name
    }

    override fun getBalancesMap(): Map<String, Double> {
        return balances
    }

    override fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType): Order? {

        val base = balances[instrument.base] ?: 0.0
        val cost = price * asset
        if (cost > base){
            log.error("Couldn't buy {} {} for {}, not enough {} ({})", instrument.asset, asset, price, instrument.base, base)
            return null
        }

        setBalance(instrument.base!!, base - cost)
        setBalance(instrument.asset!!, (balances[instrument.asset!!] ?: 0.0) + asset * (1.0 - getFee() / 200.0))
        return Order(UUID.randomUUID().toString(), instrument, price, asset, type, Decision.BUY, ZonedDateTime.now())
    }

    override fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType): Order? {

        val myAsset = balances[instrument.asset] ?: 0.0
        if (asset > myAsset){
            log.error("Couldn't sell {} {} for {}, not enough {} ({})", instrument.asset, asset, price, instrument.asset, myAsset)
            return null
        }

        setBalance(instrument.base!!, (balances[instrument.base!!] ?: 0.0) + price * asset * (1.0 - getFee() / 200.0))
        setBalance(instrument.asset!!, myAsset - asset)
        return Order(UUID.randomUUID().toString(), instrument, price, asset, type, Decision.SELL, ZonedDateTime.now())

    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> {
        return arrayListOf()
    }

    override fun startTrade(instrument: Instrument, interval: BarInterval, consumer: (XBar, Boolean) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopTrade() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun getPricesMap(): Map<Instrument, Double> {
        return ticker.mapValues { it.value.highestBid }
    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        return listOf()
    }

    override fun cancelOrder(order: Order) {

    }
}