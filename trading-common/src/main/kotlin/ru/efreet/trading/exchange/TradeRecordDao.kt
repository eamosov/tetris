package ru.efreet.trading.exchange

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.db.DatabaseType
import com.j256.ormlite.db.SqliteDatabaseType
import com.j256.ormlite.jdbc.JdbcDatabaseConnection
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import com.j256.ormlite.table.DatabaseTableConfig
import com.j256.ormlite.table.TableUtils
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

/**
 * Created by fluder on 20/04/2018.
 */
class TradeRecordDao(jdbcConnection: Connection, val tableName:String = "trades") {

    private val log = LoggerFactory.getLogger(TradeRecordDao::class.java)

    private val dao: Dao<TradeRecord, String>

    private val jc = JdbcDatabaseConnection(jdbcConnection)

    init {


        val jdbcConn = object : ConnectionSource {
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

        val tableConfig = DatabaseTableConfig<TradeRecord>()
        tableConfig.dataClass = TradeRecord::class.java
        tableConfig.tableName = tableName

        dao = DaoManager.createDao(jdbcConn, tableConfig)

        try {
            dao.queryForAll()
        } catch (e: SQLException) {
            val bk = tableName + "_backup_" + System.currentTimeMillis() / 1000
            log.info("Recreating trades table, backup old as $bk")

            try {
                jc.update("ALTER TABLE $tableName RENAME TO $bk", arrayOf(), arrayOf())
            } catch (e: SQLException) {
            }
            TableUtils.dropTable(dao, true)
            TableUtils.createTableIfNotExists(jdbcConn, TradeRecord::class.java)
        }
    }

    fun create(tradeRecord: TradeRecord) {
        dao.create(tradeRecord)
    }

    fun create(tradeRecords: List<TradeRecord>) {
        tradeRecords.forEach { dao.create(it) }
    }

    fun update(tradeRecord: TradeRecord) {
        dao.update(tradeRecord)
    }

    fun clear(){
        jc.update("DELETE FROM $tableName", arrayOf(), arrayOf())
    }

    fun lastTrade(instrument: Instrument, exchangeName: String): TradeRecord? {
        var qb = dao.queryBuilder()
        qb.setWhere(qb.where().eq("instrument", instrument.toString()).and().eq("exchange", exchangeName))
        qb.limit(1)
        qb.orderBy("time", false)
        return qb.iterator().first()
    }

}