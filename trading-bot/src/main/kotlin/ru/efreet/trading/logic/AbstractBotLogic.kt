package ru.efreet.trading.logic

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bars.indexOf
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.trainer.Metrica
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.PropertyEditorFactory
import ru.efreet.trading.utils.SeedType
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf

/**
 * Created by fluder on 20/02/2018.
 */
abstract class AbstractBotLogic<P : AbstractBotLogicParams>(val name: String,
                                                            val paramsCls: KClass<P>,
                                                            override val instrument: Instrument,
                                                            val barInterval: BarInterval,
                                                            val bars: MutableList<XExtBar> = mutableListOf()) : BotLogic<P> {

    val properties = PropertyEditorFactory<P>(paramsCls)

    override val genes: List<PropertyEditor<P, Any?>>
        get() = properties.genes

    protected var _params: P = paramsCls.java.newInstance()

    private var barsIsPrepared = false

    override var historyBars = 3000L

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<P, R> =
            properties.of(kprop, key, min, max, step, hardBounds)

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, value: R): PropertyEditor<P, R> =
            properties.of(kprop, key, value)

    fun of(kprop: KMutableProperty1<P, Int?>, key: String, min: Duration, max: Duration, step: Duration, hardBounds: Boolean): PropertyEditor<P, Int?> {
        return properties.of(kprop, key,
                Math.max(1, (min.toMillis() / barInterval.duration.toMillis()).toInt()),
                Math.max(2, (max.toMillis() / barInterval.duration.toMillis()).toInt()),
                Math.max(1, (step.toMillis() / barInterval.duration.toMillis()).toInt()),
                hardBounds)
    }

    override fun logState(): String {
        return properties.log(getParams())
    }

//    override fun copyParams(orig: P): P {
//        return properties.copy(orig)
//    }

    private fun seedByCell(population: MutableList<P>, trainingSize: Int, proto: P, propIndex: Int) {


        if (propIndex >= properties.genes.size) {
            population.add(copyParams(proto))
            return
        }

        val gene = properties.genes[propIndex]

        if (gene.cls.isSubclassOf(Int::class)) {
            val stepSize = (gene.getMax(proto) as Int - gene.getMin(proto) as Int) / (trainingSize + 1)
            for (s in 1..(trainingSize)) {
                gene.setValue(proto, (gene.getMin(proto) as Int) + s * stepSize)
                seedByCell(population, trainingSize, proto, propIndex + 1)
            }
        } else if (gene.cls.isSubclassOf(Double::class)) {
            val stepSize = (gene.getMax(proto) as Double - gene.getMin(proto) as Double) / (trainingSize + 1.0)
            for (s in 1..(trainingSize)) {
                gene.setValue(proto, (gene.getMin(proto) as Double) + s * stepSize)
                seedByCell(population, trainingSize, proto, propIndex + 1)
            }
        }

    }

    override fun seed(seedType: SeedType, size: Int): MutableList<P> = when {
        seedType == SeedType.CELL -> seedByCell(size)
        seedType == SeedType.RANDOM -> seedRandom(size)
        else -> throw RuntimeException("Unknown seed method")
    }

    protected fun seedByCell(size: Int): MutableList<P> {

        val population = mutableListOf<P>()
        seedByCell(population, size, paramsCls.java.newInstance(), 0)
        return population
    }

    open protected fun seedRandom(size: Int): MutableList<P> = (0 until size).map { properties.random(_params, { copyParams(it) }) } as MutableList<P>

    override fun indexOf(time: ZonedDateTime): Int = bars.indexOf(time)

    override fun insertBar(bar: XBar) {
        synchronized(this) {
            bars.add(XExtBar(bar))

            while (bars.size > historyBars) {
                bars.removeAt(0)
            }
        }
    }

    override fun getParams(): P {
        return _params
    }

    override fun isInitialized(): Boolean = properties.isInitialized(_params)

    override fun setParams(params: P) {
        synchronized(this) {
            val oldP = _params
            _params = params
            if ((_params != oldP && isInitialized()) || (isInitialized() && !barsIsPrepared)) {
                if (barsIsPrepared) {
                    for (i in 0 until bars.size) {
                        bars[i] = XExtBar(bars[i].bar)
                    }
                }
                barsIsPrepared = true
                prepare()
            }
        }
    }

    override fun getParamsAsProperties(): Properties = properties.fromLogicParams(_params)

    override fun setParamsAsProperties(params: Properties) = setParams(properties.toLogicParams(params))

    override fun lastBar(): XExtBar = bars.last()

    override fun getBar(index: Int): XExtBar = bars[index]

    override fun firstBar(): XExtBar = bars.first()

    override fun barsCount(): Int = bars.size

    protected fun getIndicators(index: Int, bar: XExtBar): Map<String, Double> =
            indicators().mapValues { it.value.getValue(index, bar) }

    override fun metrica(params: P, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toDouble(), 50.0, 4.0))
                .add("fine_sma10", BotLogic.fine(stats.sma10, 1.0, 10.0))
                .add("fine_profit", BotLogic.fine(stats.profit, 1.0))
                .add("profit", BotLogic.fine(stats.profit, 1.0))
    }

    override fun isProfitable(stats: TradesStats): Boolean {
        //return stats.trades > 4 && stats.goodTrades > 0.6 && stats.profit > 1.0
        return stats.profit > 1.0
    }

    override fun getBarIndex(time: ZonedDateTime): Int {

        var startIndex = bars.binarySearchBy(time, selector = { it.endTime })

        if (startIndex < 0)
            startIndex = -startIndex - 1

        return startIndex
    }

    override fun setMinMax(settings: Properties) {
        properties.setMinMax(settings)
    }

    override fun setMinMax(obj: P, p: Double, hardBounds: Boolean) {
        properties.setMinMax(obj, p, hardBounds)
    }

    override fun getMinMax(): Properties = properties.getMinMax()

}