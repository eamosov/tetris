package ru.efreet.trading.bot

import org.eclipse.jetty.websocket.api.Session
import org.slf4j.LoggerFactory
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecordDao
import ru.efreet.trading.exchange.impl.Binance
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.loadFromJson
import ru.efreet.trading.utils.storeAsJson
import java.io.FileNotFoundException
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.Executors

data class MlBotData(val instrument: Instrument, var logic: BotLogic<Any>, var lastBar: ZonedDateTime = ZonedDateTime.now()) {
    var session: Session? = null
}

class MlBot {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var cache: BarsCache
    private lateinit var exchange: Exchange
    private lateinit var trader: Trader
    private val bots = mutableMapOf<Instrument, MlBotData>()

    private val balanceTimer = Periodical(Duration.ofMinutes(5))

    private fun onBar(instrument: Instrument, bar: XBar, isFinal: Boolean) {

        try {
            val bot = bots[instrument]!!

            bot.lastBar = ZonedDateTime.now()

            if (!isFinal) {
                log.debug("Receive bar for {}: {}", instrument, bar)
            } else {

                log.info("Receive final bar for {}: {}", instrument, bar)

                cache.saveBar(exchange.getName(), instrument, bar)

                bot.logic.insertBar(bar)
                val advice = bot.logic.getAdvice(true)
                trader.executeAdvice(advice)
            }

            balanceTimer.invoke({
                trader.logBalance()
            })

        } catch (e: Throwable) {
            log.error("Error in onBar", e)
        }
    }

    fun startTrade(bot: MlBotData) {
        bot.session?.close()
        bot.session = (exchange as Binance).startTrade(bot.instrument, interval) { bar, isFinal ->
            executor.submit { onBar(bot.instrument, bar, isFinal) }
        }
        bot.lastBar = ZonedDateTime.now()
    }

    fun start(args: Array<String>) {

        val cmd = CmdArgs.parse(args)

        exchange = Exchange.getExchange(cmd.exchange)
        cache = BarsCache(cmd.cachePath)


        val botConfig = try {
            loadFromJson<BotConfig>(configPath)
        } catch (e: FileNotFoundException) {
            val c = BotConfig()
            c.storeAsJson(configPath)
            c
        }

        log.info("Configuration: {}", botConfig)

        val startTime = ZonedDateTime.now()

        for (instrument in botConfig.instruments) {

            val cacheStart = cache.getLast(exchange.getName(), instrument, interval)?.endTime?.minus(interval.duration)
                    ?: ZonedDateTime.parse("2017-10-01T00:00Z[GMT]")
            log.info("Fetching ${instrument}/${interval.duration} from ${exchange.getName()} between ${cacheStart} and ${startTime} ")
            val cacheBars = exchange.loadBars(instrument, interval, cacheStart, startTime)
            log.info("Saving ${cacheBars.size} bars from ${cacheBars.first().endTime} to ${cacheBars.last().endTime}")
            cache.saveBars(exchange.getName(), instrument, cacheBars.filter { it.timePeriod == interval.duration })


            val logic: BotLogic<Any> = LogicFactory.getLogic("ml", instrument, interval)

            val historyStart = startTime.minus(interval.duration.multipliedBy(logic.historyBars))
            val history = cache.getBars(exchange.getName(), logic.instrument, interval, historyStart, startTime)
            log.info("Loaded history ${history.size} bars from $historyStart to ${startTime} for ${logic.instrument}")

            history.forEach { logic.insertBar(it) }

            log.info("Training model for {}", instrument)
            logic.prepareBars()
            bots[instrument] = MlBotData(instrument, logic)
        }

        val tradeRecordDao = TradeRecordDao(cache.getConnection())

        trader = Trader(tradeRecordDao, exchange, botConfig.usdLimit, botConfig.betLimit, bots.keys.toList())

        trader.logBalance()

        bots.forEach { _, bot ->
            startTrade(bot)
        }

        while (true) {
            Thread.sleep(1000)

            bots.forEach { instrument, bot ->

                if (Duration.between(bot.lastBar, ZonedDateTime.now()).toMinutes() > 2) {
                    log.error("No bars for {}, restart session", instrument)
                    startTrade(bot)
                }
            }
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(MlBot::class.java)

        val configPath = "bot.json"
        val interval = BarInterval.ONE_MIN

        @JvmStatic
        fun main(args: Array<String>) {

            MlBot().start(args)

        }
    }
}