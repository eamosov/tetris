package ru.efreet.trading.logic

import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.trainer.Metrica
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.PropertyEditorFactory
import ru.efreet.trading.utils.SeedType
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf

abstract class AbstractBotLogic<P : Any>(val name: String,
                                         val paramsCls: KClass<P>,
                                         override val instrument: Instrument,
                                         val barInterval: BarInterval) : BotLogic<P> {

    val propertyEditorFactory = PropertyEditorFactory(paramsCls) { newInitParams() }

    override val genes: List<PropertyEditor<P, Any?>>
        get() = propertyEditorFactory.genes

    init {
        params = newInitParams()
    }

    abstract fun newInitParams(): P

    override val historyBars = 3000L

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<P, R> =
            propertyEditorFactory.of(kprop, key, min, max, step, hardBounds)

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<P, R>, key: String, value: R): PropertyEditor<P, R> =
            propertyEditorFactory.of(kprop, key, value)

    fun of(kprop: KMutableProperty1<P, Int?>, key: String, min: Duration, max: Duration, step: Duration, hardBounds: Boolean): PropertyEditor<P, Int?> {
        return propertyEditorFactory.of(kprop, key,
                Math.max(1, (min.toMillis() / barInterval.duration.toMillis()).toInt()),
                Math.max(2, (max.toMillis() / barInterval.duration.toMillis()).toInt()),
                Math.max(1, (step.toMillis() / barInterval.duration.toMillis()).toInt()),
                hardBounds)
    }

    override fun logState(): String {
        return propertyEditorFactory.log(params)
    }

    fun resetGenes() {
        propertyEditorFactory.consts.addAll(propertyEditorFactory.genes)
        propertyEditorFactory.genes.clear()
    }

    private fun seedByCell(population: MutableList<P>, trainingSize: Int, proto: P, propIndex: Int) {


        if (propIndex >= propertyEditorFactory.genes.size) {
            population.add(copyParams(proto))
            return
        }

        val gene = propertyEditorFactory.genes[propIndex]

        if (gene.cls.isSubclassOf(Int::class)) {
            val stepSize = (gene.getMax(proto) as Int - gene.getMin(proto) as Int) / (trainingSize + 1)
            for (s in 1..(trainingSize)) {
                gene.setValue(proto, (gene.getMin(proto) as Int) + s * stepSize)
                seedByCell(population, trainingSize, proto, propIndex + 1)
            }
        } else if (gene.cls.isSubclassOf(Float::class)) {
            val stepSize = (gene.getMax(proto) as Float - gene.getMin(proto) as Float) / (trainingSize + 1.0)
            for (s in 1..(trainingSize)) {
                gene.setValue(proto, (gene.getMin(proto) as Float) + s * stepSize)
                seedByCell(population, trainingSize, proto, propIndex + 1)
            }
        }

    }

    fun seed(seedType: SeedType, size: Int): MutableList<P> = when {
        seedType == SeedType.CELL -> seedByCell(size)
        seedType == SeedType.RANDOM -> seedRandom(size)
        else -> throw RuntimeException("Unknown seed method")
    }

    fun seedByCell(size: Int): MutableList<P> {

        val population = mutableListOf<P>()
        seedByCell(population, size, paramsCls.java.newInstance(), 0)
        return population
    }

    protected open fun seedRandom(size: Int): MutableList<P> = (0 until size).map { propertyEditorFactory.randomParams(copyParams(params)) } as MutableList<P>

    override fun getParamsAsProperties(): Properties = propertyEditorFactory.newProperties(params)

    override fun setParams(properties: Properties) {
        params = propertyEditorFactory.newParams(properties)
    }

    override fun metrica(params: P, stats: TradesStats): Metrica {

        return Metrica()
                .add("fine_trades", BotLogic.fine(stats.trades.toFloat(), 50.0F, 2.0F))
                .add("fine_sma10", BotLogic.fine(stats.sma10, 1.0F, 10.0F) * 2 + 1)
                .add("fine_profit", BotLogic.fine(stats.profit, 1.0F))
                .add("relProfit", BotLogic.funXP(stats.relProfit, 1.0F))
    }

    override fun setMinMax(settings: Properties) {
        propertyEditorFactory.setMinMax(settings)
    }

    override fun setMinMax(obj: P, p: Float, hardBounds: Boolean) {
        propertyEditorFactory.setMinMax(obj, p, hardBounds)
    }

    override fun getMinMax(): Properties = propertyEditorFactory.newMinMaxProperties()
}