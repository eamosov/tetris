package ru.efreet.trading

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.Trader
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
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class State(var name: String,
                 var time: String,
                 var startTime: String = time,
                 var usd: Double = 1000.0,
                 var asset: Double = 0.0,
                 var trainDays: Long = 10,
                 var instrument: Instrument,
                 var interval: BarInterval,
                 var minTrainTrades: Int = 10,
                 var maxParamsDeviation: Double = 10.0,
                 var hardBounds: Boolean = true,
                 var useLastProps: Boolean = false,
                 val tradeDuration: Duration = Duration.ofHours(24),
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

    fun dumpTraderHistory(trader: Trader) {
        val traderHistory = trader.history(ZonedDateTime.parse(state.startTime), state.getTime())
        Files.newOutputStream(Paths.get("history.data"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
            val dos = ObjectOutputStream(it)
            dos.writeObject(traderHistory)
            dos.close()
        }
    }

    fun run(state: State) {

        this.state = state

        cache = BarsCache(cmd.cachePath)
        val realExchange = Exchange.getExchange(cmd.exchange)
        exchange = CachedExchange(realExchange.getName(), realExchange.getFee(), cmd.barInterval, cache)

        state.name = cmd.logicName
        cmd.start?.let {
            state.setTime(it)
            state.startTime = it.toString()
        }

        if (cmd.resetUsd) {
            state.usd = 1000.0
            state.asset = 0.0
        }

        state.instrument = cmd.instrument
        state.interval = cmd.barInterval


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

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                try {
                    dumpTraderHistory(trader)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })

        var stats: TradesStats? = null

        while (state.getTime().plus(state.tradeDuration).isBefore(end)) {

            if (cmd.train!! > 0) {

                while (true) {
                    val (params, _stats) = tuneParams(state.trainDays, logic.getParams()!!)
//                val (params, _stats) = tuneParams(cmd.train!!.toLong(), logic.getParams()!!)

                    if (_stats.trades < state.minTrainTrades || _stats.profit < 1.1) {
                        state.trainDays++
                    } else if (_stats.trades > state.minTrainTrades) {
                        state.trainDays--
                    }

                    if (_stats.trades >= state.minTrainTrades && _stats.profit >= 1.1) {
                        logic.setParams(params)
                        stats = _stats
                        println("STRATEGY: ${logic.getParams()} $stats")
                        break
                    } else {
                        continue
                    }
                }
            }

            saveState()

            val newBars = cache.getBars(exchange.getName(), state.instrument, state.interval, state.getTime(), state.getTime().plus(state.tradeDuration))

            var bar: XBar? = null

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
            }

            state.setTime(state.getTime().plus(state.tradeDuration))
            saveState()
        }

        Graph().drawHistory(trader.history(ZonedDateTime.parse(state.startTime), state.getTime()))
    }

    fun tuneParams(trainDays: Long, curParams: SimpleBotLogicParams): Pair<SimpleBotLogicParams, TradesStats> {
        val trainStart = state.getTime().minusDays(trainDays)

        //tmpLogic нужно для генерации population и передачи tmpLogic.genes в getBestParams
        val tmpLogic: BotLogic<SimpleBotLogicParams> = LogicFactory.getLogic(state.name, state.instrument, state.interval)


        if (state.useLastProps) {
            tmpLogic.setParams(curParams)
            tmpLogic.setMinMax(curParams, state.maxParamsDeviation, state.hardBounds)
        } else {
            tmpLogic.loadState(cmd.settings!!)
            tmpLogic.setMinMax(tmpLogic.getParams(), state.maxParamsDeviation, state.hardBounds)
        }

        val population = tmpLogic.seed(cmd.seedType, state.population)
        population.add(curParams)

        val bars = exchange.loadBars(state.instrument, state.interval, trainStart.minus(state.interval.duration.multipliedBy(tmpLogic.historyBars)).truncatedTo(state.interval), state.getTime().truncatedTo(state.interval))
        println("Searching best strategy for ${state.instrument} population=${population.size}, start=${trainStart} end=${state.getTime()}. Loaded ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}. Logic settings: ${tmpLogic.logState()}")
        bars.checkBars()


        return CdmBotTrainer().getBestParams(tmpLogic.genes, population,
                {
                    val history = ProfitCalculator().tradeHistory(state.name, it, state.instrument, state.interval, exchange.getFee(), bars, arrayListOf(Pair(trainStart, state.getTime())), false)
                    val stats = StatsCalculator().stats(history)
                    stats
                },
                { args, stats ->
                    //tmpLogic.metrica(stats)
                    //BotLogic.fine(stats.sma10, 0.8, 10.0) + BotLogic.fine(stats.profit, 1.0) + stats.profit
                    BotLogic.fine(stats.sma10, 1.0, 10.0) + BotLogic.fine(stats.profit, 1.0) + stats.profit
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
            return State(name = "sd2", instrument = Instrument.BTC_USDT, interval = BarInterval.ONE_MIN, time = ZonedDateTime.now().toString())
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
