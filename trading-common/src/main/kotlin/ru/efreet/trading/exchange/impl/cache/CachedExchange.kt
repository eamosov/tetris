package ru.efreet.trading.exchange.impl.cache

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import java.time.ZonedDateTime

/**
 * Created by fluder on 11/02/2018.
 */
class CachedExchange(_name: String, _fee: Float, interval: BarInterval, val cache: BarsCache) : FakeExchange(_name, _fee, interval) {


    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        return cache.getBars(_name, instrument, interval, startTime, endTime)
    }

}