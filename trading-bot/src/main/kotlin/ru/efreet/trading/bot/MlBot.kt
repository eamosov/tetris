package ru.efreet.trading.bot

import org.eclipse.jetty.websocket.api.Session
import org.slf4j.LoggerFactory
import ru.efreet.telegram.Telegram
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.MarketBarFactory
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.marketBarList
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecordDao
import ru.efreet.trading.exchange.impl.Binance
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.utils.*
import java.io.FileNotFoundException
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.Executors

data class MlBotData(val instrument: Instrument, var logic: BotLogic<Any>?, var lastBar: ZonedDateTime = ZonedDateTime.now()) {
    var session: Session? = null
}

class MlBot {

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var cache: BarsCache
    private lateinit var exchange: Exchange
    private lateinit var trader: Trader
    private lateinit var marketBarFactory: MarketBarFactory
    private var telegram: Telegram? = null

    private val bots = mutableMapOf<Instrument, MlBotData>()

    private val balanceTimer = Periodical(Duration.ofMinutes(5))

    private inline fun <T> logDuration(comment: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val end = System.currentTimeMillis()
            if (end - start > 1000) {
                log.warn("{}: {}ms", comment, end - start)
            }
        }
    }

    private fun onBar(instrument: Instrument, bar: XBar, isFinal: Boolean) {

        val start = System.currentTimeMillis()

        try {
            val bot = bots[instrument]!!

            bot.lastBar = ZonedDateTime.now()

            if (!isFinal) {
                log.debug("Receive bar for {}: {}", instrument, bar)
            } else {

                if (log.isDebugEnabled)
                    log.debug("Receive final bar for {}: {}", instrument, bar)
                else if (bot.logic != null)
                    log.info("Receive final bar for {}: {}", instrument, bar)

                logDuration("Saving bar ${bar}") {
                    cache.saveBar(exchange.getName(), instrument, bar)
                }

                if (bot.logic != null) {
                    val marketBar = marketBarFactory.build(bar.endTime.withSecond(59).minus(interval.duration))

                    log.info("Market bar: {}", marketBar)

                    val advice = bot.logic!!.getAdvice(bar, marketBar)

                    if (Duration.between(bar.endTime, ZonedDateTime.now()).toMinutes() > 1) {
                        log.error("Skip bar {}", bar)
                        telegram?.sendMessage("Skip bar $bar")
                    } else {

                        if (advice.decision != Decision.NONE) {
                            log.info("ADVICE: {}", advice)
                        }

                        val trade = try {
                            logDuration("Executing advise") { tryMultipleTimes(5) { trader.executeAdvice(advice) } }
                        } catch (e: Throwable) {
                            if (advice.decision != Decision.NONE) {
                                telegram?.sendMessage("Couldn't execute advice \"${advice.log()}\": $e")
                            }
                            throw e
                        }

                        if (trade != null) {
                            log.info("TRADE: $trade")
                        }
                    }
                }
            }

            balanceTimer.invoke({
                tryMultipleTimes(5) {
                    trader.logBalance()
                }

            })

        } catch (e: Throwable) {
            log.error("Error in onBar", e)
        } finally {
            val end = System.currentTimeMillis()
            if (end - start > 1000) {
                log.warn("Processing bar {} with {}ms", bar.toString(), end - start)
            }
        }
    }

    fun startTrade(bot: MlBotData) {
        bot.session?.close()
        bot.session = (exchange as Binance).startTrade(bot.instrument, interval) { bar, isFinal ->
            executor.submit { onBar(bot.instrument, bar, isFinal) }
        }
        bot.lastBar = ZonedDateTime.now()
    }

    fun updateCache() {
        log.info("Refresh cache of Bars")
        for (instrument in exchange.getTicker().keys.toList().marketBarList()) {

            cache.createTable(exchange.getName(), instrument, interval)

            val cacheStart = cache.getLast(exchange.getName(), instrument, interval)?.endTime?.minus(interval.duration)
                    ?: ZonedDateTime.parse("2017-10-01T00:00Z[GMT]")

            log.info("Fetching ${instrument}/${interval.duration} from ${exchange.getName()} between ${cacheStart} and now ")
            val cacheBars = exchange.loadBars(instrument, interval, cacheStart, ZonedDateTime.now())

            log.info("Saving ${cacheBars.size} bars from ${cacheBars.first().endTime} to ${cacheBars.last().endTime}")
            cache.saveBars(exchange.getName(), instrument, cacheBars.filter { it.timePeriod == interval.duration })

            bots.putIfAbsent(instrument, MlBotData(instrument, null))
        }
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

        updateCache()

        val startTime = ZonedDateTime.now()

        val tmpLogic: BotLogic<Any> = LogicFactory.getLogic(logicName, Instrument.BTC_USDT, interval, simulate = true)
        val historyStart = startTime.minus(interval.duration.multipliedBy(tmpLogic.historyBars))

        marketBarFactory = MarketBarFactory(cache, interval, exchange.getName())

        log.info("Start building history of MarketBars")
        val mbs = System.currentTimeMillis()
        val marketBarsList = marketBarFactory.build(historyStart, startTime)
        log.info("Ok building {} MarketBars with {}s", marketBarsList.size, (System.currentTimeMillis() - mbs) / 1000)

        for ((instrument, _) in botConfig.instruments) {

            val logic: BotLogic<Any> = LogicFactory.getLogic(logicName, instrument, interval)

            val history = cache.getBars(exchange.getName(), logic.instrument, interval, historyStart, startTime)
            log.info("Loaded history ${history.size} bars from $historyStart to ${startTime} for ${logic.instrument}")

            log.info("Training model for {}", instrument)
            logic.setHistory(history, marketBarFactory.trim(marketBarsList, history))

            bots[instrument] = MlBotData(instrument, logic)
        }

        val tradeRecordDao = TradeRecordDao(cache.getConnection())

        telegram = if (botConfig.telegram) {
            Telegram.create()
        } else null

        trader = Trader(tradeRecordDao, exchange, botConfig.usdLimit, botConfig.instruments, telegram, botConfig.keepBnb)

        trader.logBalance()

        //update cache second time
        updateCache()

        bots.forEach { _, bot ->
            startTrade(bot)
        }

        telegram?.sendMessage("Bot have been started with config: ${botConfig.toString()}, total: ${trader.deposit().round2()}$, BNB: ${trader.balances["BNB"]?.round2()
                ?: 0.0F})")
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


    fun <R> tryMultipleTimes(times: Int, block: () -> R): R {
        var i = 0
        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                i++

                if (i < times) {
                    log.warn("Exception in operation, trying to repeat it", e)
                } else {
                    throw e
                }
            }
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(MlBot::class.java)

        val configPath = "bot.json"
        val interval = BarInterval.ONE_MIN
        val logicName = "ml"

        @JvmStatic
        fun main(args: Array<String>) {

            MlBot().start(args)

        }
    }
}