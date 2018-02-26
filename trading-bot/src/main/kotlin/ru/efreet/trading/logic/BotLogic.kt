package ru.efreet.trading.logic

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.ta.indicators.XIndicator
import ru.efreet.trading.bot.Advice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.SeedType
import ru.efreet.trading.utils.SortedProperties
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
import java.util.*


interface BotLogic<P> {

    val instrument: Instrument

    val genes: List<PropertyEditor<P, Any?>>

    fun indexOf(time: ZonedDateTime): Int

    fun insertBar(bar: XBar)

    fun firstBar(): XBar

    fun lastBar(): XBar

    fun getBar(index: Int): XBar

    fun getBarIndex(time: ZonedDateTime): Int

    fun barsCount(): Int

    fun copyParams(src: P): P

    fun getParams(): P

    fun setParams(params: P)

    fun getParamsAsProperties(): Properties

    fun setParamsAsProperties(params: Properties)

    fun saveState(path: String, comment: String) {
        Files.newOutputStream(Paths.get(path), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use {
            val all = SortedProperties()
            all.putAll(getMinMax())
            all.putAll(getParamsAsProperties())
            all.store(it, comment)
        }
    }

    fun loadState(configPath: String) {
        try {
            println("Loading config from $configPath")

            Files.newInputStream(Paths.get(configPath)).use {
                val p = Properties()
                p.load(it)
                setMinMax(p)
                setParamsAsProperties(p)
            }
        } catch (e: java.nio.file.NoSuchFileException) {
            println("WARN: $configPath not found")
        }
    }

    fun prepare()

    fun getAdvice(index: Int, stats: TradesStats?, trader: Trader, fillIndicators: Boolean = false): Advice

    fun getAdvice(stats: TradesStats?, trader: Trader, fillIndicators: Boolean = false): Advice {
        return getAdvice(barsCount() - 1, stats, trader, fillIndicators)
    }

    fun metrica(stats: TradesStats): Double

    var maxBars: Int

    var historyBars: Long

    fun isProfitable(stats: TradesStats): Boolean

    fun indicators(): Map<String, XIndicator<XExtBar>>

    fun getAdvice(index: Int, bar: XExtBar): OrderSide?

    fun setMinMax(settings: Properties)

    fun setMinMax(obj: P, p: Double, hardBounds: Boolean)

    fun getMinMax(): Properties

    fun seed(seedType: SeedType, size: Int): MutableList<P>

    fun logState(): String
}