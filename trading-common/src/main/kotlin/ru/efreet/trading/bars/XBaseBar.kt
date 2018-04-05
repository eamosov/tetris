package ru.efreet.trading.bars

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 19/02/2018.
 */
data class XBaseBar(override var timePeriod: Duration,
                    override var endTime: ZonedDateTime,
                    override var openPrice: Double = 0.0,
                    override var maxPrice: Double = 0.0,
                    override var minPrice: Double = Double.MAX_VALUE,
                    override var closePrice: Double = 0.0,
                    override var volume: Double = 0.0,
                    override var trades:Int = 0) : XBar {

    /** Begin time of the bar  */
    override var beginTime: ZonedDateTime = endTime.minus(timePeriod)

    /**
     * Constructor.
     * @param endTime the end time of the bar period
     * @param openPrice the open price of the bar period
     * @param highPrice the highest price of the bar period
     * @param lowPrice the lowest price of the bar period
     * @param closePrice the close price of the bar period
     * @param volume the volume of the bar period
     */
    constructor(endTime: ZonedDateTime, openPrice: String, highPrice: String, lowPrice: String, closePrice: String, volume: String) :
            this(endTime, openPrice.toDouble(),
                    highPrice.toDouble(),
                    lowPrice.toDouble(),
                    closePrice.toDouble(),
                    volume.toDouble())


    /**
     * Constructor.
     * @param endTime the end time of the bar period
     * @param openPrice the open price of the bar period
     * @param highPrice the highest price of the bar period
     * @param lowPrice the lowest price of the bar period
     * @param closePrice the close price of the bar period
     * @param volume the volume of the bar period
     */
    constructor(endTime: ZonedDateTime, openPrice: Double, highPrice: Double, lowPrice: Double, closePrice: Double, volume: Double) :
            this(Duration.ofDays(1), endTime, openPrice, highPrice, lowPrice, closePrice, volume)


    /**
     * Constructor.
     * @param timePeriod the time period
     * @param endTime the end time of the bar period
     * @param openPrice the open price of the bar period
     * @param highPrice the highest price of the bar period
     * @param lowPrice the lowest price of the bar period
     * @param closePrice the close price of the bar period
     * @param volume the volume of the bar period
     */
    constructor(timePeriod: Duration, endTime: ZonedDateTime, openPrice: Double, highPrice: Double, lowPrice: Double, closePrice: Double, volume: Double) :
            this(timePeriod, endTime, openPrice, highPrice, lowPrice, closePrice, volume, 0)

    constructor(bar: XBar) :
            this(bar.timePeriod, bar.endTime, bar.openPrice, bar.maxPrice, bar.minPrice, bar.closePrice, bar.volume, bar.trades)


    /**
     * Adds a trade at the end of bar period.
     * @param tradeVolume the traded volume
     * @param tradePrice the price
     */
    override fun addTrade(tradeVolume: Double, tradePrice: Double) {
        if (openPrice == 0.0) {
            openPrice = tradePrice
        }
        closePrice = tradePrice


        maxPrice = if (maxPrice < tradePrice) tradePrice else maxPrice

        minPrice = if (minPrice > tradePrice) tradePrice else minPrice

        volume = volume + tradeVolume
        trades++
    }

    fun addBar(bar:XBar){
        if  (openPrice == 0.0){
            openPrice = bar.openPrice
        }
        closePrice = bar.closePrice
        maxPrice = if (maxPrice < bar.maxPrice) bar.maxPrice else maxPrice
        minPrice = if (minPrice > bar.minPrice) bar.minPrice else minPrice

        volume = volume + bar.volume
        trades += minOf(bar.trades, 1)
    }

    override fun toString(): String {
        return String.format("{begin time: %s, end time: %s, close price: %f, open price: %f, min price: %f, max price: %f, volume: %f, trades: %d}",
                beginTime, endTime, closePrice, openPrice, minPrice, maxPrice, volume, trades)
    }
}