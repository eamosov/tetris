package ru.efreet.trading

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradesStats
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

data class State(var name: String,
                 var time: ZonedDateTime,
                 var startTime: ZonedDateTime = time,
                 var usd: Double = 1000.0,
                 var asset: Double = 0.0,
                 var trainDays: Long = 45,
                 var instrument: Instrument,
                 var interval: BarInterval,
                 var maxParamsDeviation: Double = 10.0,
                 var hardBounds: Boolean = false,
                 val tradeDuration: Duration = Duration.ofHours(24),
                 var population: Int = 20,
                 var historyPath: String = "simulate_history.json",
                 var properties: String = "simulate.properties") {


}

class Simulate(val cmd: CmdArgs, val statePath: String) {

    //lateinit var lastBar: Bar
    lateinit var exchange: Exchange
    lateinit var cache: BarsCache
    lateinit var state: State
    var cdm = CdmBotTrainer(cmd.cpu, cmd.steps)

    fun fee(): Double = (1.0 - (exchange.getFee() / 100.0) / 2.0)

    fun run(state: State) {

        this.state = state

        cache = BarsCache(cmd.cachePath)

        val realExchange = Exchange.getExchange(cmd.exchange)
        exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, cache)

        state.name = cmd.logicName
        cmd.start?.let {
            state.time = it
            state.startTime = it
        }

        state.instrument = cmd.instrument
        state.interval = cmd.barInterval

        cmd.population?.let { state.population = it }

        saveState()

        val logic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(state.name, state.instrument, state.interval)
        logic.loadState(state.properties)

        val _maxParamsDeviation= state.maxParamsDeviation
        state.maxParamsDeviation = 50.0
        val (params, _stats) = tuneParams(logic.getParams(), 100, false)
        println("Initial optimisation of random parameters have ended with stats: $_stats")
        logic.setParams(params)
        state.maxParamsDeviation = _maxParamsDeviation


        println(logic.logState())

        val historyStart = state.time.minus(state.interval.duration.multipliedBy(logic.historyBars))
        val history = cache.getBars(exchange.getName(), state.instrument, state.interval, historyStart, state.time)
        println("Loaded history ${history.size} bars from $historyStart to ${state.time}")
        history.forEach { logic.insertBar(it) }
        logic.prepareBars()


        val end = cache.getLast(exchange.getName(), state.instrument, state.interval).endTime

        val trader = FakeTrader(state.usd, state.asset, exchange.getFee(), true, exchange.getName(), state.instrument)

        var stats: TradesStats?

        var bar: XBar? = null

        while (state.time.plus(state.tradeDuration).isBefore(end)) {

            val (params, _stats) = tuneParams(logic.getParams(), state.population)
            logic.setParams(params)
            logic.saveState(state.properties, _stats.toString())

            stats = _stats
            println("STRATEGY: ${logic.getParams()} $stats")

            saveState()

            val newBars = cache.getBars(exchange.getName(), state.instrument, state.interval, state.time, state.time.plus(state.tradeDuration))

            if (newBars.isNotEmpty()) {

                println("Loaded ${newBars.size} bars from ${newBars.first().endTime} to ${newBars.last().endTime}")

                for (_bar in newBars) {
                    bar = _bar

                    logic.insertBar(bar)
                    val advice = logic.getAdvice(stats, trader, true)
                    val trade = trader.executeAdvice(advice)

                    if (trade != null) {
                        println("TRADE: $trade")
                    }
                }

                println("\"EOD\",\"${LocalDate.from(bar!!.endTime)}\",${trader.usd.round2()},${trader.asset.round5()},${bar.closePrice.round2()},${(trader.usd + trader.asset * bar.closePrice).toInt()}")

                trader.history(state.startTime, bar.endTime).storeAsJson(state.historyPath)
            }

            state.time = state.time.plus(state.tradeDuration)
            saveState()
        }

        val tradeHistory = trader.history(state.startTime, bar!!.endTime)

        tradeHistory.storeAsJson(state.historyPath)

        Graph().drawHistory(tradeHistory)
    }

    fun tuneParams(curParams: SimpleBotLogicParams, populationSize: Int, inclCurParams: Boolean = true): Pair<SimpleBotLogicParams, TradesStats> {

        //tmpLogic нужно для генерации population и передачи tmpLogic.genes в getBestParams
        val tmpLogic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(state.name, state.instrument, state.interval)
        tmpLogic.setParams(curParams)
        tmpLogic.setMinMax(curParams, state.maxParamsDeviation, state.hardBounds)

        val population = tmpLogic.seed(SeedType.RANDOM, populationSize)
        if (inclCurParams)
            population.add(curParams)

        val trainStart = state.time.minusDays(state.trainDays)

        val bars = exchange.loadBars(state.instrument, state.interval, trainStart.minus(state.interval.duration.multipliedBy(tmpLogic.historyBars)).truncatedTo(state.interval), state.time.truncatedTo(state.interval))
        println("Searching best strategy for ${state.instrument} population=${population.size}, start=${trainStart} end=${state.time}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${tmpLogic.logState()}")
        bars.checkBars()


        return cdm.getBestParams(tmpLogic.genes, population,
                {
                    val history = ProfitCalculator().tradeHistory(state.name, it, state.instrument, state.interval, exchange.getFee(), bars, arrayListOf(Pair(trainStart, state.time)), false)
                    val stats = StatsCalculator().stats(history)
                    stats
                },
                { args, stats ->
                    tmpLogic.metrica(args, stats)
                },
                { tmpLogic.copyParams(it) })
    }

    fun saveState() {
        state.storeAsJson(statePath)
    }

    fun readState(): State {
        try {
            return loadFromJson(statePath)
        } catch (e: Exception) {
            println("Create new ${statePath}")
            return State(name = "sd3", instrument = Instrument.BTC_USDT, interval = BarInterval.ONE_MIN, time = ZonedDateTime.parse("2018-03-01T00:00Z[GMT]"))
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
