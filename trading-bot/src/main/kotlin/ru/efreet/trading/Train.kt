package ru.efreet.trading

import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.AbstractBotLogicParams
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.trainer.CdmBotTrainer
import ru.efreet.trading.trainer.getBestParams
import ru.efreet.trading.utils.CmdArgs
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

            val exchange = Exchange.getExchange(cmd.exchange)
            val cache = CachedExchange(exchange.getName(), exchange.getFee(), cmd.barInterval, BarsCache(cmd.cachePath))

//            val ts: TrainSettings
//
//            if (cmd.start != null && cmd.end != null) {
//                ts = TrainSettings(arrayListOf(Pair(cmd.start!!.toString(), cmd.end!!.toString())))
//                ts.storeAsJson("train.json")
//            } else {
//                ts = loadFromJson("train.json")
//            }

            val (sp, stats) = CdmBotTrainer().getBestParams(
                    cache, cmd.instrument, cmd.barInterval,
                    cmd.logicName,
                    cmd.settings!!,
                    cmd.seedType,
                    cmd.population ?: 10,
                    //ts.times.map { Pair(ZonedDateTime.parse(it.first), ZonedDateTime.parse(it.second)) },
                    arrayListOf(Pair(cmd.start!!, cmd.end!!)),
                    null as AbstractBotLogicParams?)

            println(sp.toJson())
            val savePath = cmd.settings + ".out"
            println("Saving logic's properties to ${savePath}")

            val logic: BotLogic<AbstractBotLogicParams> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
            logic.setMinMax(sp, 20.0, false)
            logic.setParams(sp)
            logic.saveState(savePath, stats.toString())
            println(logic.logState())
        }
    }
}