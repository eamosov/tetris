package ru.efreet.trading.bars

import ru.efreet.trading.exchange.BarInterval
import java.time.ZonedDateTime

/**
 * Created by fluder on 25/02/2018.
 */
class XBarsAggregator(val barInterval: BarInterval) {

    private var lastBar: XBaseBar? = null

    fun addBars(bars: Collection<XBar>): List<XBar> {
        val ret = mutableListOf<XBar>()

        bars.forEach {
            addBar(it)?.let { ret.add(it) }
        }

        return ret
    }

    fun addBar(bar: XBar): XBar? {

        val closedBar = when {
            lastBar == null -> {
                openBar(bar.endTime)
                null
            }
            lastBar!!.inPeriod(bar.endTime) -> null
            else -> {
                val closedBar = lastBar
                openBar(bar.endTime)
                closedBar
            }
        }

        lastBar!!.addBar(bar)
        return closedBar
    }

    fun closeBar(): XBar? {
        val closedBar = lastBar
        lastBar = null
        return closedBar
    }

    private fun openBar(endTime: ZonedDateTime) {
        lastBar = XBaseBar(barInterval.duration, endTime.truncatedTo(barInterval).plus(barInterval.duration))
    }
}