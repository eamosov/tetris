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
class CdmBotTrainer(val processors:Int = Runtime.getRuntime().availableProcessors(), val steps: Array<Int> = arrayOf(20, 5, 1)) : BotTrainer {

    companion object {

        val cpus = Runtime.getRuntime().availableProcessors();

        private val threadPool = ThreadPoolExecutor(cpus / 7 + 1, cpus / 7 + 1,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                ThreadFactoryBuilder().setDaemon(true).build())
    }

    private val executor:ForkJoinPool = ForkJoinPool(
            processors,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true);

    data class TrainItem<P, R>(var args: P, var result: R)

    @JvmField
    var logs : Boolean = true

    override fun <P, R, M:Comparable<M>> getBestParams(genes: List<PropertyEditor<P, Any?>>,
                                      origin: List<P>,
                                      function: (P) -> R,
                                      metrica: (P, R) -> M,
                                      copy: (P) -> P,
                                      newBest: ((P, R) -> Unit)?): Pair<P, R> {

        val population: MutableList<TrainItem<P, R?>> = origin.map { TrainItem(it, null as R?) }.toMutableList()

        doCdm(genes, population, function, metrica, copy, newBest)

        population.sortBy { metrica(it.args, it.result!!) }

        if (logs){
            println("TOP RESULTS:")
            for (i in maxOf(population.size - 5, 0) until population.size) {
                val metrica1 = metrica(population[i].args, population[i].result!!)
                val trainItem = population[i]
                println("$metrica1 $trainItem")
            }
        }

        return Pair(population.last().args, population.last().result!!)
    }


    fun <P, R, M:Comparable<M>> doCdm(genes: List<PropertyEditor<P, Any?>>,
                     population: MutableList<TrainItem<P, R?>>,
                     function: (P) -> R,
                     metrica: (P, R) -> M,
                     copy: (P) -> P,
                     newBest: ((P, R) -> Unit)? = null) {

        val futures: MutableList<CompletableFuture<Unit>> = mutableListOf()
        var finished = 0
        var lastBest: M? = null

        (0 until population.size).mapTo(futures) {
            CompletableFuture.supplyAsync(Supplier {
                try {
                    population[it].result = function(population[it].args)
                    population[it] = doCdm(genes, population[it], function, metrica, copy)

                    val m = metrica(population[it].args, population[it].result!!)
                    if (lastBest == null || m > lastBest!!) {
                        lastBest = m
                        if (logs)
                            println("CDM: NEW BEST (${m}) ${population[it]}")
                        newBest?.invoke(population[it].args, population[it].result!!)
                    }
                    if (logs)
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

    fun <P, R, M:Comparable<M>> doCdm(genes: List<PropertyEditor<P, Any?>>,
                     origin: TrainItem<P, R?>,
                     function: (P) -> R, metrica: (P, R) -> M, copy: (P) -> P): TrainItem<P, R?> {

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

    fun <P, R, M:Comparable<M>> doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                         origin: TrainItem<P, R?>,
                         step: Int,
                         function: (P) -> R,
                         metrica: (P, R) -> M,
                         copy: (P) -> P): TrainItem<P, R?>? {

        val origMetrica: M = metrica(origin.args, origin.result!!)

        val futures: MutableList<CompletableFuture<Triple<PropertyEditor<P, Any?>, Int, TrainItem<P, R>>>> = mutableListOf()

        //Посчитать метрику при изменении каждой координату на шаг step
        for (gene in genes) {
            arrayOf(-step, step).mapTo(futures) {
                CompletableFuture.supplyAsync(Supplier {
                    val steppedParams = copy(origin.args)
                    gene.step(steppedParams, it)
                    return@Supplier Triple(gene, it, TrainItem(steppedParams, function(steppedParams)))
                }, executor)
            }
        }

        //Несколько случайных прыжков
        val rFutures = (0 .. 2).map { CompletableFuture.supplyAsync(Supplier {
            val steppedParams = copy(origin.args)
            for (gene in genes) {
                gene.step(steppedParams, rnd(-step * 10, step * 10))
            }
            return@Supplier TrainItem(steppedParams, function(steppedParams))
        }, executor) }

        //Найдем изменения, которые привели к росту метрики
        val goodResults = CompletableFuture.allOf(*futures.toTypedArray())
                .get()
                .let { futures.map { it.get() } }
                .filter { metrica(it.third.args, it.third.result) > origMetrica }
                .toMutableList()

        //Все результаты
        val allResults = arrayListOf<TrainItem<P, R>>()

        //Добавим всем результаты по отдельным координатам
        allResults.addAll(goodResults.map { it.third })

        //Добавим результат по всем координатам, где был рост
        val steppedParams = copy(origin.args)
        goodResults.forEach { it.first.step(steppedParams, it.second) }
        allResults.add(TrainItem(steppedParams, function(steppedParams)))

        //Добавим все результаты случайных прыжков
        allResults.addAll(CompletableFuture.allOf(*rFutures.toTypedArray()).get().let { rFutures.map { it.get() } })

        //Найдем лучший
        val best = allResults.maxWith(Comparator.comparing<TrainItem<P,R>, M> { metrica(it.args, it.result) })

        if (metrica(best!!.args, best!!.result) > origMetrica)
            return best as TrainItem<P, R?>

        return null
    }

}