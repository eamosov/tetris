package ru.efreet.trading.exchange.impl

import com.webcerebrium.binance.api.BinanceApi
import com.webcerebrium.binance.datatype.*
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterAggTrades
import org.eclipse.jetty.websocket.api.Session
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.round
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.stream.Collectors

/**
 * Created by fluder on 08/02/2018.
 */
class Binance() : Exchange {

    private var session: Session? = null

    private val api = BinanceApi()

    init {
        api.setBaseUrl("https://us.binance.com/api/")
        api.setBaseWapiUrl("https://us.binance.com/wapi/")
    }

    override fun getName(): String = "binance"

    override fun getBalancesMap(): Map<String, Double> {
        return api.balancesMap().mapValues { it.value.free.toDouble() }
    }

    fun symbol(instrument: Instrument): BinanceSymbol {
        return BinanceSymbol(instrument.asset + instrument.base)
    }

    fun orderType(type: OrderType): BinanceOrderType {
        return when (type) {
            OrderType.LIMIT -> BinanceOrderType.LIMIT
            OrderType.MARKET -> BinanceOrderType.MARKET
        }
    }

    fun interval(barInterval: BarInterval): BinanceInterval {
        return when (barInterval) {
            BarInterval.ONE_MIN -> BinanceInterval.ONE_MIN
            BarInterval.FIVE_MIN -> BinanceInterval.FIVE_MIN
            BarInterval.FIFTEEN_MIN -> BinanceInterval.FIFTEEN_MIN
            BarInterval.ONE_HOUR -> BinanceInterval.ONE_HOUR
            BarInterval.TWO_HOURS -> BinanceInterval.TWO_HOURS
            BarInterval.ONE_DAY -> BinanceInterval.ONE_DAY
            else -> throw RuntimeException("unknown $barInterval")
        }
    }

    override fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord {

        val placement = BinanceOrderPlacement(symbol(instrument), BinanceOrderSide.BUY)
        placement.setType(orderType(type))
        placement.setPrice(BigDecimal.valueOf(price).round())
        placement.setQuantity(BigDecimal.valueOf(asset).round())
        val order = api.getOrderById(symbol(instrument), api.createOrder(placement).get("orderId").asLong)
        return TradeRecord(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(order.time), ZoneId.of("GMT")),
                order.orderId.toString(),
                instrument,
                order.price.toDouble(),
                OrderSide.BUY,
                false,
                type,
                order.origQty.toDouble())
    }

    override fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType): TradeRecord {

        val placement = BinanceOrderPlacement(symbol(instrument), BinanceOrderSide.SELL)
        placement.setType(orderType(type))
        placement.setPrice(BigDecimal.valueOf(price).round())
        placement.setQuantity(BigDecimal.valueOf(asset).round())
        val order = api.getOrderById(symbol(instrument), api.createOrder(placement).get("orderId").asLong)
        return TradeRecord(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(order.time), ZoneId.of("GMT")),
                order.orderId.toString(),
                instrument,
                order.price.toDouble(),
                OrderSide.SELL,
                false,
                type,
                order.origQty.toDouble())
    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        val zone = ZoneId.of("GMT")
        val bars = mutableListOf<XBar>()
        var nextStartTime = startTime.toEpochSecond() * 1000

        do {
            val candles = api.klines(symbol(instrument), interval(interval), 500, hashMapOf(Pair("startTime", nextStartTime),
                    Pair("endTime", endTime.toEpochSecond() * 1000)))

            candles.forEach {
                val bar = XBaseBar(Duration.ofMillis(it.closeTime - it.openTime + 1),
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.closeTime), zone),
                        it.open.toDouble(),
                        it.high.toDouble(),
                        it.low.toDouble(),
                        it.close.toDouble(),
                        it.volume.toDouble())

                bars.add(bar)
            }

            if (!candles.isEmpty())
                nextStartTime = candles.last().closeTime + 1

        } while (candles.size == 500)

        println("loaded series for ${symbol(instrument)} / $interval from ${bars.first().endTime} to ${bars.last().endTime} (${bars.size} bars)")
        return bars
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> =
            api.aggTrades(symbol(instrument)).map { AggTrade(it.timestamp, it.price.toDouble(), it.quantity.toDouble()) }


    override fun startTrade(instrument: Instrument, consumer: (AggTrade) -> Unit) {

        session = api.websocketTrades(symbol(instrument), object : BinanceWebSocketAdapterAggTrades() {
            override fun onMessage(message: BinanceEventAggTrade) {

                consumer(AggTrade(message.eventTime, message.price.toDouble(), message.quantity.toDouble()))

            }
        })
    }

    override fun stopTrade() {
        if (session != null) {
            session!!.close()
            session = null
        }
    }

    override fun getFee(): Double {
        return 0.1
    }

    override fun getIntervals(): List<BarInterval> {
        return arrayListOf(BarInterval.ONE_MIN, BarInterval.FIVE_MIN, BarInterval.FIFTEEN_MIN)
    }

    private fun asInstrument(symbol: String): Instrument {
        if (symbol.endsWith("USDT"))
            return Instrument(symbol.substring(0, symbol.length - 4), "USDT")
        else
            return Instrument(symbol.substring(0, symbol.length - 3), symbol.substring(symbol.length - 3))
    }

    override fun getTicker(): Map<Instrument, Ticker> {

        return api.allBookTickersMap().values
                .stream()
                .collect(Collectors.toMap({ asInstrument(it.symbol) }, { Ticker(asInstrument(it.symbol), it.bidPrice.toDouble(), it.askPrice.toDouble()) }))
    }
}