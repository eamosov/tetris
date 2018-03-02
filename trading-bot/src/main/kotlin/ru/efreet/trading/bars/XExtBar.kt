package ru.efreet.trading.bars

import ru.efreet.trading.ta.indicators.BarGetterSetter
import java.time.Duration
import java.time.ZonedDateTime

fun List<XExtBar>.indexOf(endTime: ZonedDateTime): Int {
    var startIndex = this.binarySearchBy(endTime, selector = { it.endTime })
    if (startIndex < 0)
        startIndex = -startIndex - 1

    return startIndex
}

/**
 * Created by fluder on 19/02/2018.
 */
data class XExtBar(val bar: XBar) : XBar {

    override var openPrice: Double
        get() = bar.openPrice
        set(value) {
            bar.openPrice = value
        }
    override var minPrice: Double
        get() = bar.minPrice
        set(value) {
            bar.minPrice = value
        }
    override var maxPrice: Double
        get() = bar.maxPrice
        set(value) {
            bar.maxPrice = value
        }
    override var closePrice: Double
        get() = bar.closePrice
        set(value) {
            bar.closePrice = value
        }
    override var volume: Double
        get() = bar.volume
        set(value) {
            bar.volume = value
        }
    override var trades: Int
        get() = bar.trades
        set(value) {
            bar.trades = value
        }
    override var amount: Double
        get() = bar.amount
        set(value) {
            bar.amount = value
        }
    override var timePeriod: Duration
        get() = bar.timePeriod
        set(value) {
            bar.timePeriod = value
        }
    override var beginTime: ZonedDateTime
        get() = bar.beginTime
        set(value) {
            bar.beginTime = value
        }
    override var endTime: ZonedDateTime
        get() = bar.endTime
        set(value) {
            bar.endTime = value
        }

    override fun addTrade(tradeVolume: Double, tradePrice: Double) {
        bar.addTrade(tradeVolume, tradePrice)
    }

    companion object {
        fun of(bars: List<XBar>): MutableList<XExtBar> {
            val ret = ArrayList<XExtBar>(bars.size)
            bars.forEach { ret.add(XExtBar(it)) }
            return ret
        }

        var _shortEma1 = BarGetterSetter<XExtBar>({ o, v -> o.shortEma1 = v }, { it.shortEma1 })
        var _shortEma2 = BarGetterSetter<XExtBar>({ o, v -> o.shortEma2 = v }, { it.shortEma2 })
        var _shortEma = BarGetterSetter<XExtBar>({ o, v -> o.shortEma = v }, { it.shortEma })

        var _longEma1 = BarGetterSetter<XExtBar>({ o, v -> o.longEma1 = v }, { it.longEma1 })
        var _longEma2 = BarGetterSetter<XExtBar>({ o, v -> o.longEma2 = v }, { it.longEma2 })
        var _longEma = BarGetterSetter<XExtBar>({ o, v -> o.longEma = v }, { it.longEma })

        var _signalEma1 = BarGetterSetter<XExtBar>({ o, v -> o.signalEma1 = v }, { it.signalEma1 })
        var _signalEma2 = BarGetterSetter<XExtBar>({ o, v -> o.signalEma2 = v }, { it.signalEma2 })
        var _signalEma = BarGetterSetter<XExtBar>({ o, v -> o.signalEma = v }, { it.signalEma })

        var _sma = BarGetterSetter<XExtBar>({ o, v -> o.sma = v }, { it.sma })
        var _sd = BarGetterSetter<XExtBar>({ o, v -> o.sd = v }, { it.sd })

        var _dayShortEma = BarGetterSetter<XExtBar>({ o, v -> o.dayShortEma = v }, { it.dayShortEma })
        var _dayLongEma = BarGetterSetter<XExtBar>({ o, v -> o.dayLongEma = v }, { it.dayLongEma })
        var _daySignalEma = BarGetterSetter<XExtBar>({ o, v -> o.daySignalEma = v }, { it.daySignalEma })
        var _daySignal2Ema = BarGetterSetter<XExtBar>({ o, v -> o.daySignal2Ema = v }, { it.daySignal2Ema })
    }

    var shortEma1: Double = Double.MAX_VALUE
    var shortEma2: Double = Double.MAX_VALUE
    var shortEma: Double = Double.MAX_VALUE

    var longEma1: Double = Double.MAX_VALUE
    var longEma2: Double = Double.MAX_VALUE
    var longEma: Double = Double.MAX_VALUE

    var signalEma1: Double = Double.MAX_VALUE
    var signalEma2: Double = Double.MAX_VALUE
    var signalEma: Double = Double.MAX_VALUE

    var sma: Double = Double.MAX_VALUE
    var sd: Double = Double.MAX_VALUE

    var dayShortEma: Double = Double.MAX_VALUE
    var dayLongEma: Double = Double.MAX_VALUE
    var daySignalEma: Double = Double.MAX_VALUE
    var daySignal2Ema: Double = Double.MAX_VALUE
}