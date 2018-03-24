package ru.efreet.trading.trainer

import com.google.common.util.concurrent.ThreadFactoryBuilder
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.rnd
import ru.efreet.trading.utils.round2
import java.util.concurrent.*
import java.util.function.Supplier


/**
 * Created by fluder on 09/02/2018.
 */
class CdmBotTrainer : BotTrainer {

    companion object {
//        private val executor = ForkJoinPool(
//                Runtime.getRuntime().availableProcessors() * 85 / 100,
//                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
//                null, true)

        private val executor = ForkJoinPool.commonPool()

        private val threadPool = ThreadPoolExecutor(4, 4,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                ThreadFactoryBuilder().setDaemon(true).build())
    }

    data class TrainItem<P, R>(var args: P, var result: R)

    override fun <P, R> getBestParams(genes: List<PropertyEditor<P, Any?>>, origin: List<P>, function: (P) -> R, metrica: (P, R) -> Double, copy: (P) -> P): Pair<P, R> {

        val population: MutableList<TrainItem<P, R?>> = origin.map { TrainItem(it, null as R?) }.toMutableList()

        doCdm(genes, population, function, metrica, copy)

        population.sortBy { metrica(it.args, it.result!!) }

        println("TOP RESULTS:")

        for (i in maxOf(population.size - 5, 0) until population.size) {
            println(population[i])
        }

        return Pair(population.last().args, population.last().result!!)
    }


    fun <P, R> doCdm(genes: List<PropertyEditor<P, Any?>>, population: MutableList<TrainItem<P, R?>>,
                     function: (P) -> R, metrica: (P, R) -> Double, copy: (P) -> P) {

        val futures: MutableList<CompletableFuture<Unit>> = mutableListOf()
        var finished = 0
        var lastBest: Double? = null

        (0 until population.size).mapTo(futures) {
            CompletableFuture.supplyAsync(Supplier {
                try {
                    population[it].result = function(population[it].args)
                    population[it] = doCdm(genes, population[it], function, metrica, copy)

                    val m = metrica(population[it].args, population[it].result!!)
                    if (lastBest == null || m > lastBest!!) {
                        lastBest = m
                        println("CDM: NEW BEST ${population[it]}")
                    }

                    print("CDM: (${(finished * 100.0 / population.size).round2()} %): $finished\r")

                } catch (e: Exception) {
                    println("WARNING: Exception ${e.message} for ${population[it]}")
                    e.printStackTrace()
                }
                finished++
                Unit
            }, threadPool)
        }

        val all = CompletableFuture.allOf(*(futures.toTypedArray() as Array<CompletableFuture<*>>))
        all.get()
    }

    //val steps: Array<Double> = arrayOf(0.2, 0.1, 0.05, 0.02, 0.01, 0.001)
    val steps: Array<Int> = arrayOf(20,5,1)

    fun <P, R> doCdm(genes: List<PropertyEditor<P, Any?>>,
                     origin: TrainItem<P, R?>,
                     function: (P) -> R, metrica: (P, R) -> Double, copy: (P) -> P): TrainItem<P, R?> {

        var c = origin

        var i = 0
        while (true) {
            val n = doCdmStep(genes, c, steps[i], function, metrica, copy)
            if (n == null) {
                if (i < steps.size - 1) {
                    i++
                } else {
                    return c
                }
            } else {
                c = n
                i = maxOf(0, i - 1)
            }
        }
    }

    fun <P, R> doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                         origin: TrainItem<P, R?>,
                         step: Int,
                         function: (P) -> R,
                         metrica: (P, R) -> Double,
                         copy: (P) -> P): TrainItem<P, R?>? {

        val origMetrica: Double = metrica(origin.args, origin.result!!)

        val futures: MutableList<CompletableFuture<TrainItem<P, R>>> = mutableListOf()


        for (gene in genes) {

            for (step in arrayOf(-step, step/*, rnd(-10, 10)*/)) {

                val f = CompletableFuture.supplyAsync(Supplier {
                    val copyParams = copy(origin.args)
                    gene.step(copyParams, step)
                    return@Supplier TrainItem(copyParams, function(copyParams))
                }, executor)

                futures.add(f)
            }
        }

        for (i in 0 until 2) {
            val f = CompletableFuture.supplyAsync(Supplier {
                val copyParams = copy(origin.args)
                for (gene in genes) {
                    //gene.step(copyParams, rnd(-100, 100))
                    gene.step(copyParams, rnd(-step * 10, step * 10))
                }
                return@Supplier TrainItem(copyParams, function(copyParams))
            }, executor)

            futures.add(f)
        }


        val allF = CompletableFuture.allOf(*futures.toTypedArray())
        allF.get()

        val best = futures.map { it.get() }.maxWith(Comparator.comparingDouble { metrica(it.args, it.result) })
        if (metrica(best!!.args, best!!.result) > origMetrica)
            return best as TrainItem<P, R?>

        return null
    }

