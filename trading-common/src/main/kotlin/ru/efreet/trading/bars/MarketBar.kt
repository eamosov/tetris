package ru.efreet.trading.bars

import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.floats.FloatList
import java.time.ZonedDateTime

fun FloatList.gtZero(): Float {
    var k = 0

    for (i in 0 until this.size) {
        if (this.getFloat(i) > 0) {
            k++
        }
    }

    return k.toFloat() / this.size
}

data class MarketBar(val endTime: ZonedDateTime,
                     val delta5m: FloatList = FloatArrayList(),
                     val delta15m: FloatList = FloatArrayList(),
                     val delta1h: FloatList = FloatArrayList(),
                     val delta1d: FloatList = FloatArrayList(),
                     val delta7d: FloatList = FloatArrayList()) {

    private var count = 0

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

        count ++
    }

    fun p5m(): Float = delta5m.gtZero()
    fun p15m(): Float = delta15m.gtZero()
    fun p1h(): Float = delta1h.gtZero()
    fun p1d(): Float = delta1d.gtZero()
    fun p7d(): Float = delta7d.gtZero()
    fun max5m(): Float = delta5m.max()?:0.0f
    fun max15m(): Float = delta15m.max()?:0.0f
    fun max1h(): Float = delta1h.max()?:0.0f
    fun max1d(): Float = delta1d.max()?:0.0f
    fun max7d(): Float = delta7d.max()?:0.0f
    fun min5m(): Float = delta5m.min()?:0.0f
    fun min15m(): Float = delta15m.min()?:0.0f
    fun min1h(): Float = delta1h.min()?:0.0f
    fun min1d(): Float = delta1d.min()?:0.0f
    fun min7d(): Float = delta7d.min()?:0.0f

    override fun toString(): String {
        return "MarketBar(endTime=$endTime, count=${count}, 5m=${p5m()}, 15m=${p15m()}, 1h=${p1h()}, 1d=${p1d()}, 7d=${p7d()})"
    }
}
