package ru.efreet.trading.exchange.impl.cache

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.*
import java.time.ZonedDateTime

/**
 * Created by fluder on 11/02/2018.
 */
class CachedExchange(val _name:String, val _fee:Double, val interval: BarInterval, val cache: BarsCache) : Exchange {

    override fun getName(): String {
        return _name
    }

    override fun getBalancesMap(): Map<String, Double> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        return cache.getBars(_name, instrument, interval, startTime, endTime)
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> {
        return arrayListOf()
    }

    override fun startTrade(instrument: Instrument, consumer: (AggTrade) -> Unit) {
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
        return hashMapOf()
    }
}