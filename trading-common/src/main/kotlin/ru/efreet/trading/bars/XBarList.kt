package ru.efreet.trading.bars

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.utils.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

val gmt = ZoneId.of("GMT")


fun ByteArrayList.setXBar(index: Int, v: XBar): Int {
    var i = index
    i += setFloat(i, v.openPrice)
    i += setFloat(i, v.minPrice)
    i += setFloat(i, v.maxPrice)
    i += setFloat(i, v.closePrice)
    i += setFloat(i, v.volume)
    i += setFloat(i, v.volumeBase)
    i += setFloat(i, v.volumeQuote)
    i += setShort(i, v.trades.toInt())
    i += setShort(i, (v.timePeriod.toMillis() / 1000).toInt())
    i += setLong(i, v.endTime.toEpochSecond())

    i += setFloat(i, v.delta5m)
    i += setFloat(i, v.delta15m)
    i += setFloat(i, v.delta1h)
    i += setFloat(i, v.delta1d)
    i += setFloat(i, v.delta7d)

    return i
}

const val OPEN_PRICE_OFFSET = 0
const val MIN_PRICE_OFFSET = OPEN_PRICE_OFFSET + 4
const val MAX_PRICE_OFFSET = MIN_PRICE_OFFSET + 4
const val CLOSE_PRICE_OFFSET = MAX_PRICE_OFFSET + 4
const val VOLUME_OFFSET = CLOSE_PRICE_OFFSET + 4
const val VOLUME_BASE_OFFSET = VOLUME_OFFSET + 4
const val VOLUME_QUOTE_OFFSET = VOLUME_BASE_OFFSET + 4
const val TRADES_OFFSET = VOLUME_QUOTE_OFFSET + 4
const val TIME_PERIOD_OFFSET = TRADES_OFFSET + 2
const val END_TIME_OFFSET = TIME_PERIOD_OFFSET + 2

const val DELTA5M_OFFSET = END_TIME_OFFSET + 8
const val DELTA15M_OFFSET = DELTA5M_OFFSET + 4
const val DELTA1H_OFFSET = DELTA15M_OFFSET + 4
const val DELTA1D_OFFSET = DELTA1H_OFFSET + 4
const val DELTA7D_OFFSET = DELTA1D_OFFSET + 4

class XBarRef(val b: ByteArrayList, val index: Int) : XBar {
    override var openPrice: Float
        get() = b.getFloat(index + OPEN_PRICE_OFFSET)
        set(value) {
            b.setFloat(index + OPEN_PRICE_OFFSET, value)
        }
    override var minPrice: Float
        get() = b.getFloat(index + MIN_PRICE_OFFSET)
        set(value) {
            b.setFloat(index + MIN_PRICE_OFFSET, value)
        }
    override var maxPrice: Float
        get() = b.getFloat(index + MAX_PRICE_OFFSET)
        set(value) {
            b.setFloat(index + MAX_PRICE_OFFSET, value)
        }
    override var closePrice: Float
        get() = b.getFloat(index + CLOSE_PRICE_OFFSET)
        set(value) {
            b.setFloat(index + CLOSE_PRICE_OFFSET, value)
        }
    override var volume: Float
        get() = b.getFloat(index + VOLUME_OFFSET)
        set(value) {
            b.setFloat(index + VOLUME_OFFSET, value)
        }
    override var volumeBase: Float
        get() = b.getFloat(index + VOLUME_BASE_OFFSET)
        set(value) {
            b.setFloat(index + VOLUME_BASE_OFFSET, value)
        }
    override var volumeQuote: Float
        get() = b.getFloat(index + VOLUME_QUOTE_OFFSET)
        set(value) {
            b.setFloat(index + VOLUME_QUOTE_OFFSET, value)
        }
    override var trades: Short
        get() = b.getShort(index + TRADES_OFFSET)
        set(value) {
            b.setShort(index + TRADES_OFFSET, value.toInt())
        }
    override var timePeriod: Duration
        get() = Duration.ofSeconds(b.getShort(index + TIME_PERIOD_OFFSET).toLong())
        set(value) {
            b.setShort(index + TIME_PERIOD_OFFSET, (value.toMillis() / 1000).toInt())
        }
    override var beginTime: ZonedDateTime
        get() = ZonedDateTime.ofInstant(Instant.ofEpochSecond(b.getLong(index + END_TIME_OFFSET) - b.getShort(index + TIME_PERIOD_OFFSET)), gmt)
        set(value) {
            throw NotImplementedError("Setting beginTime is not implemented")
        }
    override var endTime: ZonedDateTime
        get() = ZonedDateTime.ofInstant(Instant.ofEpochSecond(b.getLong(index + END_TIME_OFFSET)), gmt)
        set(value) {
            b.setLong(index + END_TIME_OFFSET, value.toEpochSecond())
        }

