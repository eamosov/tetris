package ru.efreet.trading.bars

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.utils.trimToBar
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.reflect.KMutableProperty1

fun List<Instrument>.marketBarList(): List<Instrument> {
    return this.filter { it.base == "USDT" && it.asset != "TUSD" && it.asset != "VEN" }
}

class MarketBarFactory(val cache: BarsCache, val interval: BarInterval = BarInterval.ONE_MIN, val exchange: String = "binance") {

    private data class Holder(val bars: List<XBar>, var index: Int = -1)

    private val instruments = cache.getInstruments(exchange, interval).marketBarList()

    fun setDeltaXX(bar: XBar, instrument: Instrument, interval: BarInterval, prop: KMutableProperty1<XBar, Float>, duration: Duration) {
        cache.getBar(exchange, instrument, interval, bar.endTime.minus(duration))?.let {
            prop.set(bar, bar.closePrice - it.closePrice)
        }
    }

    fun setDeltaXX(bar: XBar, instrument: Instrument, interval: BarInterval) {
        setDeltaXX(bar, instrument, interval, XBar::delta5m, Duration.ofMinutes(5))
        setDeltaXX(bar, instrument, interval, XBar::delta15m, Duration.ofMinutes(15))
        setDeltaXX(bar, instrument, interval, XBar::delta1h, Duration.ofHours(1))
        setDeltaXX(bar, instrument, interval, XBar::delta12h, Duration.ofHours(12))
        setDeltaXX(bar, instrument, interval, XBar::delta1d, Duration.ofDays(1))
        setDeltaXX(bar, instrument, interval, XBar::delta3d, Duration.ofDays(3))
        setDeltaXX(bar, instrument, interval, XBar::delta7d, Duration.ofDays(7))
    }

    fun build(time: ZonedDateTime): MarketBar {

        val mb = MarketBar(time)
        val time = time.trimToBar()

        for (instrument in instruments) {

            cache.getBar(exchange, instrument, interval, time)?.let {
                setDeltaXX(it, instrument, interval)
                mb.addBar(it)
            }
        }

        return mb
    }

    fun trim(marketBars: List<MarketBar>, bars: List<XBar>): List<MarketBar> {

        val out = ArrayList<MarketBar>(bars.size)
        var mbIndex = -1

        for (index in 0 until bars.size) {

            var mb: MarketBar? = null
            val endTime = bars[index].endTime

            if (mbIndex < marketBars.size - 1) {

                if (mbIndex >= 0 && marketBars[mbIndex + 1].endTime.isEqual(endTime)) {
                    mbIndex += 1
                    mb = marketBars[mbIndex]
                } else {
                    val bsr = marketBars.binarySearchBy(endTime.toEpochSecond(), mbIndex + 1, marketBars.size, selector = { it.endTime.toEpochSecond() })
                    if (bsr >= 0) {
                        mbIndex = bsr
                        mb = marketBars[mbIndex]
                    }
                }
            }

            if (mb == null)
                throw RuntimeException("Couldn't find MarketBar for $endTime")

            out.add(mb)
        }

        if (out.size != bars.size)
            throw RuntimeException("out.size != bars.size")

        for (index in 0 until bars.size) {
            if (!bars[index].endTime.isEqual(out[index].endTime)) {
                throw RuntimeException("bars[$index].endTime(${bars[index].endTime}) != out[$index].endTime(${out[index].endTime})")
            }
        }

        return out
    }

    fun build(start: ZonedDateTime, end: ZonedDateTime): List<MarketBar> {

        val map = mutableMapOf<Instrument, Holder>()

        //println("instruments: $instruments")

        for (instrument in instruments) {
            map[instrument] = Holder(cache.getBars(exchange, instrument, interval, start.minusDays(7), end))
        }

        val deltaList = arrayListOf<MarketBar>()

        var endTime = start.trimToBar()
        while (!endTime.isAfter(end.trimToBar())) {

            val delta = MarketBar(endTime)

            for (instrument in instruments) {
                val holder = map[instrument]!!

                var bar: XBar? = null

                if (holder.index < holder.bars.size - 1) {
                    if (holder.index >= 0 && holder.bars[holder.index + 1].endTime.isEqual(endTime)) {
                        holder.index = holder.index + 1
                        bar = holder.bars[holder.index]
                    } else {
                        val bsr = holder.bars.binarySearchBy(endTime.toEpochSecond(), holder.index + 1, holder.bars.size, selector = { it.endTime.toEpochSecond() })
                        if (bsr >= 0) {
                            //println("binarySearchBy success")
                            holder.index = bsr
                            bar = holder.bars[holder.index]
                        }
                    }
                }

                if (bar != null) {
                    bar = holder.bars.setDeltaXX(holder.index)
                    delta.addBar(bar)
                    //println("${instrument} $bar")
                } else {
                    //println("Could'n find bar ${btcBar.endTime} for ${instrument}")
                }
            }

            if (delta.count > 0) {
                deltaList.add(delta)
            }

            endTime = endTime.plus(interval.duration)
        }

        return deltaList
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val cache = BarsCache("cache.sqlite3")

            val mbf = MarketBarFactory(cache)

            val start = System.currentTimeMillis()
            val bars = mbf.build(ZonedDateTime.now().minusDays(180), ZonedDateTime.now())
            val end = System.currentTimeMillis()

            println((end - start) / 1000)
            //println(bars.map { Triple(it.endTime, it.delta1h, it.p1h()) })
        }
    }
}