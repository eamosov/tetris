package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.rnd
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier


/**
 * Created by fluder on 09/02/2018.
 */
class GdmBotTrainer<P, R, M>(processors: Int = Runtime.getRuntime().availableProcessors(), steps: Array<Int> = arrayOf(10, 1)) : AbstractBotTrainer<P, R, M>(processors, steps) where M : Comparable<M>, M : BotMetrica {

    data class Direction<P>(val gene: PropertyEditor<P, Any?>, val step: Int) {
        override fun toString(): String = "Vector(gene=${gene.key}, step=$step)"
    }

    data class Vector<P>(val directions: List<Direction<P>>) {

        companion object {
            fun <P> of(gene: PropertyEditor<P, Any?>, step: Int): Vector<P> = Vector(listOf(Direction(gene, step)))

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
    override fun doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                           origin: TrainItem<P, R, M>,
                           step: Int,
                           function: (P) -> R, //тяжелая функция, считающая результат
                           metrica: (P, R) -> M, //легкая функция, считающая метрику от результата
                           copy: (P) -> P): TrainItem<P, R, M>? {


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
                    derivatives.add(Pair(Direction(it, grStep), (calc.toDouble() - origin.metrica.toDouble()) / grStep))
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
        var bestGradient: Vector<P>? = null


        for (i in 0 until 2) {
            testGradients.add(Vector.rnd(genes, 10 * step))
        }

        CompletableFuture.allOf(*testGradients.map { g ->
            CompletableFuture.supplyAsync(Supplier {

                val nextParams = copy(origin.args)
                g.step(nextParams)

                val next = TrainItem.of(nextParams, function, metrica)

                synchronized(testGradients) {
                    if (next.metrica > best.metrica) {
                        //println("new best: gradient: $g, params=$nextParams")

                        best = next
                        bestGradient = g
                    }
                }
            }, executor)
        }.toTypedArray()).join()

        if (best === origin)
            return null

        //return best
        return conjugate(bestGradient!!, best, function, metrica, copy)
    }

    fun conjugate(vectorList: Vector<P>,
                  origin: TrainItem<P, R, M>,
                  function: (P) -> R, //тяжелая функция, считающая результат
                  metrica: (P, R) -> M, //легкая функция, считающая метрику от результата
                  copy: (P) -> P): TrainItem<P, R, M> {

        var best = origin

        //Шагаем в сторону vectorList пока удается увеличивать функцию
        while (true) {
            //Посчитаем изменение параметров в этом направлении
            val params = copy(best.args)
            vectorList.directions.forEach { it.gene.step(params, it.step) }

            val next = TrainItem.of(params, function, metrica)

            if (next.metrica <= best.metrica)
                return best

            best = next
        }
    }

}
