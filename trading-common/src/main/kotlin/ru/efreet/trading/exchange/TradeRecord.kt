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
        val price: Float? = null,

        @DatabaseField(persisterClass = EnumStringType::class)
        val decision: Decision? = null,

        @DatabaseField(persisterClass = MapPersister::class)
        val decisionArgs: Map<String, String>? = null,

        @DatabaseField(persisterClass = EnumStringType::class)
        val type: OrderType? = null,

        @DatabaseField
        val amount: Float? = null, /*of asset*/

        @DatabaseField
        val fee: Float? = null, /* feeRatio */

        @DatabaseField
        val usdBefore: Float? = null,

        @DatabaseField
        val assetBefore: Float? = null,

        @DatabaseField
        val usdAfter: Float? = null,

        @DatabaseField
        val assetAfter: Float? = null

) : Serializable {

    fun before(): Float {
        return usdBefore!! + assetBefore!! * price!!
    }

    fun after(): Float {
        return usdAfter!! + assetAfter!! * price!!
    }

    fun profit(buy:TradeRecord): Float {
        return price!! / buy.price!! * (1.0F - fee!!) * (1.0F - buy.fee!!)
    }

}