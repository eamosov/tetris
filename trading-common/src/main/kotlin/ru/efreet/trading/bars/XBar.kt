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
    var openPrice: Double

    /**
     * @return the min price of the period
     */
    var minPrice: Double

    /**
     * @return the max price of the period
     */
    var maxPrice: Double

    /**
     * @return the close price of the period
     */
    var closePrice: Double

    /**
     * @return the whole traded volume in the period
     */
    var volume: Double

    /**
     * @return volume made by takers of base asset
     */
    var volumeBase: Double

    /**
     * @return volume made by takers of quote asset
     */
    var volumeQuote: Double

    /**
     * @return the number of trades in the period
     */
    var trades: Int

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

    fun middlePrice() : Double {
        return (minPrice+maxPrice)/2
    }

    fun deltaMaxMin() : Double {
        return maxPrice-minPrice;
    }

    fun delta() : Double {
        return closePrice-openPrice;
    }

    /**
     * Adds a trade at the end of bar period.
     * @param tradeVolume the traded volume
     * @param tradePrice the price
     */
    fun addTrade(tradeVolume: String, tradePrice: String) {
        addTrade(tradeVolume.toDouble(), tradePrice.toDouble())
    }

    /**
     * Adds a trade at the end of bar period.
     * @param tradeVolume the traded volume
     * @param tradePrice the price
     */
    abstract fun addTrade(tradeVolume: Double, tradePrice: Double)
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