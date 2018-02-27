package ru.efreet.trading

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradeHistory
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.trainer.CdmBotTrainer
import ru.efreet.trading.utils.*
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class State(var name: String,
                 var time: String,
                 var usd: Double = 1000.0,
                 var asset: Double = 0.0,
                 var instrument: Instrument,
                 var interval: BarInterval,
                 var trainDays: Long = 0,
                 var tradeHistory: TradeHistory? = null,
                 val tradeDuration: Duration = Duration.ofDays(1),
                 var population: Int = 20
) {

    fun getTime(): ZonedDateTime {
        return ZonedDateTime.parse(time)
    }

    fun setTime(time: ZonedDateTime) {
        this.time = time.toString()
    }

}

class Simulate(val cmd: CmdArgs, val statePath: String) {

    //lateinit var lastBar: Bar
    lateinit var exchange: Exchange
    lateinit var cache: BarsCache
    lateinit var state: State


    fun fee(): Double = (1.0 - (exchange.getFee() / 100.0) / 2.0)

    fun run(state: State) {

        this.state = state

        cache = BarsCache(cmd.cachePath)
        val realExchange = Exchange.getExchange(cmd.exchange)
        exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, cache)

        state.name = cmd.logicName
        cmd.start?.let { state.setTime(it) }
        if (cmd.resetUsd) {
            state.usd = 1000.0
            state.asset = 0.0
            state.tradeHistory = null
        }

        state.instrument = cmd.instrument
        state.interval = cmd.barInterval


        cmd.train?.let { state.trainDays = it.toLong() }
        cmd.population?.let { state.population = it }

        saveState()

        val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(state.name, state.instrument, state.interval)
        logic.loadState(cmd.settings!!)

        println(logic.logState())

        val historyStart = state.getTime().minus(state.interval.duration.multipliedBy(logic.historyBars))
        val history = cache.getBars(exchange.getName(), state.instrument, state.interval, historyStart, state.getTime())
        println("Loaded history ${history.size} bars from $historyStart to ${state.getTime()}")
        history.forEach { logic.insertBar(it) }
        logic.prepare()


        val end = cache.getLast(exchange.getName(), state.instrument, state.interval).endTime

        val trader = FakeTrader(state.usd, state.asset, exchange.getFee(), true)


        var startStatsTime = state.getTime().truncatedTo(ChronoUnit.DAYS)
        //var endStatsTime = state.getTime().truncatedTo(ChronoUnit.DAYS).plusDays(1)

        fun logDayStats() {
            val stats = StatsCalculator().stats(trader.history(startStatsTime, state.getTime()))
            trader.clearHistory()
            println("STATS: ${state.getTime()}:  ${stats}")
        }

        while (state.getTime().plus(state.tradeDuration).isBefore(end)) {

            if (state.getTime().isAfter(startStatsTime.plusDays(1))) {
                startStatsTime = state.getTime().truncatedTo(ChronoUnit.DAYS)
                logDayStats()
            }

            if (state.trainDays > 0) {
                val trainStart = state.getTime().minusDays(state.trainDays)
//                val (params, stats) = CdmBotTrainer().getBestParams(exchange, state.instrument, state.interval,
//                        cmd.logicName,
//                        cmd.settings!!,
//                        cmd.seedType,
//                        state.population, arrayListOf(Pair(trainStart, state.getTime())), logic.getParams())

////
                //tmpLogic нужно для генерации population и передачи tmpLogic.genes в getBestParams
                val tmpLogic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(state.name, state.instrument, state.interval)
                tmpLogic.setMinMax(logic.getParams()!!, 20.0, false)
                val population = tmpLogic.seed(cmd.seedType, state.population)
                population.add(logic.getParams()!!)

                val bars = exchange.loadBars(state.instrument, state.interval, trainStart.minus(state.interval.duration.multipliedBy(logic.historyBars)).truncatedTo(state.interval), state.getTime().truncatedTo(state.interval))
                println("Searching best strategy for ${state.instrument} population=${population.size}, start=${trainStart} end=${state.getTime()}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${tmpLogic.logState()}")
                bars.checkBars()


                val (params, stats) = CdmBotTrainer().getBestParams(tmpLogic.genes, population,
                        {
                            val history = ProfitCalculator().tradeHistory(state.name, it, state.instrument, state.interval, exchange.getFee(), bars, arrayListOf(Pair(trainStart, state.getTime())), false)
                            val stats = StatsCalculator().stats(history)

//                            if (stats.profit > 1.0 && params is SimpleBotLogicParams && (params as SimpleBotLogicParams).f3Index !=null){
//                                IntFunction3.incCounter(params.f3Index!!)
//                            }

                            stats
                        },
                        { args, stats -> logic.metrica(stats) },
                        { logic.copyParams(it) })

////

                logic.setParams(params)
                //logic.setMinMax(params, 20.0, false)
                //println(logic.logState())
                println("STRATEGY: ${logic.getParams()} $stats")
            }

            saveState()

            val newBars = cache.getBars(exchange.getName(), state.instrument, state.interval, state.getTime(), state.getTime().plus(state.tradeDuration))

            var bar: XBar? = null

            if (newBars.isNotEmpty()) {

                println("Loaded ${newBars.size} bars from ${newBars.first().endTime} to ${newBars.last().endTime}")

                for (_bar in newBars) {
                    bar = _bar

                    logic.insertBar(bar)
                    //TODO передавать stats?
                    val advice = logic.getAdvice(/*state.stats*/null, trader, true)
                    val trade = trader.executeAdvice(advice)

                    if (trade != null) {
                        println("TRADE: $trade")
                    }
                }

                println("\"EOD\",\"${LocalDate.from(bar!!.endTime)}\",${trader.usd.round2()},${trader.asset.round5()},${bar.closePrice.round2()},${(trader.usd + trader.asset * bar.closePrice).toInt()}")
            }

            state.setTime(state.getTime().plus(state.tradeDuration))
            saveState()
        }

        logDayStats()
    }

    fun saveState() {
        state.storeAsJson(statePath)
    }

    fun readState(): State {
        try {
            return loadFromJson(statePath)
        } catch (e: Exception) {
            println("Create new ${statePath}")
            return State(name = "sd2", instrument = Instrument.BTC_USDT, interval = BarInterval.ONE_MIN, time = ZonedDateTime.now().toString(), trainDays = 14)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)

            val sim = Simulate(cmd, "simulate.json")

            sim.run(sim.readState())
        }
    }
}
