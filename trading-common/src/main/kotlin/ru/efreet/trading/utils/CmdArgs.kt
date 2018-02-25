package ru.efreet.trading.utils

import org.apache.commons.cli.*
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import java.time.ZonedDateTime

/**
 * Created by fluder on 17/02/2018.
 */
data class CmdArgs(var stoploss: Double? = null,
                   var start: ZonedDateTime? = null,
                   var end: ZonedDateTime? = null,
                   var population: Int? = null,
                   var resetStrategy: Boolean = false,
                   var resetUsd: Boolean = false,
                   var train: Int? = null,
                   var exchange: String = "binance",
                   var instrument: Instrument = Instrument("BTC", "USDT"),
                   var barInterval: BarInterval = BarInterval.ONE_MIN,
                   var testOnly: Boolean = false,
                   var usdLimit: Double? = null,
                   var trainPeriod: Long? = null,
                   var logicName: String = "sd3",
                   var cachePath: String = "cache.sqlite3",
                   var logicPropertiesPath: String? = null, // = "bot.properties",
                   var seedType: SeedType = SeedType.RANDOM) {

    companion object {

        fun parseTime(time: String): ZonedDateTime {
            if (time.equals("now"))
                return ZonedDateTime.now()

            return try {
                ZonedDateTime.parse(time)
            } catch (e: Exception) {
                ZonedDateTime.parse(time + "T00:00Z[GMT]")
            }
        }

        fun parse(args: Array<String>): CmdArgs {

            val options = Options()

                    .addOption("l", "stoploss", true, "stop loss in %")
                    .addOption("s", "start", true, "start date")
                    .addOption("e", "end", true, "end date")
                    .addOption("p", "population", true, "population")
                    .addOption("r", "reset", false, "reset strategy")
                    .addOption("u", "usd", false, "reset usd to 1000")
                    .addOption("a", "train", true, "train days")
                    .addOption("x", "exchange", true, "exchange (poloniex,trading)")
                    .addOption("i", "instrument", true, "instrument, default BTC_USDT")
                    .addOption("t", "interval", true, "interval, default ONE_MIN")
                    .addOption("n", "test", false, "test only, default false")
                    .addOption("m", "limit", true, "usd limit ratio")
                    .addOption("o", "tperiod", true, "train period in hours")
                    .addOption("g", "logic", true, "logic (macd, sd(default))")
                    .addOption("c", "cache", true, "cache path")
                    .addOption("f", "settings", true, "logic settings path")
                    .addOption("d", "seed", true, "SeedType")


            val parser = BasicParser()
            val cmdArgs = CmdArgs()

            val cmd: CommandLine
            try {
                cmd = parser.parse(options, args)
            } catch (e: ParseException) {
                System.err.println(e.message)
                val formater = HelpFormatter()
                formater.printHelp("Bot", options)
                System.exit(-1)
                return cmdArgs
            }

            if (cmd.hasOption('l'))
                cmdArgs.stoploss = cmd.getOptionValue('l').toDouble()

            if (cmd.hasOption('s')) {
                cmdArgs.start = parseTime(cmd.getOptionValue('s'))
            }

            if (cmd.hasOption('e')) {
                cmdArgs.end = parseTime(cmd.getOptionValue('e'))
            }

            if (cmd.hasOption('p')) {
                cmdArgs.population = cmd.getOptionValue('p').toInt()
            }

            if (cmd.hasOption('r')) {
                cmdArgs.resetStrategy = true
            }

            if (cmd.hasOption('u')) {
                cmdArgs.resetUsd = true
            }

            cmd.getOptionValue('a')?.let { cmdArgs.train = it.toInt() }

            if (cmd.hasOption('n')) {
                cmdArgs.testOnly = true
            }

            cmd.getOptionValue('m')?.let { cmdArgs.usdLimit = it.toDouble() }
            cmd.getOptionValue('o')?.let { cmdArgs.trainPeriod = it.toLong() }

            cmd.getOptionValue('x')?.let { cmdArgs.exchange = it }

            cmd.getOptionValue('i')?.let {
                val n = it.split("_")
                cmdArgs.instrument = Instrument(n[0], n[1])
            }

            cmd.getOptionValue('t')?.let { cmdArgs.barInterval = BarInterval.valueOf(it) }

            cmd.getOptionValue('g')?.let { cmdArgs.logicName = it }
            cmd.getOptionValue('c')?.let { cmdArgs.cachePath = it }

            cmd.getOptionValue('f')?.let { cmdArgs.logicPropertiesPath = it }
            cmd.getOptionValue('d')?.let { cmdArgs.seedType = SeedType.valueOf(it) }

            return cmdArgs
        }
    }

}