//    override fun getBestParams(logic: BotLogic<P>,
//                               feeP: Double,
//                               genes: MutableMap<KMutableProperty1<Any, Any?>, BotLogicParamsDescriptor<Any, Any, Any>>,
//                               startTime: ZonedDateTime, bars: List<XBar>, origin: List<P>): TrainerItem<P> {
//
//        val population: MutableList<TrainerItem<P>> = origin.map { TrainerItem(it) }.toMutableList()
//
//        val strategies = doCdm(logic, feeP, genes, bars, startTime, population)
//
//        val best = strategies.maxWith(Comparator.comparingDouble { it.params.metrica(it.stats!!) })!!
//
//        println("Best strategy: $best")
//        return best
//    }
//
//
//    fun doCdm(logic: BotLogic<P>,
//              feeP: Double,
//              genes: MutableMap<KMutableProperty1<Any, Any?>, BotLogicParamsDescriptor<Any, Any, Any>>,
//              bars: List<XBar>, start: ZonedDateTime, population: MutableList<TrainerItem<P>>): List<TrainerItem<P>> {
//
//        val futures: MutableList<CompletableFuture<Unit>> = mutableListOf()
//
//        (0 until population.size).mapTo(futures) {
//            CompletableFuture.supplyAsync(Supplier {
//                try {
//                    population[it].stats = logic.calcProfit(population[it].params, bars, start, feeP).stats
//                    population[it] = doCdm(logic, feeP, genes, population[it], bars, start)
//                } catch (e: Exception) {
//                    println("WARNING: Exception ${e.message} for ${population[it]}")
//                    e.printStackTrace()
//                }
//                println("Finish cdm:  ${population[it]}")
//            }, threadPool)
//        }
//
//        val all = CompletableFuture.allOf(*(futures.toTypedArray() as Array<CompletableFuture<*>>))
//        all.get()
//
//        return population
//    }
//
//    fun doCdm(logic: BotLogic<P>,
//              feeP: Double,
//              genes: MutableMap<KMutableProperty1<Any, Any?>, BotLogicParamsDescriptor<Any, Any, Any>>,
//              params: TrainerItem<P>, bars: List<XBar>, start: ZonedDateTime): TrainerItem<P> {
//
//        var c = params
//
//        var step = 1 + 2 * 5
//
//        while (true) {
//            val n = doCdmStep(logic, feeP, genes, c, bars, start, step)
//            if (n == null) {
//                if (step > 1)
//                    step -= 5
//                else
//                    return c
//            } else {
//                c = n
//            }
//        }
//    }
//
//    fun doCdmStep(logic: BotLogic<P>,
//                  feeP: Double,
//                  genes: MutableMap<KMutableProperty1<Any, Any?>, BotLogicParamsDescriptor<Any, Any, Any>>,
//                  params: TrainerItem<P>,
//                  bars: List<XBar>, start: ZonedDateTime, step: Int): TrainerItem<P>? {
//
//        val origMetrica: Double = params.params.metrica(params.stats!!)
//
//        val futures: MutableList<CompletableFuture<TrainerItem<P>>> = mutableListOf()
//
//
//        for ((prop, descriptor) in genes) {
//
//            for (step in arrayOf(-step, step/*, rnd(-10, 10)*/)) {
//
//                val f = CompletableFuture.supplyAsync(Supplier {
//                    val copyParams = params.params.copy()
//                    BotLogicParamsDescriptor.step(prop, descriptor, step, copyParams)
//
//                    val trades = logic.calcProfit(copyParams, bars, start, feeP)
//                    return@Supplier TrainerItem(copyParams, trades.stats)
//                }, executor)
//
//                futures.add(f)
//            }
//        }
//
//        for (i in 0 until 2) {
//            val f = CompletableFuture.supplyAsync(Supplier {
//
//                val copyParams = params.params.copy()
//
//                for ((prop, descriptor) in genes) {
//                    BotLogicParamsDescriptor.step(prop, descriptor, rnd(-100, 100), copyParams)
//                }
//
//                val trades = logic.calcProfit(copyParams, bars, start, feeP)
//                return@Supplier TrainerItem(copyParams, trades.stats)
//            }, executor)
//
//            futures.add(f)
//        }
//
//
//        val allF = CompletableFuture.allOf(*futures.toTypedArray())
//        allF.get()
//
//        val best = futures.map { it.get() }.maxWith(Comparator.comparingDouble { it.params.metrica(it.stats!!) })
//        if (best!!.params.metrica(best.stats!!) > origMetrica)
//            return best
//
//        return null
//    }

}