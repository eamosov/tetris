package ru.efreet.trading

import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.trainer.CdmBotTrainer
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.SeedType
import ru.efreet.trading.utils.toJson

/**
 * Created by fluder on 08/02/2018.
 */

//data class TrainSettings(val times: List<Pair<String, String>>)

class Train {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val cdm = CdmBotTrainer(cmd.cpu, arrayOf(1, 5, 20, 40))

            val realExchange = Exchange.getExchange(cmd.exchange)


            //realExchange.loadBars(Ins)
//            val cache = BarsCache(cmd.cachePath)
//            var bars = cache.getBars("binance", Instrument.BTC_USDT, BarInterval.ONE_SECOND, ZonedDateTime.now().minusDays(10), ZonedDateTime.now())


            val exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, BarsCache(cmd.cachePath))

            val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
            logic.loadState(cmd.settings!!)

            val population = logic.seed(SeedType.RANDOM, cmd.population ?: 10)
            if (logic.isInitialized())
                population.add(logic.getParams().copy())

            val bars = exchange.loadBars(cmd.instrument, cmd.barInterval, cmd.start!!.minus(cmd.barInterval.duration.multipliedBy(logic.historyBars)).truncatedTo(cmd.barInterval), cmd.end!!.truncatedTo(cmd.barInterval))
            println("Searching best strategy for ${cmd.instrument} population=${population.size}, start=${cmd.start!!} end=${cmd.end!!}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${logic.logState()}")


            bars.checkBars()

            val (sp, stats) = cdm.getBestParams(logic.genes, population,
                    {
                        val history = ProfitCalculator().tradeHistory(cmd.logicName, it, cmd.instrument, cmd.barInterval, exchange.getFee(), bars, arrayListOf(Pair(cmd.start!!, cmd.end!!)), false)

                        StatsCalculator().stats(history)
                    },
                    { params, stats -> logic.metrica(params, stats) },
                    { logic.copyParams(it) },
                    { params, stats ->
                        synchronized(Train.Companion) {
                            val savePath = cmd.settings + ".out"
                            println("Saving intermediate logic's properties to ${savePath}")
                            val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
                            logic.setMinMax(params, 50.0, false)
                            logic.setParams(params)
                            logic.saveState(savePath, stats.toString())
                        }
                    })

            println(sp.toJson())
            val savePath = cmd.settings + ".out"
            println("Saving logic's properties to ${savePath}")

            logic.setMinMax(sp, 50.0, false)
            logic.setParams(sp)
            logic.saveState(savePath, stats.toString())
            println(logic.logState())
        }
    }
}