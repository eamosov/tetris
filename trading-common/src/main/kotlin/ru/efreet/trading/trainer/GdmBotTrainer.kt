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
class GdmBotTrainer<P, R, M>(val processors: Int = Runtime.getRuntime().availableProcessors(), val steps: Array<Int> = arrayOf(1, 5, 20)) : BotTrainer<P, R, M> where M : Comparable<M>, M : BotMetrica {

    companion object {

        val cpus = Runtime.getRuntime().availableProcessors();

        private val threadPool = ThreadPoolExecutor(cpus / 7 + 1, cpus / 7 + 1,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                ThreadFactoryBuilder().setDaemon(true).build())
    }

    private val executor: ForkJoinPool = ForkJoinPool(
            processors,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true);

    data class TrainItem<P, R>(var args: P, var result: R)

    @JvmField
    var logs: Boolean = true

    override fun getBestParams(genes: List<PropertyEditor<P, Any?>>,
                               origin: List<P>,
                               function: (P) -> R,
                               metrica: (P, R) -> M,
                               copy: (P) -> P,
                               newBest: ((P, R) -> Unit)?): Pair<P, R> {

        val population: MutableList<TrainItem<P, R?>> = origin.map { TrainItem(it, null as R?) }.toMutableList()

        doCdm(genes, population, function, metrica, copy, newBest)

        population.sortBy { metrica(it.args, it.result!!) }

        if (logs) {
            println("TOP RESULTS:")
            for (i in maxOf(population.size - 5, 0) until population.size) {
                val metrica1 = metrica(population[i].args, population[i].result!!)
                val trainItem = population[i]
                println("$metrica1 $trainItem")
            }
        }

        return Pair(population.last().args, population.last().result!!)
    }

    @Suppress("UNCHECKED_CAST")
    fun doCdm(genes: List<PropertyEditor<P, Any?>>,
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

    fun doCdm(genes: List<PropertyEditor<P, Any?>>,
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

    data class Direction<P>(val gene: PropertyEditor<P, Any?>, val step: Int) {
        override fun toString(): String {
            return "Vector(gene=${gene.key}, step=$step)"
        }
    }

    data class Vector<P>(val directions: List<Direction<P>>) {

        companion object {
            fun <P> of(gene: PropertyEditor<P, Any?>, step: Int): Vector<P> {
                return Vector(listOf(Direction(gene, step)))
            }

            //случайный вектор
            fun <P> rnd(genes: List<PropertyEditor<P, Any?>>, step: Int): Vector<P> {
                val vectors = mutableListOf<Direction<P>>()

                for (gene in genes) {
                    val rStep = rnd(-step, step)
                    vectors.add(Direction(gene, rStep))
                }

                return Vector(vectors)
            }
        }

        fun step(obj: P) {
            directions.forEach { it.gene.step(obj, it.step) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                  origin: TrainItem<P, R?>,
                  step: Int,
                  function: (P) -> R, //тяжелая функция, считающая результат
                  metrica: (P, R) -> M, //легкая функция, считающая метрику от результата
                  copy: (P) -> P): TrainItem<P, R?>? {

        //println("Check step ${step}")

        val origMetrica: M = metrica(origin.args, origin.result!!)

        //поиск градиента
        val derivatives = mutableListOf<Pair<Direction<P>, Double>>()

        CompletableFuture.allOf(*genes.map() {
            CompletableFuture.supplyAsync(Supplier {

                //сделать шаг параметра
                val steppedParams = copy(origin.args)
                val grStep = 1
                it.step(steppedParams, grStep)
                val calc = metrica(steppedParams, function(steppedParams))
                synchronized(derivatives) {
                    derivatives.add(Pair(Direction(it, grStep), (calc.toDouble() - origMetrica.toDouble()) / grStep))
                }
            }, executor)
        }.toTypedArray()).join()

        //println("originParams: ${origin.args} Derivatives: $derivatives")

        val testGradients = mutableListOf<Vector<P>>()

        //Размер шага
        val maxd = derivatives.filter { it.second != 0.0 }.maxBy { Math.abs(it.second) }
        if (maxd != null) {
            val stepSize = Math.abs(step / maxd.second)
            //println("maxd=$maxd, stepSize=$stepSize")
            val gradient = Vector(derivatives.map { Direction<P>(it.first.gene, Math.round(stepSize * it.second).toInt()) })
            testGradients.add(gradient)
            //println("gradient: $gradient")
        }

        var best = origin
        var bestMetrica = origMetrica
        var bestGradient: Vector<P>? = null


        for (i in 0 until 2) {
            testGradients.add(Vector.rnd(genes, 10 * step))
        }

        CompletableFuture.allOf(*testGradients.map { g ->
            CompletableFuture.supplyAsync(Supplier {

                val nextParams = copy(origin.args)
                g.step(nextParams)

                val nextResult = function(nextParams)
                val nextMetrica = metrica(nextParams, nextResult)

                synchronized(testGradients) {
                    if (nextMetrica > bestMetrica) {
                        //println("new best: gradient: $g, params=$nextParams")

                        best = TrainItem(nextParams, nextResult)
                        bestMetrica = nextMetrica
                        bestGradient = g
                    }
                }
            }, executor)
        }.toTypedArray()).join()

        if (best === origin)
            return null

        //return best
        return conjugate(bestGradient!!, best as TrainItem<P, R>, function, metrica, copy)
    }

    fun conjugate(vectorList: Vector<P>,
                  origin: TrainItem<P, R>,
                  function: (P) -> R, //тяжелая функция, считающая результат
                  metrica: (P, R) -> M, //легкая функция, считающая метрику от результата
                  copy: (P) -> P): TrainItem<P, R?> {

        var best = origin
        var bestMetrica = metrica(best.args, best.result)

        //Шагаем в сторону vectorList пока удается увеличивать функцию
        while (true) {
            //Посчитаем изменение параметров в этом направлении
            val params = copy(best.args)
            vectorList.directions.forEach { it.gene.step(params, it.step) }

            val next = TrainItem(params, function(params))
            val nextMetrica = metrica(next.args, next.result)

            if (nextMetrica <= bestMetrica)
                return best as TrainItem<P, R?>

            best = next
            bestMetrica = nextMetrica

            //println("conjugate params:${best!!.args} bestMetrica:${bestMetrica}")
        }
    }

}