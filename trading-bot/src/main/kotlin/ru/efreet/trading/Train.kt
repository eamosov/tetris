package ru.efreet.trading

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.bot.TradesStatsShort
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.trainer.Metrica
import ru.efreet.trading.trainer.TrainItem
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.SeedType
import ru.efreet.trading.utils.SortedProperties
import ru.efreet.trading.utils.toJson
import java.io.File

/**
 * Created by fluder on 08/02/2018.
 */

//data class TrainSettings(val times: List<Pair<String, String>>)

class Train {
    companion object {
        private fun saveAllResults(path: String, result: List<TrainItem<Any, TradesStats, Metrica>>) {
            File(path).printWriter().use{ out ->
                out.println(result.map { it -> TrainItem(it.args,TradesStatsShort(it.result.trades,it.result.profit, it.result.pearson),it.metrica) }.toJson())
            }

        }

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val trainer = cmd.makeTrainer<Any, TradesStats, Metrica>()

            val realExchange = Exchange.getExchange(cmd.exchange)


            //realExchange.loadBars(Ins)
//            val cache = BarsCache(cmd.cachePath)
//            var bars = cache.getBars("binance", Instrument.BTC_USDT, BarInterval.ONE_SECOND, ZonedDateTime.now().minusDays(10), ZonedDateTime.now())


            val exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, BarsCache(cmd.cachePath))

            val logic = LogicFactory.getLogic<Any>(cmd.logicName, cmd.instrument, cmd.barInterval) as AbstractBotLogic<Any>
            val stateLoaded = logic.loadState(cmd.settings!!)

            val population = logic.seed(SeedType.RANDOM, cmd.population ?: 10)
            if (stateLoaded)
                population.add(logic.copyParams(logic.params))

            val bars = exchange.loadBars(cmd.instrument, cmd.barInterval, cmd.start!!.minus(cmd.barInterval.duration.multipliedBy(logic.historyBars)).truncatedTo(cmd.barInterval), cmd.end!!.truncatedTo(cmd.barInterval))
            println("Searching best strategy for ${cmd.instrument} population=${population.size}, start=${cmd.start!!} end=${cmd.end!!}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${logic.logState()}")


            bars.checkBars()

            val result = trainer.getBestParams(logic.genes, population,
                    {
                        val history = ProfitCalculator().tradeHistory(cmd.logicName, it, cmd.instrument, cmd.barInterval, exchange.getFee(), bars, arrayListOf(Pair(cmd.start!!, cmd.end!!)), false)

                        StatsCalculator().stats(history)
                    },
                    { params, stats -> logic.metrica(params, stats) },
                    { logic.copyParams(it) },
                    { trainItem ->
                        synchronized(Train.Companion) {
                            val savePath = cmd.settings + ".out"
                            println("Saving intermediate logic's properties to ${savePath}")
                            val tmpLogic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
                            tmpLogic.setMinMax(trainItem.args, 50.0F, false)
                            tmpLogic.params = trainItem.args
                            tmpLogic.saveState(savePath, trainItem.result.toString())
                        }
                    })

            val (sp, stats) = result.last()

            println(sp.toJson())
            val savePath = cmd.settings + ".out"
            println("Saving logic's properties to ${savePath}")

            logic.setMinMax(sp, 50.0F, false)
            logic.params = sp
            logic.saveState(savePath, stats.toString())
            println(logic.logState())
            if (cmd.saveAll) {
                val saveResultsPath = cmd.settings + ".results"
                println("Saving results to ${saveResultsPath}")
                saveAllResults(saveResultsPath, result);
            }
        }

    }
}
