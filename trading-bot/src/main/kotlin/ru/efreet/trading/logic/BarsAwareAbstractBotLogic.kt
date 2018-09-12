package ru.efreet.trading.logic

import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.indexOf
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.utils.trimToBar
import java.time.ZonedDateTime
import kotlin.reflect.KClass

abstract class BarsAwareAbstractBotLogic<P : Any, B : XBar>(name: String, paramsCls: KClass<P>, instrument: Instrument, barInterval: BarInterval) : AbstractBotLogic<P>(name, paramsCls, instrument, barInterval) {

    val bars = mutableListOf<B>()
    protected var barsIsPrepared = false

    fun lastBar(): B = bars.last()

    fun getBar(index: Int): B = bars[index]

    fun firstBar(): B = bars.first()

    fun barsCount(): Int = bars.size

    fun indexOf(time: ZonedDateTime): Int = bars.indexOf(time)

    fun getBarIndex(time: ZonedDateTime): Int {

        var startIndex = bars.binarySearchBy(time.toEpochSecond(), selector = { it.endTime.toEpochSecond() })

        if (startIndex < 0)
            startIndex = -startIndex - 1

        return startIndex
    }

    override fun setHistory(bars: List<XBar>, marketBars: List<MarketBar>?) {
        val mm = marketBars?.map { it.endTime.trimToBar() to it }?.toMap() ?: mapOf()
        bars.forEach {
            insertBar(it, mm[it.endTime.trimToBar()])
        }
        prepareBars()
    }

    abstract fun insertBar(bar: XBar, marketBar: MarketBar? = null)

    protected abstract fun getAdviceImpl(index: Int, fillIndicators: Boolean = false): BotAdvice

    fun getAdvice(index: Int, fillIndicators: Boolean): BotAdvice {
        if (!barsIsPrepared)
            prepareBars()

        return getAdviceImpl(index, fillIndicators);
    }

    override fun getAdvice(nextBar: XBar, nextMarketBar: MarketBar?): BotAdvice {
        insertBar(nextBar, nextMarketBar)
        return getAdvice(barsCount() - 1, true)
    }

    abstract fun indicators(): Map<String, XIndicator>

    protected fun getIndicators(index: Int): Map<String, Float> = indicators().mapValues { it.value.getValue(index) }

    open fun prepareBars() {
        barsIsPrepared = true
    }

    open fun resetBars() {
        barsIsPrepared = false
    }

    override var params: P = newInitParams()
        set(value) {
            val oldP = field
            field = value
            if (oldP != value) {
                if (barsIsPrepared)
                    resetBars()
            }
        }

}