package ru.efreet.trading.exchange

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBaseBar
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Created by fluder on 08/02/2018.
 */
data class AggTrade(val timestampMillis: Long, val price: Double, val quantity: Double) {

    fun asSecondsBar(): XBar {
        val tradeTimeStamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.of("GMT"))
        val bar = XBaseBar(Duration.ofSeconds(1), tradeTimeStamp.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1))
        bar.addTrade(quantity, price)
        return bar
    }
}