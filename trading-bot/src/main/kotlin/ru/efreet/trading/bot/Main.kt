package ru.efreet.trading.bot

import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.loadFromJson
import ru.efreet.trading.utils.round2
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
            val cache = BarsCache(cmd.cachePath)
            val baseName = "USDT"

            exchange.logBalance(baseName)

//            if (cmd.resetStrategy)
//                botSettings.params.clear()

            val bots = hashMapOf<Instrument, TradeBot>()

            //val population = cmd.population ?: 50
            //val trainPeriod = cmd.trainPeriod ?: 1L

            val botConfiguration: BotConfiguration = loadFromJson("bot.js")

            for (bot in botConfiguration.bots) {

                val instrument = Instrument.parse(bot.instrument)
                val interval = BarInterval.valueOf(bot.interval)

                val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(bot.logic, instrument, interval)
                logic.loadState(bot.settings)


                val lastCachedBar = cache.getLast(exchange.getName(), instrument, interval)
                println("Updating cache from ${lastCachedBar.endTime}...")
                val newCachedBars = exchange.loadBars(instrument, interval, lastCachedBar.endTime.minusHours(1), ZonedDateTime.now())
                cache.saveBars(exchange.getName(), instrument, newCachedBars.filter { it.timePeriod == interval.duration })


                for (days in arrayOf(56, 28, 14, 7)) {
                    val historyStart = ZonedDateTime.now().minusDays(days.toLong()).minus(interval.duration.multipliedBy(logic.historyBars))
                    val bars = cache.getBars(exchange.getName(), instrument, interval, historyStart, ZonedDateTime.now())
                    bars.checkBars()

                    val tradeHistory = ProfitCalculator().tradeHistory(bot.logic,
                            logic.getParams()!!, instrument, interval, exchange.getFee(), bars,
                            listOf(Pair(ZonedDateTime.now().minusDays(days.toLong()), ZonedDateTime.now())),
                            true)

                    val tradesStats = StatsCalculator().stats(tradeHistory)
                    println("Stats ${bot.logic}/${bot.instrument}/${bot.settings} for last ${days} days: trades=${tradesStats.trades}, profit=${tradesStats.profit.round2()} sma10=${tradesStats.sma10.round2()}, last=${tradeHistory.trades.last().time}")
                }

                bots.put(instrument, TradeBot(exchange, cache, bot.limit, cmd.testOnly, instrument, logic, interval, { _, _ ->
                    //                    botSettings.addTrade(bot.instrument, order)
//                    BotSettings.save(botSettingsPath, botSettings)
                }))

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

            //val trainerTimer = Periodical(Duration.ofHours(trainPeriod))
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

