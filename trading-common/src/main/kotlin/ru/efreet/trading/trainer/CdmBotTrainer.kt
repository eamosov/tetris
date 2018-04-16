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
class CdmBotTrainer(val processors:Int) : BotTrainer {

    companion object {

        private val threadPool = ThreadPoolExecutor(4, 4,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                ThreadFactoryBuilder().setDaemon(true).build())
    }

    private val executor:ForkJoinPool = ForkJoinPool(
            processors,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true);

    constructor(): this (Runtime.getRuntime().availableProcessors())


    data class TrainItem<P, R>(var args: P, var result: R)

    override fun <P, R> getBestParams(genes: List<PropertyEditor<P, Any?>>,
                                      origin: List<P>,
                                      function: (P) -> R,
                                      metrica: (P, R) -> Double,
                                      copy: (P) -> P,
                                      newBest: ((P, R) -> Unit)?): Pair<P, R> {

        val population: MutableList<TrainItem<P, R?>> = origin.map { TrainItem(it, null as R?) }.toMutableList()

        doCdm(genes, population, function, metrica, copy, newBest)

        population.sortBy { metrica(it.args, it.result!!) }

        println("TOP RESULTS:")

        for (i in maxOf(population.size - 5, 0) until population.size) {
            println("${metrica(population[i].args, population[i].result!!).round2()} ${population[i]}")
        }

        return Pair(population.last().args, population.last().result!!)
    }


    fun <P, R> doCdm(genes: List<PropertyEditor<P, Any?>>,
                     population: MutableList<TrainItem<P, R?>>,
                     function: (P) -> R,
                     metrica: (P, R) -> Double,
                     copy: (P) -> P,
                     newBest: ((P, R) -> Unit)? = null) {

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
                        println("CDM: NEW BEST (${m.round2()}) ${population[it]}")
                        newBest?.invoke(population[it].args, population[it].result!!)
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
    val steps: Array<Int> = arrayOf(20, 5, 1)

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

}