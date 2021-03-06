package ru.efreet.trading.utils

import org.apache.commons.cli.*
import org.slf4j.LoggerFactory
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.trainer.BotMetrica
import ru.efreet.trading.trainer.BotTrainer
import ru.efreet.trading.trainer.CdmBotTrainer
import ru.efreet.trading.trainer.GdmBotTrainer
import java.time.ZonedDateTime

/**
 * Created by fluder on 17/02/2018.
 */
data class CmdArgs(var start: ZonedDateTime? = null,
                   var end: ZonedDateTime? = null,
                   var population: Int? = null,
                   var train: Int? = null,
                   var exchange: String = "binance",
                   var instruments: List<Instrument> = arrayListOf(Instrument("BTC", "USDT")),
                   var barInterval: BarInterval = BarInterval.ONE_MIN,
                   var testOnly: Boolean = false,
                   var saveAll: Boolean = false,
                   var logicName: String = "ml",
                   var cachePath: String = "cache.sqlite3",
                   var settings: String? = null, // = "bot.properties",
                   var cpu: Int = Runtime.getRuntime().availableProcessors() - 2,
                   var steps: Array<Int>? = null,
                   var shortTest: Boolean = false,
                   var trainer: String = "cdm") {

    val instrument: Instrument
        get() = instruments.first()

    companion object {

        val log = LoggerFactory.getLogger(CmdArgs::class.java)

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

                    .addOption("s", "start", true, "start date")
                    .addOption("e", "end", true, "end date")
                    .addOption("p", "population", true, "population")
                    .addOption("a", "train", true, "train days")
                    .addOption("x", "exchange", true, "exchange (poloniex,trading)")
                    .addOption("i", "instrument", true, "instrument, default BTC_USDT")
                    .addOption("t", "interval", true, "interval, default ONE_MIN")
                    .addOption("n", "test", false, "test only, default false")
                    .addOption("g", "logic", true, "logic (macd, sd(default))")
                    .addOption("c", "cache", true, "cache path")
                    .addOption("f", "settings", true, "logic settings path")
                    .addOption("m", "cpu", true, "cpu numbers")
                    .addOption("d", "steps", true, "CDM steps, default 1,5,20")
                    .addOption("z", "shorttest", false, "no test on start")
                    .addOption("r", "trainer", true, "cdm(default)|gdm")
                    .addOption("1", "saveall", false, "save all results")


            val parser = BasicParser()
            val cmdArgs = CmdArgs()

            val cmd: CommandLine
            try {
                cmd = parser.parse(options, args)
            } catch (e: ParseException) {
                log.error("ParseException", e)
                val formater = HelpFormatter()
                formater.printHelp("Bot", options)
                System.exit(-1)
                return cmdArgs
            }

            if (cmd.hasOption('s')) {
                cmdArgs.start = parseTime(cmd.getOptionValue('s'))
            }

            if (cmd.hasOption('e')) {
                cmdArgs.end = parseTime(cmd.getOptionValue('e'))
            }

            if (cmd.hasOption('p')) {
                cmdArgs.population = cmd.getOptionValue('p').toInt()
            }

            cmd.getOptionValue('a')?.let { cmdArgs.train = it.toInt() }

            if (cmd.hasOption('n')) {
                cmdArgs.testOnly = true
            }

            if (cmd.hasOption('1')) {
                cmdArgs.saveAll = true
            }

            if (cmd.hasOption('z')) {
                cmdArgs.shortTest = true
            }

            cmd.getOptionValue('x')?.let { cmdArgs.exchange = it }

            cmd.getOptionValue('i')?.let {
                cmdArgs.instruments = it.split(",").map { Instrument.parse(it) }
            }

            cmd.getOptionValue('t')?.let { cmdArgs.barInterval = BarInterval.valueOf(it) }

            cmd.getOptionValue('g')?.let { cmdArgs.logicName = it }
            cmd.getOptionValue('c')?.let { cmdArgs.cachePath = it }

            cmd.getOptionValue('f')?.let { cmdArgs.settings = it }

            cmd.getOptionValue('m')?.let { cmdArgs.cpu = it.toInt() }

            cmd.getOptionValue('d')?.let { cmdArgs.steps = it.split(",").map { it.toInt() }.toTypedArray() }

            cmd.getOptionValue('r')?.let { cmdArgs.trainer = it }

            return cmdArgs
        }
    }

    fun <P, R, M> makeTrainer(): BotTrainer<P, R, M> where M : Comparable<M>, M : BotMetrica {
        return when (trainer) {
            "cdm" -> if (steps != null) CdmBotTrainer<P, R, M>(cpu, steps!!) else CdmBotTrainer<P, R, M>(cpu)
            "gdm" -> if (steps != null) GdmBotTrainer<P, R, M>(cpu, steps!!) else GdmBotTrainer<P, R, M>(cpu)
            else -> throw RuntimeException("trainer $trainer not found")
        }
    }
}