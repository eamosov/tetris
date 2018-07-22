package ru.efreet.trading.exchange.impl.cache

import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.utils.CmdArgs
import java.time.ZonedDateTime

/**
 * Created by fluder on 01/06/2018.
 */
class UpdateAll {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val cache = BarsCache(cmd.cachePath)
            val exchange = Exchange.getExchange(cmd.exchange)

            println("Cached instruments: " + cache.getInstruments(exchange.getName(), cmd.barInterval))

            val prices = exchange.getPricesMap()

            val end = cmd.end ?: ZonedDateTime.now()

            for (instrument in prices.keys) if (instrument.base!!.equals("USDT") || instrument.base!!.equals("BTC")){

                cache.createTable(exchange.getName(), instrument, cmd.barInterval)

                val start = (cmd.start ?: cache.getLast(exchange.getName(), instrument, cmd.barInterval)?.endTime?.minus(cmd.barInterval.duration)) ?: ZonedDateTime.parse("2017-01-01T00:00Z[GMT]")

                println("Fetching ${instrument}/${cmd.barInterval.duration} from ${exchange.getName()} between ${start} and ${end} ")

                val bars = exchange.loadBars(instrument, cmd.barInterval, start, end)

                println("Saving ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}")

                cache.saveBars(exchange.getName(), instrument, bars.filter { it.timePeriod == cmd.barInterval.duration })
            }
        }
    }

}