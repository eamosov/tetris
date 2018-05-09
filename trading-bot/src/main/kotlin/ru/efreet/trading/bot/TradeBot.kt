package ru.efreet.trading.bot

import ru.efreet.trading.exchange.*
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.utils.Periodical
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Created by fluder on 06/02/2018.
 */
class TradeBot(val exchange: Exchange,
               var barsCache: BarsCache,
               var baseLimit: Double,
               val testOnly: Boolean,
               val instrument: Instrument,
               val logicName: String,
               val logic: BotLogic<Any>,
               val settings:String,
               val barInterval: BarInterval,
               val trainStart:ZonedDateTime,
               var orderListener: ((TradeBot, TradeRecord) -> Unit)? = null,
               val training: Boolean) {

    private val zone = ZoneId.of("GMT")

    private var lastTradeTime: ZonedDateTime = ZonedDateTime.now()

    private var logStateTimer = Periodical(Duration.ofMinutes(5))

    private val tradeRecordDao = TradeRecordDao(barsCache.getConnection())

    private val trader = when {
        testOnly -> FakeTrader(1000.0, 0.0, 0.02, true, exchange.getName(), instrument)
        else -> RealTrader(tradeRecordDao, exchange, baseLimit, exchange.getName(), instrument)
    }


    init {
        barsCache.createTable(exchange.getName(), instrument, BarInterval.ONE_SECOND)
    }

    fun logState() {
        println("instrument: $instrument")
        println("interval: $barInterval")
        println("testOnly: $testOnly")
        println("baseLimit: $baseLimit")
    }

    fun checkStrategy() {

        val advice = logic.getAdvice(null, trader, true)

        println("advice: ${advice}")

        try {
            val trade = trader.executeAdvice(advice)
            if (trade != null) {
                println("TRADE: $trade")
                orderListener?.invoke(this, trade)
            } else {
                println("no trade")
            }
        } catch (e: Exception) {
            println("ERROR")
            e.printStackTrace()
        }
    }

    fun startStrategy() {
        synchronized(this) {
            fetchTradesHistory()
            checkStrategy()
            startTrade()
        }
    }

    fun fetchTradesHistory() {

        val lastBars = barsCache.getBars(
                exchange.getName(),
                instrument,
                barInterval,
                ZonedDateTime.now().minus(barInterval.duration.multipliedBy(logic.historyBars)), ZonedDateTime.now())

        logic.insertBars(lastBars);

        logic.prepareBars()

        println("Ok fetchTradesHistory (${logic.barsCount()} bars from ${logic.firstBar().endTime} to ${logic.lastBar().endTime})")
    }


    fun startTrade() {

        synchronized(this) {
            exchange.startTrade(instrument, barInterval, { bar, isFinal ->

                synchronized(this) {
                    lastTradeTime = ZonedDateTime.now()

                    println("bar: final=$isFinal, $bar")

                    if (isFinal) {
                        barsCache.saveBar(exchange.getName(), instrument, bar)
                        logic.insertBar(bar)
                        checkStrategy()
                    }
                }
            })
        }
    }

    fun stopTrade() {
        synchronized(this) {
            exchange.stopTrade()
        }
    }

    fun periodic() {

        synchronized(this) {
            if (lastTradeTime.isBefore(ZonedDateTime.now().minusSeconds(30))) {
                println("ERROR, not trades for 30 seconds ($instrument) , restart listener")
                try {
                    stopTrade()
                } catch (e: Throwable) {
                    println("Error stopping listener: " + e)
                }

                try {
                    startTrade()
                } catch (e: Throwable) {
                    println("ERROR starting trade: " + e.message)
                }
            }

            logStateTimer.invoke({
                logState()
            })
        }

    }

}