package ru.efreet.trading.bars

import java.time.ZonedDateTime

fun List<Float>.gtZero(): Float = this.count { it > 0 }.toFloat() / this.size.toFloat()

data class MarketBar(val endTime: ZonedDateTime,
                     val delta5m: MutableList<Float> = arrayListOf(),
                     val delta15m: MutableList<Float> = arrayListOf(),
                     val delta1h: MutableList<Float> = arrayListOf(),
                     val delta1d: MutableList<Float> = arrayListOf(),
                     val delta7d: MutableList<Float> = arrayListOf()) {

    fun addBar(b: XBar) {
        if (b.delta5m != 0f)
            delta5m.add(b.delta5m / b.closePrice)

        if (b.delta15m != 0f)
            delta15m.add(b.delta15m / b.closePrice)

        if (b.delta1h != 0f)
            delta1h.add(b.delta1h / b.closePrice)

        if (b.delta1d != 0f)
            delta1d.add(b.delta1d / b.closePrice)

        if (b.delta7d != 0f)
            delta7d.add(b.delta7d / b.closePrice)
    }

    fun p5m(): Float = delta5m.gtZero()
    fun p15m(): Float = delta15m.gtZero()
    fun p1h(): Float = delta1h.gtZero()
    fun p1d(): Float = delta1d.gtZero()
    fun p7d(): Float = delta7d.gtZero()
}
