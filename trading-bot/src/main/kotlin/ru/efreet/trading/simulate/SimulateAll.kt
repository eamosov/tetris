package ru.efreet.trading.simulate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.storeAsJson
import java.time.Duration
import java.time.ZonedDateTime

class SimulateAll {

    companion object {

        val log: Logger = LoggerFactory.getLogger(SimulateAll::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)

            val sim = Simulator(cmd)

            val instruments = sim.cache.getInstruments(cmd.exchange, cmd.barInterval).filter { it.base == "USDT" }

            log.info("Simulate instruments: {}", instruments)

            for (instrument in instruments) {

                try {

                    val firstBar = sim.cache.getFirst(cmd.exchange, instrument, cmd.barInterval)
                    val lastBar = sim.cache.getLast(cmd.exchange, instrument, cmd.barInterval)

                    if (Duration.between(firstBar!!.endTime, lastBar!!.endTime).toDays() < 60)
                        continue

                    log.info("Simulate {}, fistBar={}, lastBar={}", instrument, firstBar.endTime, lastBar.endTime)

                    val state = State()
                    state.instruments = listOf(instrument to 1.0f).toMap()
                    state.interval = cmd.barInterval
                    state.startTime = ZonedDateTime.parse("2018-07-01T00:00Z[GMT]")
                    state.endTime = ZonedDateTime.parse("2018-08-06T00:00Z[GMT]")

                    val tradeHistory = sim.run(state)
                    val stats = StatsCalculator().stats(tradeHistory)

                    log.info("FINISH {}:{}", instrument, stats)

                    tradeHistory.storeAsJson("simulate_${instrument}.json")
                } catch (e: Throwable) {
                    log.error("Error in ${instrument}", e)
                }
            }

        }
    }

}