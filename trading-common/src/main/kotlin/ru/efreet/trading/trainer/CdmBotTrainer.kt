package ru.efreet.trading.trainer

import ru.efreet.trading.utils.PropertyEditor
import ru.efreet.trading.utils.rnd
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier


/**
 * Created by fluder on 09/02/2018.
 */
class CdmBotTrainer<P, R, M>(processors: Int = Runtime.getRuntime().availableProcessors(), steps: Array<Int> = arrayOf(50, 10, 5, 1)) : AbstractBotTrainer<P, R, M>(processors, steps) where M : Comparable<M>, M : BotMetrica {


    @Suppress("UNCHECKED_CAST")
    override fun doCdmStep(genes: List<PropertyEditor<P, Any?>>,
                           origin: TrainItem<P, R, M>,
                           step: Int,
                           function: (P) -> R,
                           metrica: (P, R) -> M,
                           copy: (P) -> P): TrainItem<P, R, M>? {


        val futures: MutableList<CompletableFuture<Triple<PropertyEditor<P, Any?>, Int, TrainItem<P, R, M>>>> = mutableListOf()

        //Посчитать метрику при изменении каждой координату на шаг step
        for (gene in genes) {
            arrayOf(-step, step).mapTo(futures) {
                CompletableFuture.supplyAsync(Supplier {
                    val steppedParams = copy(origin.args)
                    gene.step(steppedParams, it)
                    return@Supplier Triple(gene, it, TrainItem.of(steppedParams, function, metrica))
                }, executor)
            }
        }

        //Несколько случайных прыжков
        val rFutures = (0..2).map {
            CompletableFuture.supplyAsync(Supplier {
                val steppedParams = copy(origin.args)
                for (gene in genes) {
                    gene.step(steppedParams, rnd(-step * 10, step * 10))
                }
                return@Supplier TrainItem.of(steppedParams, function, metrica)
            }, executor)
        }

        //Найдем изменения, которые привели к росту метрики
        val goodResults = CompletableFuture.allOf(*futures.toTypedArray())
                .get()
                .let { futures.map { it.get() } }
                .filter { it.third.metrica > origin.metrica }
                .toMutableList()

        //Все результаты
        val allResults = arrayListOf<TrainItem<P, R, M>>()

        //Добавим всем результаты по отдельным координатам
        allResults.addAll(goodResults.map { it.third })

        //Добавим результат по всем координатам, где был рост
        val steppedParams = copy(origin.args)
        goodResults.forEach { it.first.step(steppedParams, it.second) }
        allResults.add(TrainItem.of(steppedParams, function, metrica))

        //Добавим все результаты случайных прыжков
        allResults.addAll(CompletableFuture.allOf(*rFutures.toTypedArray()).get().let { rFutures.map { it.get() } })

        //Найдем лучший
        val best = allResults.maxWith(Comparator.comparing<TrainItem<P, R, M>, M> { it.metrica })

        if (best!!.metrica > origin.metrica)
            return best

        return null
    }

}