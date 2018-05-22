package ru.efreet.trading

import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.bot.TradesStatsShort
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.trainer.Metrica
import ru.efreet.trading.trainer.TrainItem
import ru.efreet.trading.utils.*
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class State(var startTime: ZonedDateTime = ZonedDateTime.parse("2018-03-01T00:00Z[GMT]"),
                 var endTime: ZonedDateTime = ZonedDateTime.parse("2018-05-12T00:00Z[GMT]"),
                 var usd: Double = 1000.0,
                 var asset: Double = 0.0,
                 var trainDays: Long = 60,
                 var interval: BarInterval = BarInterval.ONE_MIN,
                 var startMaxParamsDeviation: Double = 50.0,
                 var maxParamsDeviation: Double = 10.0,
                 var hardBounds: Boolean = false,
                 var noSaveState: Boolean = false,
                 var saveDaysData: Boolean = false,
                 val trainPeriod: Duration = Duration.ofHours(24),
                 var startPopulation: Int = 500,
                 var population: Int = 20,
                 var historyPath: String = "simulate_history.json",
                 var properties: String = "simulate.properties",
                 var initOptimisation: Boolean = true,
                 var graph: Boolean = true) {


}

class Simulate(val cmd: CmdArgs, val statePath: String) {

    //lateinit var lastBar: Bar
    lateinit var exchange: Exchange
    lateinit var cache: BarsCache
    lateinit var state: State
    var trainer = cmd.makeTrainer<Any, TradesStats, Metrica>()

    fun fee(): Double = (1.0 - (exchange.getFee() / 100.0) / 2.0)

    fun run(state: State) {

        this.state = state

        cache = BarsCache(cmd.cachePath)

        val realExchange = Exchange.getExchange(cmd.exchange)
        exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, cache)

        state.interval = cmd.barInterval

        cmd.population?.let { state.population = it }

        saveState()

        val logic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, state.interval)
        logic.loadState(state.properties)

        if (state.initOptimisation) {
            val (params, _stats) = tuneParams(state.startTime, logic.getParams(), state.startMaxParamsDeviation, state.startPopulation, false)
            logic.setParams(params)
            if (!state.noSaveState)
                logic.saveState(state.properties, _stats.toString())
            println("Initial optimisation of random parameters: ${params} $_stats")
        }

        println(logic.logState())

        val historyStart = state.startTime.minus(state.interval.duration.multipliedBy(logic.historyBars))
        val history = cache.getBars(exchange.getName(), cmd.instrument, state.interval, historyStart, state.startTime)
        println("Loaded history ${history.size} bars from $historyStart to ${state.startTime}")
        history.forEach { logic.insertBar(it) }
        logic.prepareBars()

        val trader = FakeTrader(state.usd, state.asset, exchange.getFee(), true, exchange.getName(), cmd.instrument)

        val bars = cache.getBars(exchange.getName(), cmd.instrument, state.interval, state.startTime, state.endTime)
        println("Loaded ${history.size} bars from ${state.startTime} to ${state.endTime}")

        var lastTrainTime = bars.first().endTime
        var everyDay = bars.first().endTime

        var skipBuy = false

        var savedN = 0

        for (bar in bars){

            if (Duration.between(lastTrainTime, bar.endTime).toMillis() >= state.trainPeriod.toMillis()
                    && trader.availableAsset(cmd.instrument) == 0.0) { //если мы в баксах
                logic.loadState(state.properties)
                val (params, _stats) = tuneParams(bar.endTime, logic.getParams(), state.maxParamsDeviation, state.population)

                logic.setParams(params)
                if (!state.noSaveState)
                    logic.saveState(state.properties, _stats.toString())
                if (state.saveDaysData) {
                    File("props").mkdir()
                    logic.saveState("props/${bar.endTime.toEpochSecond()}.properties", _stats.toString())
                }
                println("STRATEGY: ${logic.getParams()} $_stats")
                lastTrainTime = bar.endTime
                skipBuy = true
            }

            logic.insertBar(bar)
            val advice = logic.getAdvice(trader, true)

            if (skipBuy){
                if (advice.decision == Decision.BUY){
                    println("skip buy")
                    continue
                }else{
                    skipBuy = false
                }
            }

            val trade = trader.executeAdvice(advice)

            if (trade != null) {
                println("TRADE: $trade")
            }

            if (Duration.between(everyDay, bar.endTime).toHours() >= 24) {
                println("\"EOD\",\"${LocalDate.from(bar!!.endTime)}\",${trader.usd.round2()},${trader.asset.round5()},${bar.closePrice.round2()},${(trader.usd + trader.asset * bar.closePrice).toInt()}")
                trader.history(state.startTime, bar.endTime).storeAsJson(state.historyPath)
                everyDay = bar.endTime
            }
        }

        val tradeHistory = trader.history(bars.first().endTime, bars.last().endTime)

        tradeHistory.storeAsJson(state.historyPath)

        if (state.graph) {
            Graph().drawHistory(tradeHistory)
        }
    }

    fun tuneParams(endTime: ZonedDateTime, curParams: Any, maxParamsDeviation: Double, populationSize: Int, inclCurParams: Boolean = true): TrainItem<Any, TradesStats, Metrica> {

        //tmpLogic нужно для генерации population и передачи tmpLogic.genes в getBestParams
        val tmpLogic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, state.interval)
        tmpLogic.setParams(curParams)
        if (maxParamsDeviation>0)
            tmpLogic.setMinMax(curParams, maxParamsDeviation, state.hardBounds)

        val population = tmpLogic.seed(SeedType.RANDOM, populationSize)
        if (inclCurParams)
            population.add(curParams)

        val trainStart = endTime.minusDays(state.trainDays)

        val bars = exchange.loadBars(cmd.instrument, state.interval, trainStart.minus(state.interval.duration.multipliedBy(tmpLogic.historyBars)).truncatedTo(state.interval), endTime.truncatedTo(state.interval))
        println("Searching best strategy for ${cmd.instrument} population=${population.size}, start=${trainStart} end=${endTime}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${tmpLogic.logState()}")
        bars.checkBars()


        val result = trainer.getBestParams(tmpLogic.genes, population,
                {
                    val history = ProfitCalculator().tradeHistory(cmd.logicName, it, cmd.instrument, state.interval, exchange.getFee(), bars, arrayListOf(Pair(trainStart, endTime)), false)
                    val stats = StatsCalculator().stats(history)
                    stats
                },
                { args, stats ->
                    tmpLogic.metrica(args, stats)
                },
                { tmpLogic.copyParams(it) })
        if (state.saveDaysData) {
            File("pops").mkdir()
            File("pops/${endTime.toEpochSecond()}.population").printWriter().use { out ->
                out.println(result.map { it -> TrainItem(it.args, TradesStatsShort(it.result.trades, it.result.profit, it.result.pearson), it.metrica) }.toJson())
            }
        }

        return result.last()
    }

    fun saveState() {
        state.storeAsJson(statePath)
    }

    fun readState(): State {
        try {
            return loadFromJson(statePath)
        } catch (e: Exception) {
            println("Create new ${statePath}")
            return State()
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
