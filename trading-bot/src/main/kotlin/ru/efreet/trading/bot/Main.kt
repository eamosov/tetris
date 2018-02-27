package ru.efreet.trading.bot

import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.Periodical
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 08/02/2018.
 */

class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)

            val exchange = Exchange.getExchange(cmd.exchange)
            val barsCache = BarsCache(cmd.cachePath)
            val instruments = arrayListOf(cmd.instrument)
            val baseName = "USDT"

            exchange.logBalance(baseName)

//            if (cmd.resetStrategy)
//                botSettings.params.clear()

            val bots = hashMapOf<Instrument, TradeBot>()

            val usdLimit = cmd.usdLimit ?: 0.5
            val testOnly = cmd.testOnly
            val population = cmd.population ?: 50
            val interval = cmd.barInterval
            val trainPeriod = cmd.trainPeriod ?: 1L


            for (i in 0 until instruments.size) {
                val instrument = instruments[i]

                val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
                logic.loadState(cmd.logicPropertiesPath!!)

                val cache = BarsCache(cmd.cachePath)

                val lastCachedBar = cache.getLast(exchange.getName(), cmd.instrument, cmd.barInterval)
                println("Updating cache from ${lastCachedBar.endTime}...")
                val newCachedBars = exchange.loadBars(cmd.instrument, cmd.barInterval, lastCachedBar.endTime.minusHours(1), ZonedDateTime.now())
                cache.saveBars(exchange.getName(), cmd.instrument, newCachedBars.filter { it.timePeriod == cmd.barInterval.duration })


                for (days in arrayOf(56, 28, 14, 7)){
                    val historyStart = ZonedDateTime.now().minusDays(days.toLong()).minus(cmd.barInterval.duration.multipliedBy(logic.historyBars))
                    val bars = cache.getBars(exchange.getName(), cmd.instrument, cmd.barInterval, historyStart, ZonedDateTime.now())
                    bars.checkBars()

                    val tradeHistory = ProfitCalculator().tradeHistory(cmd.logicName,
                            logic.getParams()!!, cmd.instrument, cmd.barInterval, exchange.getFee(), bars,
                            listOf(Pair(ZonedDateTime.now().minusDays(days.toLong()), ZonedDateTime.now())),
                            true)

                    val tradesStats = StatsCalculator().stats(tradeHistory)
                    println("Stats for last ${days} days: $tradesStats")
                }


                val bot = TradeBot(exchange, barsCache, usdLimit / instruments.size, testOnly, instrument, logic, interval, { bot, order ->
//                    botSettings.addTrade(bot.instrument, order)
//                    BotSettings.save(botSettingsPath, botSettings)
                })
                bots.put(instrument, bot)

                /*val params = botSettings.getParams(instrument) ?: CdmBotTrainer().getBestParams(exchange, instrument, interval,
                        cmd.logicName,
                        cmd.logicPropertiesPath!!,
                        cmd.seedType,
                        population,
                        arrayListOf(Pair(ZonedDateTime.now().minusDays((/*cmd.train!![i]*/cmd.train!!).toLong()), ZonedDateTime.now())),
                        null).first*/

//                botSettings.getParams(instrument)?.let {
//                    logic.setParams(it)
//                }
//
//                botSettings.setParams(instrument, logic.getParams())
//                BotSettings.save(botSettingsPath, botSettings)
            }

//            if (cmd.hasOption('n')) {
//                print("Exiting")
//                return
//            }

            for (bot in bots.values) {
                bot.startStrategy(/*botSettings.getParams(bot.instrument)!!*/)
                bot.logState()
            }

            val trainerTimer = Periodical(Duration.ofHours(trainPeriod))
            val balanceTimer = Periodical(Duration.ofMinutes(5))

            while (true) {
                try {
                    Thread.sleep(10 * 1000)

                    bots.forEach { it.value.periodic() }

                    /*
                    trainerTimer.invoke({
                        for (i in 0 until instruments.size) {

                            val instrument = instruments[i]
                            //val train = cmd.train!![i].toLong()
                            val train = cmd.train!!.toLong()
                            val bot = bots[instrument]!!

                            val ts = CdmBotTrainer().getBestParams(
                                    exchange, instrument, interval,
                                    cmd.logicName,
                                    cmd.logicPropertiesPath!!,
                                    cmd.seedType,
                                    population,
                                    arrayListOf(Pair(ZonedDateTime.now().minusDays(train), ZonedDateTime.now())),
                                    botSettings.getParams(instrument))

                            println("Setting new params for ${instrument}: ${ts}")

                            botSettings.setParams(instrument, ts.first)
                            BotSettings.save(botSettingsPath, botSettings)
                            bot.logic.setParams(ts.first)
                        }
                    })*/

                    balanceTimer.invoke({
                        exchange.logBalance(baseName)
                    })


                } catch (e: InterruptedException) {
                    bots.forEach { t, u -> u.stopTrade() }
                    break
                }
            }

        }
    }
}

