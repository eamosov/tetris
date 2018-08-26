package ru.efreet.trading.bars

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.impl.cache.BarsCache
import java.time.ZonedDateTime

class MarketBarFactory(val cache: BarsCache, val interval: BarInterval = BarInterval.ONE_MIN, val exchange: String = "binance") {

    private data class Holder(val bars: List<XBar>, var index: Int = -1)

    fun build(start: ZonedDateTime, end: ZonedDateTime): List<MarketBar> {

        val map = mutableMapOf<Instrument, Holder>()

        val instruments = cache.getInstruments(exchange, interval).filter { it.base == "USDT" }

        //println("instruments: $instruments")

        for (instrument in instruments) {
            map[instrument] = Holder(cache.getBars(exchange, instrument, interval, start.minusDays(7), end))
        }

        val btcBars = map[Instrument.BTC_USDT]!!

        val deltaList = arrayListOf<MarketBar>()

        for (index in 0 until btcBars.bars.size) {

            val btcBar = btcBars.bars.fillDelta(index)
            val endTime = btcBar.endTime

            val delta = MarketBar(endTime)
            delta.addBar(btcBar)
            //println("BTC: ${btcBar}")

            for (instrument in instruments.filter { it != Instrument.BTC_USDT }) {
                val holder = map[instrument]!!

                var bar: XBar?

                if (holder.index == holder.bars.size - 1) {
                    bar = null
                } else {
                    bar = if (holder.index >= 0) {
                        holder.bars[holder.index + 1]
                    } else null

                    if (bar != null && bar.endTime == endTime) {
                        holder.index++
                    } else {
                        val bsr = holder.bars.binarySearchBy(endTime, holder.index + 1, holder.bars.size, selector = { it.endTime })
                        if (bsr >= 0) {
                            //println("binarySearchBy success")
                            holder.index = bsr
                            bar = holder.bars[bsr]
                        }
                    }
                }

                if (bar != null) {
                    bar = holder.bars.fillDelta(holder.index)
                    delta.addBar(bar)
                    //println("${instrument} $bar")
                } else {
                    //println("Could'n find bar ${btcBar.endTime} for ${instrument}")
                }
            }

            if (!delta.endTime.isBefore(start)) {
                deltaList.add(delta)
            }
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