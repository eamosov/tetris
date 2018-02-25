package ru.efreet.trading.trainer

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogicParams
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.utils.SeedType
import java.time.ZonedDateTime

/**
 * Created by fluder on 25/02/2018.
 */

fun <P : AbstractBotLogicParams> BotTrainer.getBestParams(exchange: Exchange, instrument: Instrument, interval: BarInterval,
                                                          logicName: String,
                                                          configPath: String,
                                                          seedType: SeedType,
                                                          trainingSize: Int,
                                                          times: List<Pair<ZonedDateTime, ZonedDateTime>>,
                                                          origin: P?): Pair<P, TradesStats> {

    val logic = LogicFactory.getLogic<P>(logicName, instrument, interval)
    logic.loadState(configPath)

    if (origin != null) {
        logic.setMinMax(origin, 40.0, true)
    }

    val population = logic.seed(seedType, trainingSize)
    if (origin != null)
        population.add(origin)


    val startSearchTime = times.first().first
    val finishSearchTime = times.last().second

    val bars = exchange.loadBars(instrument, interval, startSearchTime.minus(interval.duration.multipliedBy(logic.historyBars)).truncatedTo(interval), finishSearchTime.truncatedTo(interval))
    println("Searching best strategy for $instrument population=${population.size}, start=${startSearchTime} end=${finishSearchTime}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${logic.logState()}")

    bars.checkBars()

    return getBestParams(logic.genes, population,
            { calcProfit(instrument, interval, logicName, it, bars, times, exchange.getFee()) },
            { args, stats -> logic.metrica(stats) },
            { logic.copyParams(it) })
}

fun <P : AbstractBotLogicParams> BotTrainer.calcProfit(instrument: Instrument, interval: BarInterval,
                                                       logicName: String,
                                                       params: P, bars: List<XBar>, times: List<Pair<ZonedDateTime, ZonedDateTime>>, feeP: Double): TradesStats {
    val st = System.currentTimeMillis()
    try {
        val history = ProfitCalculator().tradeHistory(logicName, params, instrument, interval, feeP, bars, times, false)
        return StatsCalculator().stats(history)
    } finally {
        //println("calcProfit took ${System.currentTimeMillis()-st}")
    }
}