package ru.efreet.trading.exchange.impl

import com.webcerebrium.binance.api.BinanceApi
import com.webcerebrium.binance.datatype.*
import com.webcerebrium.binance.websocket.BinanceWebSocketAdapterKline
import org.eclipse.jetty.websocket.api.Session
import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBarList
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

    private val log = LoggerFactory.getLogger(Binance::class.java)

    private var session: Session? = null

    private val api = BinanceApi()

//    init {
//        api.setBaseUrl("https://us.binance.com/api/")
//        api.setBaseWapiUrl("https://us.binance.com/wapi/")
//    }

    override fun getName(): String = "binance"


    override fun getBalancesMap(): Map<String, Float> {
        return api.balancesMap().mapValues { it.value.free.toFloat() }
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

    override fun buy(instrument: Instrument, asset: Float, price: Float, type: OrderType, now: ZonedDateTime): Order {

        val placement = BinanceOrderPlacement(symbol(instrument), BinanceOrderSide.BUY)
        placement.setType(orderType(type))
        placement.setPrice(BigDecimal.valueOf(price.toDouble()).round())
        placement.setQuantity(BigDecimal.valueOf(asset.toDouble()).round())

        try {
            val order = api.getOrderById(symbol(instrument), api.createOrder(placement).get("orderId").asLong)
            return Order(
                    order.orderId.toString(),
                    instrument,
                    order.price.toFloat(),
                    asset,
                    type,
                    Decision.BUY,
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(order.time), ZoneId.of("GMT")))
        } catch (e: Throwable) {
            throw OrderException(instrument, placement.getQuantity().toFloat(), placement.getPrice().toFloat(), type, Decision.BUY, e)
        }

    }

    override fun sell(instrument: Instrument, asset: Float, price: Float, type: OrderType, now: ZonedDateTime): Order {

        val placement = BinanceOrderPlacement(symbol(instrument), BinanceOrderSide.SELL)
        placement.setType(orderType(type))
        placement.setPrice(BigDecimal.valueOf(price.toDouble()).round())
        placement.setQuantity(BigDecimal.valueOf(asset.toDouble()).round())

        try {
            val order = api.getOrderById(symbol(instrument), api.createOrder(placement).get("orderId").asLong)
            return Order(
                    order.orderId.toString(),
                    instrument,
                    order.price.toFloat(),
                    asset,
                    type,
                    Decision.SELL,
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(order.time), ZoneId.of("GMT")))
        } catch (e: Throwable) {
            throw OrderException(instrument, placement.getQuantity().toFloat(), placement.getPrice().toFloat(), type, Decision.SELL, e)
        }
    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        val zone = ZoneId.of("GMT")
        val bars = XBarList((Duration.between(startTime, endTime).toMinutes() / interval.duration.toMinutes()).toInt())
        var nextStartTime = startTime.toEpochSecond() * 1000

        do {
            val candles = api.klines(symbol(instrument), interval(interval), 500, hashMapOf(Pair("startTime", nextStartTime),
                    Pair("endTime", endTime.toEpochSecond() * 1000)))

            candles.forEach {
                val bar = XBaseBar(Duration.ofMillis(it.closeTime - it.openTime + 1),
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.closeTime), zone),
                        it.open.toFloat(),
                        it.high.toFloat(),
                        it.low.toFloat(),
                        it.close.toFloat(),
                        it.volume.toFloat(),
                        it.takerBuyBaseAssetVolume.toFloat(),
                        it.takerBuyQuoteAssetVolume.toFloat(),
                        it.numberOfTrades.toShort())

                bars.add(bar)
            }

            if (!candles.isEmpty())
                nextStartTime = candles.last().closeTime + 1

        } while (candles.size == 500)

        log.info("loaded series for ${symbol(instrument)} / $interval from ${bars.first().endTime} to ${bars.last().endTime} (${bars.size} bars)")
        return bars
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> =
            api.aggTrades(symbol(instrument)).map { AggTrade(it.timestamp, it.price.toFloat(), it.quantity.toFloat()) }


    fun startTrade(instrument: Instrument, interval: BarInterval, consumer: (XBar, Boolean) -> Unit): Session {

        return api.websocketKlines(symbol(instrument), interval(interval), object : BinanceWebSocketAdapterKline() {
            override fun onMessage(message: BinanceEventKline) {

                val bar = XBaseBar(Duration.ofMillis(message.endTime - message.startTime + 1),
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.endTime), ZoneId.of("GMT")),
                        message.open.toFloat(),
                        message.high.toFloat(),
                        message.low.toFloat(),
                        message.close.toFloat(),
                        message.volume.toFloat(),
                        message.volumeOfActiveBuy.toFloat(),
                        message.quoteVolumeOfActiveBuy.toFloat(),
                        message.numberOfTrades.toShort())

                consumer(bar, message.isFinal)
            }
        })
    }

//    override fun stopTrade() {
//        if (session != null) {
//            session!!.close()
//            session = null
//        }
//    }

    override fun getFee(): Float {
        return 0.1F
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
                .collect(Collectors.toMap({ asInstrument(it.symbol) }, { Ticker(asInstrument(it.symbol), it.bidPrice.toFloat(), it.askPrice.toFloat()) }))
    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        return api.openOrders(symbol(instrument)).map {
            Order(it.orderId.toString(),
                    instrument,
                    it.price.toFloat(),
                    it.origQty.toFloat() - it.executedQty.toFloat(),
                    OrderType.LIMIT,
                    when (it.side) {
                        BinanceOrderSide.BUY -> Decision.BUY
                        BinanceOrderSide.SELL -> Decision.SELL
                    },
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.time), ZoneId.of("GMT")))
        }
    }

    override fun cancelOrder(order: Order) {
        api.deleteOrderById(symbol(order.instrument), order.orderId.toLong())
    }
}