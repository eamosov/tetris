package ru.efreet.trading

import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.bot.TradesStatsShort
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
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

data class State(var startTime: ZonedDateTime = ZonedDateTime.parse("2018-02-01T00:00Z[GMT]"),
                 var endTime: ZonedDateTime = ZonedDateTime.parse("2018-06-01T00:00Z[GMT]"),
                 var instruments: List<Instrument> = arrayListOf(Instrument.ETH_USDT, Instrument.BNB_USDT, Instrument.BTC_USDT, Instrument.BCC_USDT, Instrument.LTC_USDT),
                 var usd: Double = 1000.0,
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
                 var feeFactor: Double = 1.0,
                 var historyPath: String = "simulate_history.json",
                 var properties: String = "simulate.properties",
                 var initOptimisation: Boolean = false,
                 var periodicalTrain: Boolean = false,
                 var graph: Boolean = true)

data class SimulateData(val instrument: Instrument,
                        var logic: BotLogic<Any>,
                        var lastTrainTime: ZonedDateTime,
                        var everyDay: ZonedDateTime,
                        var skipBuy: Boolean = false,
                        val barIterator: Iterator<XBaseBar>
)

class Simulate(val cmd: CmdArgs, val statePath: String) {

    //lateinit var lastBar: Bar
    lateinit var exchange: Exchange
    lateinit var cache: BarsCache
    lateinit var state: State
    var trainer = cmd.makeTrainer<Any, TradesStats, Metrica>()

    fun run(state: State) {

        this.state = state

        cache = BarsCache(cmd.cachePath)

        val realExchange = Exchange.getExchange(cmd.exchange)
        exchange = CachedExchange(realExchange.getName(), realExchange.getFee() * state.feeFactor, state.interval, cache)

        val trader = FakeTrader(state.usd, exchange.getFee(), exchange.getName())

        val simulateData = arrayListOf<SimulateData>()

        for (instrument in state.instruments) {
            val logic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, instrument, state.interval, simulate = true)
            if (!logic.loadState(state.properties)){
                logic.saveState(state.properties, "initial properties")
            }

            if (state.initOptimisation) {

                val (params, _stats) = tuneParams(instrument, state.startTime, logic.getParams(), state.startMaxParamsDeviation, state.startPopulation, false)
                logic.setParams(params)

                if (!state.noSaveState) {

                    if (state.instruments.size != 1)
                        throw RuntimeException("state.instruments.size must by 1 for initOptimisation and noSaveState==false")

                    logic.saveState(state.properties, _stats.toString())
                }
                println("Initial optimisation of random parameters: ${params} $_stats")
            }

            println("Logic state for $instrument:")
            println(logic.logState())

            val historyStart = state.startTime.minus(state.interval.duration.multipliedBy(logic.historyBars))
            val history = cache.getBars(exchange.getName(), logic.instrument, state.interval, historyStart, state.startTime)
            println("Loaded history ${history.size} bars from $historyStart to ${state.startTime} for ${logic.instrument}")

            history.forEach { logic.insertBar(it) }
            logic.prepareBars()

            val bars = cache.getBars(exchange.getName(), instrument, state.interval, state.startTime, state.endTime)
            println("Loaded ${bars.size} bars from ${state.startTime} to ${state.endTime}")

            val lastTrainTime = bars.first().endTime
            val everyDay = bars.first().endTime

            simulateData.add(SimulateData(instrument, logic, lastTrainTime, everyDay, false, bars.iterator()))
        }

        var hasNext: Boolean = true

        while (hasNext) {

            hasNext = false

            for (sd in simulateData) {
                if (sd.barIterator.hasNext()) {
                    hasNext = true

                    val bar = sd.barIterator.next()

                    if (state.periodicalTrain &&
                            Duration.between(sd.lastTrainTime, bar.endTime).toMillis() >= state.trainPeriod.toMillis()
                            && trader.availableAsset(sd.instrument) == 0.0) { //если мы в баксах
                        sd.logic.loadState(state.properties)
                        val (params, _stats) = tuneParams(sd.instrument, bar.endTime, sd.logic.getParams(), state.maxParamsDeviation, state.population)

                        sd.logic.setParams(params)
                        if (!state.noSaveState) {

                            if (state.instruments.size != 1)
                                throw RuntimeException("state.instruments.size must by 1 for initOptimisation and noSaveState==false")

                            sd.logic.saveState(state.properties, _stats.toString())
                        }

                        if (state.saveDaysData) {
                            File("props").mkdir()
                            sd.logic.saveState("props/${sd.instrument}_${bar.endTime.toEpochSecond()}.properties", _stats.toString())
                        }
                        println("STRATEGY: ${sd.logic.getParams()} $_stats")
                        sd.lastTrainTime = bar.endTime
                        sd.skipBuy = true
                    }

                    sd.logic.insertBar(bar)
                    val advice = sd.logic.getAdvice(trader, true)

                    if (sd.skipBuy) {
                        if (advice.decision == Decision.BUY) {
                            println("skip buy")
                            continue
                        } else {
                            sd.skipBuy = false
                        }
                    }

                    val trade = trader.executeAdvice(advice)

                    if (trade != null) {
                        println("TRADE: $trade")
                    }

                    if (Duration.between(sd.everyDay, bar.endTime).toHours() >= 24) {
                        println("\"EOD(${sd.instrument})\",\"${LocalDate.from(bar.endTime)}\",${trader.usd.round2()},${bar.closePrice.round2()},${(trader.funds()).toInt()}")
                        trader.history().storeAsJson(state.historyPath)
                        sd.everyDay = bar.endTime
                    }
                }
            }
        }


        val tradeHistory = trader.history()

        tradeHistory.storeAsJson(state.historyPath)

        if (state.graph) {
            Graph().drawHistory(tradeHistory)
        }
    }

    fun tuneParams(instrument: Instrument, endTime: ZonedDateTime, curParams: Any, maxParamsDeviation: Double, populationSize: Int, inclCurParams: Boolean = true): TrainItem<Any, TradesStats, Metrica> {

        //tmpLogic нужно для генерации population и передачи tmpLogic.genes в getBestParams
        val tmpLogic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, instrument, state.interval, simulate = true)
        tmpLogic.setParams(curParams)
        if (maxParamsDeviation > 0)
            tmpLogic.setMinMax(curParams, maxParamsDeviation, state.hardBounds)

        val population = tmpLogic.seed(SeedType.RANDOM, populationSize)
        if (inclCurParams)
            population.add(curParams)

        val trainStart = endTime.minusDays(state.trainDays)

        val bars = exchange.loadBars(instrument, state.interval, trainStart.minus(state.interval.duration.multipliedBy(tmpLogic.historyBars)).truncatedTo(state.interval), endTime.truncatedTo(state.interval))
        println("Searching best strategy for ${instrument} population=${population.size}, start=${trainStart} end=${endTime}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${tmpLogic.logState()}")
        bars.checkBars()


        val result = trainer.getBestParams(tmpLogic.genes, population,
                {
                    val history = ProfitCalculator().tradeHistory(cmd.logicName, it, instrument, state.interval, exchange.getFee(), bars, arrayListOf(Pair(trainStart, endTime)), false)
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
            val state =  State()
            state.storeAsJson(statePath)
            return state
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
