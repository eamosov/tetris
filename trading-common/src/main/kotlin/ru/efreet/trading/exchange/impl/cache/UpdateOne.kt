package ru.efreet.trading.exchange.impl.cache

import org.slf4j.LoggerFactory
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.utils.CmdArgs
import java.time.ZonedDateTime

/**
 * Created by fluder on 01/06/2018.
 */
class UpdateOne {

    companion object {

        val log = LoggerFactory.getLogger(UpdateOne::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val cache = BarsCache(cmd.cachePath)
            val exchange = Exchange.getExchange(cmd.exchange)

            cache.createTable(exchange.getName(), cmd.instrument, cmd.barInterval)

            val start = (cmd.start ?: cache.getLast(exchange.getName(), cmd.instrument, cmd.barInterval)?.endTime?.minus(cmd.barInterval.duration)) ?: ZonedDateTime.parse("2020-01-01T00:00Z[GMT]")
            val end = cmd.end ?: ZonedDateTime.now()

            log.info("Fetching ${cmd.instrument}/${cmd.barInterval.duration} from ${exchange.getName()} between ${start} and ${end} ")

            val bars = exchange.loadBars(cmd.instrument, cmd.barInterval, start!!, end!!)

            log.info("Saving ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}")

            cache.saveBars(exchange.getName(), cmd.instrument, bars.filter { it.timePeriod == cmd.barInterval.duration })
        }
    }

}