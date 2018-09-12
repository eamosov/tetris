package ru.efreet.trading.exchange.impl.cache

import org.sqlite.SQLiteException
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XBarList
import ru.efreet.trading.bars.XBaseBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.regex.Pattern

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

    fun getConnection(): Connection = conn

    fun getFirst(exchange: String, instrument: Instrument, interval: BarInterval): XBaseBar? {
        return synchronized(this) {
            conn.createStatement().use { statement ->
                val time = statement.executeQuery("select min(time) from ${tableName(exchange, instrument, interval)}").use { resultSet ->
                    resultSet.next()
                    resultSet.getLong(1)
                }

                statement.executeQuery("SELECT time, open, high, low, close, volume, volumebase, volumequote, trades FROM ${tableName(exchange, instrument, interval)} WHERE time = ${time}").use { resultSet ->

                    if (!resultSet.next())
                        return null

                    mapToBar(resultSet, interval);
                }
            }
        }
    }

    fun getLast(exchange: String, instrument: Instrument, interval: BarInterval): XBaseBar? {

        return synchronized(this) {
            conn.createStatement().use { statement ->
                val time = statement.executeQuery("select max(time) from ${tableName(exchange, instrument, interval)}").use { resultSet ->
                    resultSet.getLong(1)
                }

                statement.executeQuery("SELECT time, open, high, low, close, volume, volumebase, volumequote, trades FROM ${tableName(exchange, instrument, interval)} WHERE time = ${time}").use { resultSet ->

                    if (!resultSet.next())
                        return null

                    mapToBar(resultSet, interval);
                }
            }
        }
    }

    fun getInstruments(exchange: String, interval: BarInterval): List<Instrument> {
        val instruments = mutableListOf<Instrument>()
        val pattern = Pattern.compile("${exchange}_([a-z]+)_([a-z]+)_${interval.toString().toLowerCase()}")

        synchronized(this) {
            conn.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")

                while (resultSet.next()) {
                    val tableName = resultSet.getString(1)
                    val m = pattern.matcher(tableName)
                    if (m.matches()) {
                        val base = m.group(1).toUpperCase()
                        val asset = m.group(2).toUpperCase()
                        instruments.add(Instrument(asset, base))
                    }
                }
                resultSet.close()
            }
        }

        return instruments
    }

    fun mapToBar(resultSet: ResultSet, interval: BarInterval): XBaseBar {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(resultSet.getLong("time") * 1000), ZoneId.of("GMT")).withSecond(59)

        return XBaseBar(interval.duration,
                time,
                resultSet.getFloat("open"),
                resultSet.getFloat("high"),
                resultSet.getFloat("low"),
                resultSet.getFloat("close"),
                resultSet.getFloat("volume"),
                resultSet.getFloat("volumebase"),
                resultSet.getFloat("volumequote"),
                resultSet.getShort("trades")
        )
    }

    fun getBars(exchange: String, instrument: Instrument, interval: BarInterval, start: ZonedDateTime, end: ZonedDateTime): XBarList {
        synchronized(this) {
            val bars = XBarList((Duration.between(start, end).toMinutes() / interval.duration.toMinutes()).toInt())
            conn.createStatement().use { statement ->
                statement.executeQuery("SELECT time, open, high, low, close, volume, volumebase, volumequote, trades FROM ${tableName(exchange, instrument, interval)} WHERE time >=${start.toEpochSecond()} and time < ${end.toEpochSecond()} ORDER BY time").use { resultSet ->
                    while (resultSet.next()) {
                        bars.add(mapToBar(resultSet, interval))
                    }
                }
            }
            return bars
        }
    }

    fun getBar(exchange: String, instrument: Instrument, interval: BarInterval, time: ZonedDateTime): XBar? {
        synchronized(this) {
            conn.createStatement().use { statement ->
                statement.executeQuery("SELECT time, open, high, low, close, volume, volumebase, volumequote, trades FROM ${tableName(exchange, instrument, interval)} WHERE time =${time.toEpochSecond()}").use { resultSet ->
                    while (resultSet.next()) {
                        return mapToBar(resultSet, interval)
                    }
                }
            }
        }
        return null
    }

    fun tableName(exchange: String, instrument: Instrument, interval: BarInterval): String {
        return "${exchange}_${instrument.base.toLowerCase()}_${instrument.asset.toLowerCase()}_${interval.name.toLowerCase()}"
    }

    fun createTable(exchange: String, instrument: Instrument, interval: BarInterval) {
        synchronized(this) {
            conn.autoCommit = true
            conn.createStatement().use {
                try {
                    it.execute("create table ${tableName(exchange, instrument, interval)}(time bigint primary key, open double, high double, low double, close double, volume double, volumebase double, volumequote double, trades int)")
                } catch (e: SQLiteException) {

                }
            }
        }
    }

    fun saveBar(exchange: String, instrument: Instrument, bar: XBar): Int {
        return synchronized(this) {
            conn.autoCommit = true
            val ret = conn.createStatement().use { statement ->
                statement.executeUpdate("INSERT OR REPLACE INTO ${tableName(exchange, instrument, BarInterval.of(bar.timePeriod))} (time, open, high, low, close, volume, volumebase, volumequote, trades) VALUES (${bar.endTime.withSecond(59).toEpochSecond()}, ${bar.openPrice}, ${bar.maxPrice}, ${bar.minPrice}, ${bar.closePrice}, ${bar.volume}, ${bar.volumeBase}, ${bar.volumeQuote}, ${bar.trades})")
            }
            ret
        }
    }

    fun saveBars(exchange: String, instrument: Instrument, bars: List<XBar>) {

        synchronized(this) {
            conn.autoCommit = false
            conn.createStatement().use { statement ->
                bars.forEach { bar ->
                    statement.executeUpdate("INSERT OR REPLACE INTO ${tableName(exchange, instrument, BarInterval.of(bar.timePeriod))} (time, open, high, low, close, volume, volumebase, volumequote, trades) VALUES (${bar.endTime.withSecond(59).toEpochSecond()}, ${bar.openPrice}, ${bar.maxPrice}, ${bar.minPrice}, ${bar.closePrice}, ${bar.volume}, ${bar.volumeBase}, ${bar.volumeQuote}, ${bar.trades})")
                }
            }
            conn.commit()
            conn.autoCommit = true
        }
    }

}