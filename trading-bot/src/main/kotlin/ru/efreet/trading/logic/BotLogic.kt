package ru.efreet.trading.logic

import org.slf4j.LoggerFactory
import ru.efreet.trading.bars.MarketBar
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.trainer.Metrica
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.SeedType
import ru.efreet.trading.utils.SortedProperties
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

interface BotLogic<P> {

    val instrument: Instrument

    val genes: List<PropertyEditor<P, Any?>>

    fun setHistory(bars: List<XBar>, marketBars: List<MarketBar>?)

    fun copyParams(src: P): P

    var params: P

    fun setParams(properties: Properties)

    fun getParamsAsProperties(): Properties

    fun saveState(path: String, comment: String) {
        Files.newOutputStream(Paths.get(path), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use {
            val all = SortedProperties()
            all.putAll(getMinMax())
            all.putAll(getParamsAsProperties())
            all.store(it, comment)
        }
    }

    fun loadState(configPath: String): Boolean {
        return try {
            log.info("Loading config from $configPath")

            Files.newInputStream(Paths.get(configPath)).use {
                val p = Properties()
                p.load(it)
                setMinMax(p)
                setParams(p)
            }
            true
        } catch (e: java.nio.file.NoSuchFileException) {
            log.warn("$configPath not found")
            false
        }
    }

    fun getAdvice(nextBar: XBar, nextMarketBar: MarketBar? = null): BotAdvice

    fun metrica(params: P, stats: TradesStats): Metrica

    val historyBars: Long

    fun setMinMax(settings: Properties)

    fun setMinMax(obj: P, p: Float, hardBounds: Boolean)

    fun getMinMax(): Properties

    fun logState(): String

    companion object {

        private val log = LoggerFactory.getLogger(BotLogic::class.java)

        fun fine(x: Float, min: Float, base: Float = 2.0F): Float {
            return (-Math.pow(base.toDouble(), -(x.toDouble() - min.toDouble())) + 1.0).toFloat()
        }

        fun funXP(x: Float, p: Float): Float {
            return (Math.signum(x) * (Math.pow(Math.abs(x) + 1.0, p.toDouble()) - 1.0)).toFloat()
        }

    }

}