package ru.efreet.trading.bars

import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by fluder on 19/02/2018.
 */
interface XBar {


    /**
     * @return the open price of the period
     */
    var openPrice: Float

    /**
     * @return the min price of the period
     */
    var minPrice: Float

    /**
     * @return the max price of the period
     */
    var maxPrice: Float

    /**
     * @return the close price of the period
     */
    var closePrice: Float

    /**
     * @return the whole traded volume in the period
     */
    var volume: Float

    /**
     * @return volume made by takers of base asset
     */
    var volumeBase: Float

    /**
     * @return volume made by takers of quote asset
     */
    var volumeQuote: Float

    /**
     * @return the number of trades in the period
     */
    var trades: Short

    /**
     * @return the time period of the bar
     */
    var timePeriod: Duration

    /**
     * @return the begin timestamp of the bar period
     */
    var beginTime: ZonedDateTime

    /**
     * @return the end timestamp of the bar period
     */
    var endTime: ZonedDateTime


    var delta5m: Float
    var delta15m: Float
    var delta1h: Float
    var delta1d: Float
    var delta7d: Float

    /**
     * @param timestamp a timestamp
     * @return true if the provided timestamp is between the begin time and the end time of the current period, false otherwise
     */
    fun inPeriod(timestamp: ZonedDateTime): Boolean {
        return !timestamp.isBefore(beginTime) && timestamp.isBefore(endTime)
    }

    /**
     * @return a human-friendly string of the end timestamp
     */
    fun getDateName(): String {
        return endTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    /**
     * @return a even more human-friendly string of the end timestamp
     */
    fun getSimpleDateName(): String {
        return endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    /**
     * @return true if this is a bearish bar, false otherwise
     */
    fun isBearish(): Boolean {
        val openPrice = openPrice
        val closePrice = closePrice
        return closePrice < openPrice
    }

    /**
     * @return true if this is a bullish bar, false otherwise
     */
    fun isBullish(): Boolean {
        val openPrice = openPrice
        val closePrice = closePrice
        return openPrice < closePrice
    }

    fun middlePrice(): Float {
        return (minPrice+maxPrice)/2
    }

    fun deltaMaxMin(): Float {
        return maxPrice-minPrice;
    }

    fun delta(): Float {
        return closePrice-openPrice;
    }

    fun minOpenClose(): Float {
        return minOf(openPrice,closePrice)
    }

    fun maxOpenClose(): Float {
        return maxOf(openPrice,closePrice)
    }

    fun closePriceInverted() : Double{
        return 100000000.0/closePrice
    }

    fun openPriceInverted() : Double{
        return 100000000.0/openPrice
    }

    fun minPriceInverted() : Double{
        return 100000000.0/maxPrice
    }

    fun maxPriceInverted() : Double{
        return 100000000.0/minPrice
    }

    fun contains(price : Double) : Boolean{
        return price in minPrice..maxPrice
    }

    /**
     * Adds a trade at the end of bar period.
     * @param tradeVolume the traded volume
     * @param tradePrice the price
     */
    fun addTrade(tradeVolume: String, tradePrice: String) {
        addTrade(tradeVolume.toFloat(), tradePrice.toFloat())
    }

    /**
     * Adds a trade at the end of bar period.
     * @param tradeVolume the traded volume
     * @param tradePrice the price
     */
    fun addTrade(tradeVolume: Float, tradePrice: Float) {
        if (openPrice == 0.0F) {
            openPrice = tradePrice
        }
        closePrice = tradePrice


        maxPrice = if (maxPrice < tradePrice) tradePrice else maxPrice

        minPrice = if (minPrice > tradePrice) tradePrice else minPrice

        volume = volume + tradeVolume
        trades++
    }
}

fun <T : XBar> List<T>.checkBars() {

    var prevBar: XBar? = null
    for (bar in this) {
        if (prevBar != null) {
            if (!bar.beginTime.isBefore(bar.endTime)) {
                println("INVALID BAR: $bar")
            }
            if (bar.beginTime.isBefore(prevBar.endTime)) {
                println("INVALID BARS: prev=$prevBar cur=$bar")
            }
        }
        prevBar = bar
    }
}

fun <T : XBar> List<T>.setDeltaXX(index: Int): T {
    val bar = get(index)

    getWithDelta(index, -5)?.let {
        bar.delta5m = bar.closePrice - it.closePrice
    }

    getWithDelta(index, -15)?.let {
        bar.delta15m = bar.closePrice - it.closePrice
    }

    getWithDelta(index, -60)?.let {
        bar.delta1h = bar.closePrice - it.closePrice
    }

    getWithDelta(index, -60 * 24)?.let {
        bar.delta1d = bar.closePrice - it.closePrice
    }

    getWithDelta(index, -60 * 24 * 7)?.let {
        bar.delta7d = bar.closePrice - it.closePrice
    }

    return bar
}

fun List<XBar>.setDeltaXX() {
    for (index in 0 until size) {
        setDeltaXX(index)
    }
}

fun List<XBar>.getWithDelta(index: Int, deltaMinutes: Long): XBar? {
    val bar = get(index)
    val n = deltaMinutes / bar.timePeriod.toMinutes()
    val i = index + n

    if (i < 0 || i >= size)
        return null

    val r = get(i.toInt())
    if (Duration.between(bar.endTime, r.endTime).toMinutes() == deltaMinutes)
        return r

    val bs = binarySearchBy(bar.endTime.plusMinutes(deltaMinutes), 0, index, selector = { it.endTime })
    return if (bs >= 0) {
        get(bs)
    } else {
        val nearest = get(-bs - 1)
        if (Math.abs(Duration.between(bar.endTime, nearest.endTime).toMinutes() - deltaMinutes) <= 2)
            nearest
        else
            null
    }
}

