package ru.efreet.trading.bot

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.db.DatabaseType
import com.j256.ormlite.db.SqliteDatabaseType
import com.j256.ormlite.jdbc.JdbcDatabaseConnection
import com.j256.ormlite.jdbc.JdbcSingleConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import ru.efreet.trading.exchange.*
import ru.efreet.trading.utils.Periodical
import ru.efreet.trading.utils.roundAmount
import java.sql.Connection
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 23/02/2018.
 */
class RealTrader(tradesDbPath: String, jdbcConnection: Connection, val exchange: Exchange, val limit: Double, exchangeName: String, instrument: Instrument) : AbstractTrader(exchangeName, instrument) {

    var balanceResult: Exchange.CalBalanceResult
    var balanceUpdatedTimer = Periodical(Duration.ofMinutes(5))

    val startUsd: Double
    val startAsset: Double
    val startFunds: Double
    val baseName = instrument.base!!
    val assetName = instrument.asset!!

    var usd: Double
    var asset: Double
    var funds: Double

    private val dao: Dao<TradeRecord, String>
    private var lastTrade: TradeRecord? = null

    init {

        //Balances in USD
        balanceResult = exchange.calcBalance(baseName)

        startUsd = balanceResult.balances[baseName]!!
        startFunds = balanceResult.toBase["total"]!!
        startAsset = balanceResult.balances[assetName]!!

        usd = startUsd
        funds = startFunds
        asset = startAsset

        lastPrice = balanceResult.ticker[Instrument(assetName, baseName)]!!.highestBid
        minPrice = lastPrice
        maxPrice = lastPrice

        val jc = JdbcDatabaseConnection(jdbcConnection)

        val jdbcConn = object : ConnectionSource{
            override fun getReadOnlyConnection(tableName: String?): DatabaseConnection {
                return jc
            }

            override fun getDatabaseType(): DatabaseType {
                return SqliteDatabaseType()
            }

            override fun saveSpecialConnection(connection: DatabaseConnection?): Boolean {
                return false
            }

            override fun getReadWriteConnection(tableName: String?): DatabaseConnection {
                return jc
            }

            override fun isOpen(tableName: String?): Boolean {
                return true
            }

            override fun isSingleConnection(tableName: String?): Boolean {
                return true
            }

            override fun closeQuietly() {

            }

            override fun close() {

            }

            override fun releaseConnection(connection: DatabaseConnection?) {

            }

            override fun clearSpecialConnection(connection: DatabaseConnection?) {

            }

            override fun getSpecialConnection(tableName: String?): DatabaseConnection {
                return jc
            }
        }

        //JdbcSingleConnectionSource("jdbc:sqlite:" + tradesDbPath, jdbcConnection)
        //TableUtils.createTableIfNotExists(jdbcConn, TradeRecord::class.java)
        dao = DaoManager.createDao(jdbcConn, TradeRecord::class.java)
    }

    fun updateBalance(force: Boolean = false) {
        balanceUpdatedTimer.invoke({
            balanceResult = exchange.calcBalance(baseName)
        }, force)
    }

    override fun availableUsd(instrument: Instrument): Double {

        updateBalance()

        //на сколько куплено валюты
        val entered = balanceResult.toBase[instrument.asset]!!

        //полный размер депозита
        val total = balanceResult.toBase["total"]!!

        //осталось USD
        val free = balanceResult.balances[baseName]!!

        return minOf(total * limit - entered, free)
    }

    override fun availableAsset(instrument: Instrument): Double {

        updateBalance()

        balanceUpdatedTimer.invoke({
            balanceResult = exchange.calcBalance(baseName)
        })

        return balanceResult.balances[instrument.asset]!!
    }

    override fun executeAdvice(advice: Advice): TradeRecord? {
        super.executeAdvice(advice)

        if (lastTrade != null)
            dao.update(lastTrade)

        usd = balanceResult.balances[baseName]!!
        funds = balanceResult.toBase["total"]!!
        asset = balanceResult.balances[assetName]!!

        if (advice.orderSide?.side == OrderSide.BUY && advice.amount > 0) {

            if (advice.amount * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]!!
                val assetBefore = balanceResult.balances[advice.instrument.asset]!!
                val order = exchange.buy(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.LIMIT)

                updateBalance(true)

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument, order.price, order.side, order.type, order.amount,
                        0.0,
                        usdBefore,
                        assetBefore,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!,
                        balanceResult.toBase["total"]!!,
                        advice.orderSide.long, advice.sellBySl
                )

                trades.add(lastTrade!!)
                dao.create(lastTrade)
                return lastTrade
            }
        } else if (advice.orderSide?.side == OrderSide.SELL && advice.amount > 0) {
            if (advice.amount * advice.price >= 10) {

                val usdBefore = balanceResult.balances[baseName]
                val assetBefore = balanceResult.balances[advice.instrument.asset]
                val order = exchange.sell(advice.instrument, roundAmount(advice.amount, advice.price), advice.price, OrderType.LIMIT)

                updateBalance(true)

                lastTrade = TradeRecord(order.orderId, order.time, exchangeName, order.instrument, order.price, order.side, order.type, order.amount,
                        0.0,
                        usdBefore!!,
                        assetBefore!!,
                        balanceResult.balances[baseName]!!,
                        balanceResult.balances[advice.instrument.asset]!!,
                        balanceResult.toBase["total"]!!,
                        advice.orderSide.long, advice.sellBySl
                )

                trades.add(lastTrade!!)
                dao.create(lastTrade)
                return lastTrade
            }
        }

        return null
    }

    override fun history(start: ZonedDateTime, end: ZonedDateTime): TradeHistory {
        return TradeHistory(startUsd, startAsset, startFunds, usd, asset, funds, trades, indicators, arrayListOf(),
                startPrice,
                lastPrice,
                minPrice,
                maxPrice,
                start,
                end)
    }

    override fun lastTrade(): TradeRecord? {
        if (lastTrade == null) {
            var qb = dao.queryBuilder()
            qb.setWhere(qb.where().eq("instrument", instrument.toString()).and().eq("exchange", exchangeName))
            qb.limit(1)
            qb.orderBy("time", false)
            lastTrade = qb.iterator().first()
        }
        return lastTrade
    }
}