    override var delta5m: Float
        get() = b.getFloat(index + DELTA5M_OFFSET)
        set(value) {
            b.setFloat(index + DELTA5M_OFFSET, value)
        }
    override var delta15m: Float
        get() = b.getFloat(index + DELTA15M_OFFSET)
        set(value) {
            b.setFloat(index + DELTA15M_OFFSET, value)
        }
    override var delta1h: Float
        get() = b.getFloat(index + DELTA1H_OFFSET)
        set(value) {
            b.setFloat(index + DELTA1H_OFFSET, value)
        }
    override var delta1d: Float
        get() = b.getFloat(index + DELTA1D_OFFSET)
        set(value) {
            b.setFloat(index + DELTA1D_OFFSET, value)
        }
    override var delta7d: Float
        get() = b.getFloat(index + DELTA7D_OFFSET)
        set(value) {
            b.setFloat(index + DELTA7D_OFFSET, value)
        }

    override fun toString(): String {
        return "XBar(timePeriod=$timePeriod, endTime=$endTime, openPrice=$openPrice, maxPrice=$maxPrice, minPrice=$minPrice, closePrice=$closePrice, volume=$volume, volumeBase=$volumeBase, volumeQuote=$volumeQuote, trades=$trades, delta5m=$delta5m, delta15m=$delta15m, delta1h=$delta1h, delta1d=$delta1d, delta7d=$delta7d)"
    }
}

fun ByteArrayList.getXBar(index: Int): XBar {
    return XBarRef(this, index)
}

class XBarList : java.util.AbstractList<XBar>, RandomAccess {

    companion object {
        val xBarSize: Int = {
            val tmp = ByteArrayList(1000)
            tmp.expand(1000)

            tmp.setXBar(0, XBaseBar(BarInterval.ONE_MIN.duration, ZonedDateTime.now()))
        }()

        init {
            if (xBarSize != DELTA7D_OFFSET + 4) {
                throw IllegalArgumentException("Invalid xBarSize??, xBarSize=${xBarSize}, offset=${DELTA7D_OFFSET + 4}")
            }
        }
    }

    private val data: ByteArrayList

    constructor(capacity: Int) : super() {
        data = ByteArrayList(capacity * xBarSize)
    }

    constructor() : super() {
        data = ByteArrayList()
    }

    constructor (list: List<XBar>) {
        data = ByteArrayList(list.size * xBarSize)
        list.forEach { add(it) }
    }

    override fun add(element: XBar): Boolean {
        val index = data.size
        data.expand(xBarSize)
        data.setXBar(index, element)
        return true
    }

    override fun get(index: Int): XBar {

        if (index < 0 || index >= size)
            throw IndexOutOfBoundsException("Index ($index) is greater than or equal to list size ($size)")

        return data.getXBar(index * xBarSize)
    }

//    override fun removeAt(index: Int): XBar {
//        val startIndex = index * xBarSize
//        val endIndex = startIndex + xBarSize
//
//        for (i in startIndex until endIndex)
//            data.removeByte(i)
//
//        return super.removeAt(index)
//    }

    override val size: Int
        get() = data.size / xBarSize

}
