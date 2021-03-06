package ru.efreet.trading.bars

import ru.efreet.trading.Decision
import ru.efreet.trading.ta.indicators.BarGetterSetter
import ru.efreet.trading.ta.indicators.BarGetterSetter2
import java.time.Duration
import java.time.ZonedDateTime

fun List<XBar>.indexOf(endTime: ZonedDateTime): Int {
    var startIndex = this.binarySearchBy(endTime.toEpochSecond(), selector = { it.endTime.toEpochSecond() })
    if (startIndex < 0)
        startIndex = -startIndex - 1

    return startIndex
}

/**
 * Created by fluder on 19/02/2018.
 */
data class XExtBar(val bar: XBar) : XBar {

    override var openPrice: Float
        get() = bar.openPrice
        set(value) {
            bar.openPrice = value
        }
    override var minPrice: Float
        get() = bar.minPrice
        set(value) {
            bar.minPrice = value
        }
    override var maxPrice: Float
        get() = bar.maxPrice
        set(value) {
            bar.maxPrice = value
        }
    override var closePrice: Float
        get() = bar.closePrice
        set(value) {
            bar.closePrice = value
        }
    override var volume: Float
        get() = bar.volume
        set(value) {
            bar.volume = value
        }
    override var volumeBase: Float
        get() = bar.volumeBase
        set(value) {
            bar.volumeBase = value
        }
    override var volumeQuote: Float
        get() = bar.volumeQuote
        set(value) {
            bar.volumeQuote = value
        }
    override var trades: Short
        get() = bar.trades
        set(value) {
            bar.trades = value
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
    override var delta5m: Float
        get() = bar.delta5m
        set(value) {
            bar.delta5m = value
        }
    override var delta15m: Float
        get() = bar.delta15m
        set(value) {
            bar.delta15m = value
        }
    override var delta1h: Float
        get() = bar.delta1h
        set(value) {
            bar.delta1h = value
        }
    override var delta12h: Float
        get() = bar.delta12h
        set(value) {
            bar.delta12h = value
        }
    override var delta1d: Float
        get() = bar.delta1d
        set(value) {
            bar.delta1d = value
        }
    override var delta3d: Float
        get() = bar.delta3d
        set(value) {
            bar.delta3d = value
        }
    override var delta7d: Float
        get() = bar.delta7d
        set(value) {
            bar.delta7d = value
        }

    override fun addTrade(tradeVolume: Float, tradePrice: Float) {
        bar.addTrade(tradeVolume, tradePrice)
    }

    companion object {
        fun of(bars: List<XBar>): MutableList<XExtBar> {
            val ret = ArrayList<XExtBar>(bars.size)
            bars.forEach { ret.add(XExtBar(it)) }
            return ret
        }

        var _closePrice = BarGetterSetter<XExtBar>({ o, v -> o.closePrice = v }, { it.closePrice })
        var _volume = BarGetterSetter<XExtBar>({ o, v -> o.volume = v }, { it.volume })

        var _shortEma1 = BarGetterSetter<XExtBar>({ o, v -> o.shortEma1 = v }, { it.shortEma1 })
        var _shortEma2 = BarGetterSetter<XExtBar>({ o, v -> o.shortEma2 = v }, { it.shortEma2 })
        var _shortEma = BarGetterSetter<XExtBar>({ o, v -> o.shortEma = v }, { it.shortEma })

        var _longEma1 = BarGetterSetter<XExtBar>({ o, v -> o.longEma1 = v }, { it.longEma1 })
        var _longEma2 = BarGetterSetter<XExtBar>({ o, v -> o.longEma2 = v }, { it.longEma2 })
        var _longEma = BarGetterSetter<XExtBar>({ o, v -> o.longEma = v }, { it.longEma })

        var _shortEma1Sell = BarGetterSetter<XExtBar>({ o, v -> o.shortEma1Sell = v }, { it.shortEma1Sell })
        var _shortEma2Sell = BarGetterSetter<XExtBar>({ o, v -> o.shortEma2Sell = v }, { it.shortEma2Sell })
        var _shortEmaSell = BarGetterSetter<XExtBar>({ o, v -> o.shortEmaSell = v }, { it.shortEmaSell })

        var _longEma1Sell = BarGetterSetter<XExtBar>({ o, v -> o.longEma1Sell = v }, { it.longEma1Sell })
        var _longEma2Sell = BarGetterSetter<XExtBar>({ o, v -> o.longEma2Sell = v }, { it.longEma2Sell })
        var _longEmaSell = BarGetterSetter<XExtBar>({ o, v -> o.longEmaSell = v }, { it.longEmaSell })

        var _signalEma1 = BarGetterSetter<XExtBar>({ o, v -> o.signalEma1 = v }, { it.signalEma1 })
        var _signalEma2 = BarGetterSetter<XExtBar>({ o, v -> o.signalEma2 = v }, { it.signalEma2 })
        var _signalEma = BarGetterSetter<XExtBar>({ o, v -> o.signalEma = v }, { it.signalEma })


        var _signal2Ema1 = BarGetterSetter<XExtBar>({ o, v -> o.signal2Ema1 = v }, { it.signal2Ema1 })
        var _signal2Ema2 = BarGetterSetter<XExtBar>({ o, v -> o.signal2Ema2 = v }, { it.signal2Ema2 })
        var _signal2Ema = BarGetterSetter<XExtBar>({ o, v -> o.signal2Ema = v }, { it.signal2Ema })


        var _sma = BarGetterSetter<XExtBar>({ o, v -> o.sma = v }, { it.sma })
        var _sd = BarGetterSetter<XExtBar>({ o, v -> o.sd = v }, { it.sd })
        var _avrVolume = BarGetterSetter<XExtBar>({ o, v -> o.avrVolume = v }, { it.avrVolume })
        var _smaSell = BarGetterSetter<XExtBar>({ o, v -> o.smaSell = v }, { it.smaSell })
        var _sdSell = BarGetterSetter<XExtBar>({ o, v -> o.sdSell = v }, { it.sdSell })
        var _avrVolumeSell = BarGetterSetter<XExtBar>({ o, v -> o.avrVolumeSell = v }, { it.avrVolumeSell })

        var _dayShortEma = BarGetterSetter<XExtBar>({ o, v -> o.dayShortEma = v }, { it.dayShortEma })
        var _dayLongEma = BarGetterSetter<XExtBar>({ o, v -> o.dayLongEma = v }, { it.dayLongEma })
        var _daySignalEma = BarGetterSetter<XExtBar>({ o, v -> o.daySignalEma = v }, { it.daySignalEma })
        var _daySignal2Ema = BarGetterSetter<XExtBar>({ o, v -> o.daySignal2Ema = v }, { it.daySignal2Ema })
        var _stohastic = BarGetterSetter2<XExtBar, Decision>({ o, v -> o.stohastic = v }, { it.stohastic })

        var _lastDecision = BarGetterSetter2<XExtBar, Pair<Decision, Map<String, String>>>({ o, v -> o.lastDecision = v }, { it.lastDecision })
        var _decisionStart = BarGetterSetter2<XExtBar, XExtBar>({ o, v -> o.decisionStart = v }, { it.decisionStart })
        var _tslIndicator = BarGetterSetter<XExtBar>({ o, v -> o.tslIndicator = v }, { it.tslIndicator })
        var _soldBySLIndicator = BarGetterSetter2<XExtBar, Boolean>({ o, v -> o.soldBySLIndicator = v }, { it.soldBySLIndicator })
    }

    var shortEma1: Float = Float.MAX_VALUE
    var shortEma2: Float = Float.MAX_VALUE
    var shortEma: Float = Float.MAX_VALUE

    var longEma1: Float = Float.MAX_VALUE
    var longEma2: Float = Float.MAX_VALUE
    var longEma: Float = Float.MAX_VALUE

    var shortEma1Sell: Float = Float.MAX_VALUE
    var shortEma2Sell: Float = Float.MAX_VALUE
    var shortEmaSell: Float = Float.MAX_VALUE

    var longEma1Sell: Float = Float.MAX_VALUE
    var longEma2Sell: Float = Float.MAX_VALUE
    var longEmaSell: Float = Float.MAX_VALUE

    var signalEma1: Float = Float.MAX_VALUE
    var signalEma2: Float = Float.MAX_VALUE
    var signalEma: Float = Float.MAX_VALUE

    var signal2Ema1: Float = Float.MAX_VALUE
    var signal2Ema2: Float = Float.MAX_VALUE
    var signal2Ema: Float = Float.MAX_VALUE

    var sma: Float = Float.MAX_VALUE
    var sd: Float = Float.MAX_VALUE
    var avrVolume: Float = Float.MAX_VALUE

    var smaSell: Float = Float.MAX_VALUE
    var sdSell: Float = Float.MAX_VALUE
    var avrVolumeSell: Float = Float.MAX_VALUE

    var sma2: Float = Float.MAX_VALUE
    var sd2: Float = Float.MAX_VALUE
    var avrVolume2: Float = Float.MAX_VALUE

    var smaSell2: Float = Float.MAX_VALUE
    var sdSell2: Float = Float.MAX_VALUE
    var avrVolumeSell2: Float = Float.MAX_VALUE


    var dayShortEma: Float = Float.MAX_VALUE
    var dayLongEma: Float = Float.MAX_VALUE
    var daySignalEma: Float = Float.MAX_VALUE
    var daySignal2Ema: Float = Float.MAX_VALUE

    var stohastic: Decision = Decision.NONE

    var lastDecision: Pair<Decision, Map<String, String>>? = null
    var decisionStart: XExtBar? = null
    var tslIndicator: Float = Float.MAX_VALUE
    var soldBySLIndicator: Boolean? = null
}