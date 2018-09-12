package ru.efreet.trading.simulate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.efreet.trading.bars.MarketBarFactory
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bot.TradeHistory
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.exchange.impl.cache.CachedExchange
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.loadFromJson
import ru.efreet.trading.utils.round2
import ru.efreet.trading.utils.storeAsJson
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class State(var startTime: ZonedDateTime = ZonedDateTime.parse("2018-02-01T00:00Z[GMT]"),
                 var endTime: ZonedDateTime = ZonedDateTime.parse("2018-06-01T00:00Z[GMT]"),
                 var instruments: Map<Instrument, Float> = listOf(Instrument.ETH_USDT to 0.5F).toMap(),
                 var usd: Float = 1000.0F,
                 val usdLimit: Float = 1.0f,
                 var interval: BarInterval = BarInterval.ONE_MIN,
                 var feeFactor: Float = 1.0f,
                 var historyPath: String = "simulate_history.json",
                 var properties: String = "simulate.properties",
                 var graph: Boolean = true) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(Simulator::class.java)

        fun load(statePath: String): State {
            try {
                return loadFromJson(statePath)
            } catch (e: Exception) {
                log.info("Create new ${statePath}")
                val state = State()
                state.storeAsJson(statePath)
                return state
            }
        }
    }
}

data class SimulateData(val instrument: Instrument,
                        var logic: BotLogic<Any>,
                        var lastTrainTime: ZonedDateTime,
                        var everyDay: ZonedDateTime,
                        val barIterator: Iterator<XBar>
)

class Simulator(val cmd: CmdArgs) {

    val cache = BarsCache(cmd.cachePath)
    private val realExchange = Exchange.getExchange(cmd.exchange)

    fun run(state: State): TradeHistory {

        val exchange = CachedExchange(realExchange.getName(), realExchange.getFee() * state.feeFactor, state.interval, cache)

        exchange.setBalance("USDT", state.usd)

        val trader = Trader(null, exchange, state.usdLimit, state.instruments, updateBalancesTimeout = -1, updateTickerTimeout = -1)

        val simulateData = arrayListOf<SimulateData>()

        val marketBarFactory = MarketBarFactory(cache, state.interval, realExchange.getName())


        val tmpLogic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, Instrument.BTC_USDT, state.interval, simulate = true)
        val historyStart = state.startTime.minus(state.interval.duration.multipliedBy(tmpLogic.historyBars))

        log.info("Start building history of MarketBars")
        val mbs = System.currentTimeMillis()
        val marketBarsList = marketBarFactory.build(historyStart, state.endTime)
        val marketBarsMap = marketBarsList.map { it.endTime.withSecond(59) to it }.toMap()
        log.info("Ok building {} MarketBars with {}s", marketBarsMap.size, (System.currentTimeMillis() - mbs) / 1000)

        for (instrument in state.instruments.keys) {

            exchange.setBalance(instrument.asset, 0.0f)

            val logic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, instrument, state.interval, simulate = true)
            if (!logic.loadState(state.properties)) {
                logic.saveState(state.properties, "initial properties")
            }

            log.info("Logic state for $instrument:")
            log.info(logic.logState())

            val history = cache.getBars(exchange.getName(), logic.instrument, state.interval, historyStart, state.startTime)
            log.info("Loaded history ${history.size} bars from $historyStart to ${state.startTime} for ${logic.instrument}")

            logic.setHistory(history, marketBarFactory.trim(marketBarsList, history))

            val bars = cache.getBars(exchange.getName(), instrument, state.interval, state.startTime, state.endTime)
            log.info("Loaded ${bars.size} bars from ${state.startTime} to ${state.endTime}")

            val lastTrainTime = bars.first().endTime
            val everyDay = bars.first().endTime

            simulateData.add(SimulateData(instrument, logic, lastTrainTime, everyDay, bars.iterator()))
        }

        exchange.setBalance("BNB", 1.0f)

        var hasNext: Boolean = true

        while (hasNext) {

            hasNext = false

            for (sd in simulateData) {
                if (sd.barIterator.hasNext()) {
                    hasNext = true

                    val bar = sd.barIterator.next()

                    val advice = sd.logic.getAdvice(bar, marketBarsMap[bar.endTime.withSecond(59).minus(state.interval.duration)])

                    val trade = trader.executeAdvice(advice)

                    if (trade != null) {
                        log.info("TRADE: $trade")
                    }

                    if (Duration.between(sd.everyDay, bar.endTime).toHours() >= 24) {
                        log.info("\"EOD(${sd.instrument})\",\"${LocalDate.from(bar.endTime)}\",${trader.usd.round2()},${bar.closePrice.round2()},${trader.balance(sd.instrument).round2()},${trader.deposit().toInt()}")
                        trader.history().storeAsJson(state.historyPath)
                        sd.everyDay = bar.endTime
                    }
                }
            }
        }

        return trader.history()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Simulator::class.java)
    }
}
