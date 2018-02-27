package ru.efreet.trading.exchange.impl.cache

import org.sqlite.SQLiteException
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.utils.CmdArgs
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by fluder on 11/02/2018.
 */
class BarsCache(val path: String) {

    val url = "jdbc:sqlite:$path"

    val prop: Properties
    val conn: Connection

    init {
        prop = Properties()
        prop.setProperty("journal_mode", "WAL");
        conn = DriverManager.getConnection(url, prop)
    }

    fun getFirst(exchange: String, instrument: Instrument, interval: BarInterval): XBaseBar {

        conn.createStatement().use { statement ->
            val time = statement.executeQuery("select min(time) from ${tableName(exchange, instrument, interval)}").use { resultSet ->
                resultSet.next()
                resultSet.getLong(1)
            }

            statement.executeQuery("SELECT time, open, high, low, close, volume FROM ${tableName(exchange, instrument, interval)} WHERE time = ${time}").use { resultSet ->
                resultSet.next()
                return mapToBar(resultSet, interval);
            }
        }
    }

    fun getLast(exchange: String, instrument: Instrument, interval: BarInterval): XBaseBar {

        conn.createStatement().use { statement ->
            val time = statement.executeQuery("select max(time) from ${tableName(exchange, instrument, interval)}").use { resultSet ->
                resultSet.next()
                resultSet.getLong(1)
            }

            statement.executeQuery("SELECT time, open, high, low, close, volume FROM ${tableName(exchange, instrument, interval)} WHERE time = ${time}").use { resultSet ->
                resultSet.next()
                return mapToBar(resultSet, interval);
            }
        }
    }

    fun mapToBar(resultSet: ResultSet, interval: BarInterval): XBaseBar {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resultSet.getLong("time") * 1000), ZoneId.of("GMT"))

        return XBaseBar(interval.duration,
                time,
                resultSet.getDouble("open"),
                resultSet.getDouble("high"),
                resultSet.getDouble("low"),
                resultSet.getDouble("close"),
                resultSet.getDouble("volume"))
    }

    fun getBars(exchange: String, instrument: Instrument, interval: BarInterval, start: ZonedDateTime, end: ZonedDateTime): List<XBaseBar> {
        val bars = mutableListOf<XBaseBar>()

        conn.createStatement().use { statement ->
            statement.executeQuery("SELECT time, open, high, low, close, volume FROM ${tableName(exchange, instrument, interval)} WHERE time >=${start.toEpochSecond()} and time < ${end.toEpochSecond()} ORDER BY time").use { resultSet ->
                while (resultSet.next()) {
                    bars.add(mapToBar(resultSet, interval))
                }
            }
        }
        return bars
    }

    fun tableName(exchange: String, instrument: Instrument, interval: BarInterval): String {
        return "${exchange}_${instrument.base!!.toLowerCase()}_${instrument.asset!!.toLowerCase()}_${interval.name.toLowerCase()}"
    }

    fun createTable(exchange: String, instrument: Instrument, interval: BarInterval) {
        conn.createStatement().use {
            try {
                it.execute("create table ${tableName(exchange, instrument, interval)}(time bigint primary key, open double, high double, low double, close double, volume double)")
            } catch (e: SQLiteException) {

            }
        }
    }

    fun saveBar(exchange: String, instrument: Instrument, bar: XBar): Int {
        synchronized(this) {
            conn.createStatement().use { statement ->
                return statement.executeUpdate("INSERT OR REPLACE INTO ${tableName(exchange, instrument, BarInterval.of(bar.timePeriod))} (time, open, high, low, close, volume) VALUES (${bar.endTime.toEpochSecond()}, ${bar.openPrice}, ${bar.maxPrice}, ${bar.minPrice}, ${bar.closePrice}, ${bar.volume})")
            }
        }
    }

    fun saveBars(exchange: String, instrument: Instrument, bars: List<XBar>) {

        synchronized(this) {
            conn.autoCommit = false
            conn.createStatement().use { statement ->
                bars.forEach { bar ->
                    statement.executeUpdate("INSERT OR REPLACE INTO ${tableName(exchange, instrument, BarInterval.of(bar.timePeriod))} (time, open, high, low, close, volume) VALUES (${bar.endTime.toEpochSecond()}, ${bar.openPrice}, ${bar.maxPrice}, ${bar.minPrice}, ${bar.closePrice}, ${bar.volume})")
                }
            }
            conn.commit()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)
            val cache = BarsCache(cmd.cachePath)
            val exchange = Exchange.getExchange(cmd.exchange)

            cache.createTable(exchange.getName(), cmd.instrument, cmd.barInterval)

            println("Fetching ${cmd.instrument}/${cmd.barInterval.duration} from ${exchange.getName()} between ${cmd.start} and ${cmd.end} ")

            val bars = exchange.loadBars(cmd.instrument, cmd.barInterval, cmd.start!!, cmd.end!!)

            println("Saving ${bars.size} bars from ${bars.first().endTime} to ${bars.last().endTime}")

            cache.saveBars(exchange.getName(), cmd.instrument, bars.filter { it.timePeriod == cmd.barInterval.duration })
        }
    }

}