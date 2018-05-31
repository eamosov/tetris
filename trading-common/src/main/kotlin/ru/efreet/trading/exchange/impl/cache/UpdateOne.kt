package ru.efreet.trading.exchange.impl.cache

import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.utils.CmdArgs

/**
 * Created by fluder on 01/06/2018.
 */
class UpdateOne {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val cache = BarsCache(cmd.cachePath)
            val exchange = Exchange.getExchange(cmd.exchange)

            cache.createTable(exchange.getName(), cmd.instrument, cmd.barInterval)

            println("Fetching ${cmd.instrument}/${cmd.barInterval.duration} from ${exchange.getName()} between ${cmd.start} and ${cmd.end} ")

            val bars = exchange.loadBars(cmd.instrument, cmd.barInterval, cmd.start!!, cmd.end!!)

            println("Saving ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}")

            cache.saveBars(exchange.getName(), cmd.instrument, bars.filter { it.timePeriod == cmd.barInterval.duration })
        }
    }

}