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
import ru.efreet.trading.trainer.CdmBotTrainer
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
                            logic.getParams(), instrument, interval, exchange.getFee(), bars,
                            listOf(Pair(ZonedDateTime.now().minusDays(days.toLong()), ZonedDateTime.now())),
                            true)

                    val tradesStats = StatsCalculator().stats(tradeHistory)
                    println("Stats ${bot.logic}/${bot.instrument}/${bot.settings} for last ${days} days: trades=${tradesStats.trades}, profit=${tradesStats.profit.round2()} sma10=${tradesStats.sma10.round2()}, last=${tradeHistory.trades.last().time}")
                }

                bots.put(instrument, TradeBot(exchange, cmd.tradesPath, cache, bot.limit, cmd.testOnly, instrument, bot.logic, logic, bot.settings, interval, ZonedDateTime.parse(bot.trainStart), { _, _ ->
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

            val balanceTimer = Periodical(Duration.ofMinutes(5))
            val trainerTimer = Periodical(Duration.ofMinutes(cmd.train?.toLong() ?: 5))

            while (true) {
                try {
                    Thread.sleep(1000)

                    bots.forEach { it.value.periodic() }

                    trainerTimer.invoke({

                        println("Start training")

                        for ((instrument, bot) in bots) {

                            val tmpLogic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(bot.logicName, bot.instrument, bot.barInterval)
                            val curParams = bot.logic.getParams().copy()

                            val div = 20.0
                            val hardBound = false

                            tmpLogic.setParams(curParams)
                            tmpLogic.setMinMax(curParams, div, hardBound)

                            val population = tmpLogic.seed(cmd.seedType, cmd.population ?: 20)
                            population.add(curParams)

                            val trainEnd = ZonedDateTime.now().truncatedTo(bot.barInterval)

                            val bars = cache.getBars(
                                    exchange.getName(),
                                    bot.instrument,
                                    bot.barInterval,
                                    bot.trainStart.minus(bot.barInterval.duration.multipliedBy(tmpLogic.historyBars)).truncatedTo(bot.barInterval),
                                    trainEnd)

                            println("Searching best strategy for ${bot.instrument} population=${population.size}, start=${bot.trainStart} end=${trainEnd}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}")

                            val (sp, stats) = CdmBotTrainer().getBestParams(tmpLogic.genes, population,
                                    {
                                        val history = ProfitCalculator().tradeHistory(bot.logicName, it, bot.instrument, bot.barInterval, exchange.getFee(), bars, arrayListOf(Pair(bot.trainStart, trainEnd)), false)

                                        StatsCalculator().stats(history)
                                    },
                                    { _, stats -> tmpLogic.metrica(stats) },
                                    { tmpLogic.copyParams(it) })

                            println("STATS: $stats")
                            println("Setting new params for ${instrument}: ${sp}")
                            tmpLogic.setParams(sp)
                            tmpLogic.setMinMax(sp, div, hardBound)
                            tmpLogic.saveState(bot.settings, stats.toString())

                            bot.logic.setParams(sp)
                        }
                    })

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

