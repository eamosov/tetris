package ru.efreet.trading.trainer

import com.google.common.util.concurrent.ThreadFactoryBuilder
import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.round2
import java.util.concurrent.*
import java.util.function.Supplier

/**
 * Created by fluder on 09/02/2018.
 */
abstract class AbstractBotTrainer<P, R, M>(val processors: Int = Runtime.getRuntime().availableProcessors(), val steps: Array<Int> = arrayOf(10, 1)) : BotTrainer<P, R, M> where M : Comparable<M>, M : BotMetrica {

    companion object {

        val cpus = Runtime.getRuntime().availableProcessors();

        private val threadPool = ThreadPoolExecutor(cpus / 7 + 1, cpus / 7 + 1,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(),
                ThreadFactoryBuilder().setDaemon(true).build())
    }

    protected val executor: ForkJoinPool = ForkJoinPool(
            processors,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true);

    @JvmField
    var logs: Boolean = true


    override fun getBestParams(genes: List<PropertyEditor<P, Any?>>,
                               population: List<P>,
                               function: (P) -> R,
                               metrica: (P, R) -> M,
                               copy: (P) -> P,
                               newBest: ((TrainItem<P, R, M>) -> Unit)?): List<TrainItem<P, R, M>> {

        val result = doCdm(genes, population, function, metrica, copy, newBest).sortedBy { it.metrica }

        if (logs) {
            println("TOP RESULTS:")
            for (i in maxOf(result.size - 5, 0) until result.size) {
                println("${result[i].metrica} ${result[i].args} ${result[i].result}")
            }
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun doCdm(genes: List<PropertyEditor<P, Any?>>,
              population: List<P>,
              function: (P) -> R,
              metrica: (P, R) -> M,
              copy: (P) -> P,
              newBest: ((TrainItem<P, R, M>) -> Unit)? = null): List<TrainItem<P, R, M>> {

        val futures: MutableList<CompletableFuture<Unit>> = mutableListOf()
        var finished = 0
        var best: TrainItem<P, R, M>? = null

        val out = arrayOfNulls<TrainItem<P, R, M>>(population.size)

        (0 until population.size).mapTo(futures) {
            CompletableFuture.supplyAsync(Supplier {
                try {
                    out[it] = doCdm(genes, TrainItem.of(population[it], function, metrica), function, metrica, copy)

                    if (best == null || out[it]!!.metrica > best!!.metrica) {
                        best = out[it]
                        if (logs)
                            println("CDM: NEW BEST (${best!!.metrica}) args:${best!!.args} result:${best!!.result}")
                        newBest?.invoke(best!!)
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

        return out.asList() as List<TrainItem<P, R, M>>
    }

    fun doCdm(genes: List<PropertyEditor<P, Any?>>,
              origin: TrainItem<P, R, M>,
              function: (P) -> R, metrica: (P, R) -> M, copy: (P) -> P): TrainItem<P, R, M> {

        var best = origin

        var i = 0
        while (true) {
            val next = doCdmStep(genes, best, steps[i], function, metrica, copy)
            if (next == null) {
                if (i < steps.size - 1) {
                    i++
                } else {
                    return best
                }
            } else {
                best = next
                i = maxOf(0, i - 1)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    abstract fun doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                  origin: TrainItem<P, R, M>,
                  step: Int,
                  function: (P) -> R, //тяжелая функция, считающая результат
                  metrica: (P, R) -> M, //легкая функция, считающая метрику от результата
                  copy: (P) -> P): TrainItem<P, R, M>?

}