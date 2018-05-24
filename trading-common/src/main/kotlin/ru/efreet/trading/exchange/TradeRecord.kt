package ru.efreet.trading.exchange

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.types.EnumStringType
import com.j256.ormlite.table.DatabaseTable
import ru.efreet.trading.Decision
import ru.efreet.trading.utils.DateTimePersister
import ru.efreet.trading.utils.MapPersister
import java.io.Serializable
import java.time.ZonedDateTime

/**
 * Created by fluder on 25/02/2018.
 */
@DatabaseTable
data class TradeRecord(

        @DatabaseField(id = true)
        val orderId: String? = null,

        @DatabaseField(persisterClass = DateTimePersister::class, index = true)
        val time: ZonedDateTime? = null,

        @DatabaseField
        val exchange: String? = null,

        @DatabaseField
        val instrument: String? = null,

        @DatabaseField
        val price: Double? = null,

        @DatabaseField(persisterClass = EnumStringType::class)
        val decision: Decision? = null,

        @DatabaseField(persisterClass = MapPersister::class)
        val decisionArgs: Map<String, String>? = null,

        @DatabaseField(persisterClass = EnumStringType::class)
        val type: OrderType? = null,

        @DatabaseField
        val amount: Double? = null, /*of asset*/

        @DatabaseField
        val fee: Double? = null, /*of asset*/

        @DatabaseField
        val usdBefore: Double? = null,

        @DatabaseField
        val assetBefore: Double? = null,

        @DatabaseField
        val usdAfter: Double? = null,

        @DatabaseField
        val assetAfter: Double? = null

) : Serializable {

    fun before(): Double {
        return usdBefore!! + assetBefore!! * price!!
    }

    fun after(): Double {
        return usdAfter!! + assetAfter!! * price!!
    }

}