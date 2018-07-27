package ru.efreet.trading.exchange.impl

import com.cf.client.poloniex.PoloniexExchangeService
import org.java_websocket.client.WebSocketClient
import org.slf4j.LoggerFactory
import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.round
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.stream.Collectors


/**
 * Created by fluder on 08/02/2018.
 */
class Poloniex() : Exchange {

    private val log = LoggerFactory.getLogger(Poloniex::class.java)

    val apiKey: String
    val apiSecret: String

    init {
//        val properties = Poloniex::class.java.getResourceAsStream("resource.properties").use {
//            val properties = Properties()
//            properties.load(it)
//            properties
//        }
//        apiKey = properties.getProperty("POLONIEX_API_KEY")
//        apiSecret = properties.getProperty("POLONIEX_SECRET_KEY")
        apiKey = "QZ1Z6Q4T-1NHKN160-869RSEOW-91YPTVX9"
        apiSecret = "2dac8206d5f04a7d138391b0ab525523ab37be63198497cb1c39e84f3f5049e56bb76852404bd4df263fa947de69f936d1723d337bfdacba36c22161902f5f17"
    }

    private val service = PoloniexExchangeService(apiKey, apiSecret)

    //private var webSocketClient: WebSocketClient? = null

    override fun getName(): String = "poloniex"

    private fun symbol(instrument: Instrument): String = "${instrument.base}_${instrument.asset}"

    override fun getBalancesMap(): Map<String, Double> = service.returnBalance(true).mapValues { it.value.available!!.toDouble() }

    override fun buy(instrument: Instrument, asset: Double, price: Double, type: OrderType): Order {
        val _price = if (type == OrderType.LIMIT) {
            BigDecimal.valueOf(price).round()
        } else {
            service.returnTicker(symbol(instrument)).lowestAsk
        }

        val _amount = BigDecimal.valueOf(asset).round()
        log.info("TRY BUY ORDER: $instrument $_price $_amount $type")
        val result = service.buy(symbol(instrument), _price, _amount, false, false, false)
        return Order(result.orderNumber.toString(),
                instrument,
                _price.toDouble(),
                _amount.toDouble(),
                type,
                Decision.BUY,
                ZonedDateTime.now())
    }

    override fun sell(instrument: Instrument, asset: Double, price: Double, type: OrderType): Order {

        val _price = if (type == OrderType.LIMIT) {
            BigDecimal.valueOf(price).round()
        } else {
            service.returnTicker(symbol(instrument)).highestBid
        }

        val _amount = BigDecimal.valueOf(asset).round()
        log.info("TRY SELL ORDER: $instrument $_price $_amount $type")
        val result = service.sell(symbol(instrument), _price, _amount, false, false, false)
        return Order(result.orderNumber.toString(),
                instrument,
                _price.toDouble(),
                _amount.toDouble(),
                type,
                Decision.SELL,
                ZonedDateTime.now())

    }

    override fun loadBars(instrument: Instrument, interval: BarInterval, startTime: ZonedDateTime, endTime: ZonedDateTime): List<XBar> {
        return service.returnChartData(symbol(instrument), interval.duration.toMillis() / 1000, startTime.toEpochSecond()).map {
            XBaseBar(
                    interval.duration,
                    it.date,
                    it.open.toDouble(),
                    it.high.toDouble(),
                    it.low.toDouble(),
                    it.close.toDouble(),
                    it.volume.toDouble())
        }
    }

    override fun getLastTrades(instrument: Instrument): List<AggTrade> {
        return arrayListOf()
    }

//    override fun startTrade(instrument: Instrument, consumer: (AggTrade) -> Unit) {
//
//        webSocketClient = object : WebSocketClient(URI("wss://api2.poloniex.com"), Draft_6455()) {
//            override fun onMessage(message: String) {
//                val packet = JSONArray(message)
//
//                if (packet.length() == 3) {
//                    val frames = packet.getJSONArray(2)
//
//                    for (frame in frames) {
//                        if ((frame as JSONArray).getString(0) == "t") {
//
//                            consumer(AggTrade(frame.getLong(5) * 1000L, frame.getDouble(3), frame.getDouble(4)))
//                        }
//                    }
//                }
//
//                //val obj = JSONObject(message)
//
//            }
//
//            override fun onOpen(handshake: ServerHandshake) {
//                println("opened connection")
//
//                val obj = JSONObject()
//                obj.put("command", "subscribe")
//                obj.put("channel", symbol(instrument))
//                val message = obj.toString()
//                //send message
//                webSocketClient!!.send(message)
//            }
//
//            override fun onClose(code: Int, reason: String, remote: Boolean) {
//                println("closed connection")
//            }
//
//            override fun onError(ex: Exception) {
//                ex.printStackTrace()
//            }
//
//        }
//
//        webSocketClient!!.connect()
//    }

//    override fun startTrade(instrument: Instrument, interval: BarInterval, consumer: (XBar, Boolean) -> Unit) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun stopTrade() {
//
//        if (webSocketClient != null)
//            webSocketClient!!.close()
//        webSocketClient = null
//    }

    override fun getFee(): Double {
        return 0.4
    }

    override fun getIntervals(): List<BarInterval> {
        return arrayListOf(BarInterval.FIVE_MIN, BarInterval.FIFTEEN_MIN)
    }

    override fun getTicker(): Map<Instrument, Ticker> {

        return service.returnTicker()
                .entries
                .stream()
                .collect(Collectors.toMap(
                        { Instrument(it.key.split("_")[1], it.key.split("_")[0]) },
                        { Ticker(Instrument(it.key.split("_")[1], it.key.split("_")[0]), it.value.highestBid.toDouble(), it.value.lowestAsk.toDouble()) }))

    }

    override fun getOpenOrders(instrument: Instrument): List<Order> {
        throw NotImplementedError()
    }

    override fun cancelOrder(order: Order) {
        throw NotImplementedError()
    }
}
