package ru.efreet.trading.logic

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bars.indexOf
import ru.efreet.trading.bot.Advice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
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
                                                            val bars: MutableList<XExtBar> = mutableListOf<XExtBar>()) : BotLogic<P> {

    val properties = PropertyEditorFactory<P>(paramsCls)

    override val genes: List<PropertyEditor<P, Any?>>
        get() = properties.genes

    var _params:P = paramsCls.java.newInstance()
    private var paramsInited: Boolean = false

    override var maxBars = 3000

    override var historyBars = 3000L

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<P, R> {
        return properties.of(kprop, key, min, max, step, hardBounds)
    }

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, value: R): PropertyEditor<P, R> {
        return properties.of(kprop, key, value)
    }

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

    override fun seed(seedType: SeedType, size: Int): MutableList<P> {
        return when {
            seedType == SeedType.CELL -> seedByCell(size)
            seedType == SeedType.RANDOM -> seedRandom(size)
            else -> throw RuntimeException("Unknown seed method")
        }
    }

    protected fun seedByCell(size: Int): MutableList<P> {

        val population = mutableListOf<P>()
        seedByCell(population, size, paramsCls.java.newInstance(), 0)
        return population
    }

    open protected fun seedRandom(size: Int): MutableList<P> {
        return (0 until size).map { properties.random(_params, { copyParams(it) }) } as MutableList<P>
    }

    override fun indexOf(time: ZonedDateTime): Int {
        return bars.indexOf(time)
    }

    override fun insertBar(bar: XBar) {
        bars.add(XExtBar(bar))

        while (bars.size > maxBars) {
            bars.removeAt(0)
        }
    }

    override fun getParams(): P? {
        return try {
            _params
        } catch (e: kotlin.UninitializedPropertyAccessException) {
            null
        }
    }

    override fun setParams(params: P) {
        if (!paramsInited || params != _params) {
            if (paramsInited) {
                for (i in 0 until bars.size) {
                    bars[i] = XExtBar(bars[i].bar)
                }
            }
            _params = params
            paramsInited = true
            if (properties.isInitialized(_params))
                prepare()
        }
    }

    override fun getParamsAsProperties(): Properties {
        return properties.fromLogicParams(_params)
    }

    override fun setParamsAsProperties(params: Properties) {
        properties.toLogicParams(params)?.let { setParams(it) } ?: println("params haven't been set")
    }

    private fun getAdvice(index: Int, bar: XExtBar, stats: TradesStats?, trader: Trader, fillIndicators: Boolean = false): Advice {

        var advice = if (stats == null || isProfitable(stats)) {
            getAdvice(index, bar)
        } else {
            println("Dangerous statistic, SELL all")
            OrderSide.SELL
        }

        var amount: Double = 0.0

        val availableAsset = trader.availableAsset(instrument)

        val lastTrade = trader.lastTrade()
        if (lastTrade != null
                && lastTrade.side == OrderSide.BUY
                && bar.closePrice < (1.0 - _params.stopLoss / 100.0) * lastTrade.price
                && availableAsset > 0) {

            advice = OrderSide.SELL
            amount = availableAsset
        } else if (advice == OrderSide.BUY) {
            amount = trader.availableUsd(instrument).div(bar.closePrice)
        } else if (advice == OrderSide.SELL) {
            amount = availableAsset
        }

        return Advice(bar.endTime, advice, instrument, bar.closePrice, amount, bar, if (fillIndicators) getIndicators(index, bar) else null)
    }

    override fun getAdvice(index: Int, stats: TradesStats?, trader: Trader, fillIndicators: Boolean): Advice {
        val bar = getBar(index)
        return getAdvice(index, bar, stats, trader, fillIndicators)
    }


    override fun lastBar(): XExtBar {
        return bars.last()
    }

    override fun getBar(index: Int): XExtBar {
        return bars[index]
    }

    override fun firstBar(): XExtBar {
        return bars.first()
    }

    override fun barsCount(): Int {
        return bars.size
    }

    private fun getIndicators(index: Int, bar: XExtBar): Map<String, Double> {
        return indicators().mapValues { it.value.getValue(index, bar) }
    }

    override fun metrica(stats: TradesStats): Double {
        return BotLogic.fine(stats.trades.toDouble(), 200.0, 4.0) + /*BotLogic.fine((stats.avrProfitPerTrade - 1.0) * 100, 1.0, 5.0) +*/ /*foo(stats.goodTrades, 1.3, 5.0)*/ BotLogic.fine(stats.sma10, 1.0, 10.0) + BotLogic.fine(stats.profit, 1.0) + stats.profit
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

    override fun getMinMax(): Properties {
        return properties.getMinMax()
    }

}