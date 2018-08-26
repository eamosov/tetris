package ru.efreet.trading.bars

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 19/02/2018.
 */
data class XBaseBar(override var timePeriod: Duration,
                    override var endTime: ZonedDateTime,
                    override var openPrice: Float = 0.0F,
                    override var maxPrice: Float = 0.0F,
                    override var minPrice: Float = Float.MAX_VALUE,
                    override var closePrice: Float = 0.0F,
                    override var volume: Float = 0.0F,
                    override var volumeBase: Float = 0.0F,
                    override var volumeQuote: Float = 0.0F,
                    override var trades: Short = 0) : XBar {

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
            this(endTime, openPrice.toFloat(),
                    highPrice.toFloat(),
                    lowPrice.toFloat(),
                    closePrice.toFloat(),
                    volume.toFloat())


    /**
     * Constructor.
     * @param endTime the end time of the bar period
     * @param openPrice the open price of the bar period
     * @param highPrice the highest price of the bar period
     * @param lowPrice the lowest price of the bar period
     * @param closePrice the close price of the bar period
     * @param volume the volume of the bar period
     */
    constructor(endTime: ZonedDateTime, openPrice: Float, highPrice: Float, lowPrice: Float, closePrice: Float, volume: Float) :
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
    constructor(timePeriod: Duration, endTime: ZonedDateTime, openPrice: Float, highPrice: Float, lowPrice: Float, closePrice: Float, volume: Float) :
            this(timePeriod, endTime, openPrice, highPrice, lowPrice, closePrice, volume, 0.0F,0.0F,0)

    constructor(bar: XBar) :
            this(bar.timePeriod, bar.endTime, bar.openPrice, bar.maxPrice, bar.minPrice, bar.closePrice, bar.volume, bar.volumeBase, bar.volumeQuote,bar.trades)


    fun addBar(bar:XBar){
        if  (openPrice == 0.0F){
            openPrice = bar.openPrice
        }
        closePrice = bar.closePrice
        maxPrice = if (maxPrice < bar.maxPrice) bar.maxPrice else maxPrice
        minPrice = if (minPrice > bar.minPrice) bar.minPrice else minPrice

        volume = volume + bar.volume
        trades = (trades + minOf(bar.trades, 1)).toShort()
        volumeBase += bar.volumeBase
        volumeQuote += bar.volumeQuote
    }

    fun invert(){
        openPrice = 100000000.0F/openPrice
        closePrice = 100000000.0F/closePrice
        val mm = 100000000.0F/minPrice
        minPrice = 100000000.0F/maxPrice
        maxPrice = mm
    }

    override fun toString(): String {
        return String.format("{begin time: %s, end time: %s, close price: %f, open price: %f, min price: %f, max price: %f, volume: %f, volumeBase: %f, volumeQuote: %f, trades: %d}",
                beginTime, endTime, closePrice, openPrice, minPrice, maxPrice, volume, volumeBase, volumeQuote, trades)
    }
}