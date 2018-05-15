package ru.efreet.trading.test;

import kotlin.Pair;
import ru.efreet.trading.trainer.GdmBotTrainer;
import ru.efreet.trading.trainer.DoubleBotMetrica;
import ru.efreet.trading.trainer.TrainItem;
import ru.efreet.trading.utils.PropertyEditorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fluder on 15/04/2018.
 */
public class CdmTest {

    /**
     * Параметры, которые надо оптиимизировать
     */
    public static class Params {
        public double x;
        public double y;

        public Params() {
        }

        public Params(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Params(Params other) {
            this.x = other.x;
            this.y = other.y;
        }

        @Override
        public String toString() {
            return "Params{" +
                "x=" + x +
                ", y=" + y +
                '}';
        }
    }

    public static void main(String[] args) {

        PropertyEditorFactory<Params> properties = PropertyEditorFactory.of(Params.class, Params::new);

        properties.of(Double.class, "x", "x", -100.0, 100.0, 0.01, false);
        properties.of(Double.class, "y", "y", -100.0, 100.0, 0.01, false);

        //Начальное множество параметров - исходных точек оптимизации
        List<Params> origin = new ArrayList<>();
        //origin.add(new Params(23.0, 15.0));
        origin.add(new Params(18.0, 17.5));

        final AtomicInteger comp = new AtomicInteger(0);

        List<TrainItem<Params, Double, DoubleBotMetrica>> bests = new GdmBotTrainer<Params, Double, DoubleBotMetrica>(1, new Integer[]{100,10,1}).getBestParams(
            properties.getGenes(),
            origin, // исходные точки
            p -> {  // функция, которая для каждой исходной точки подсчитвает результат (любого типа)
                comp.incrementAndGet();
                return 1 / (Math.abs(p.x + p.y - 10) + 3 * Math.abs(p.y - p.x));
            },
            (p, r) -> { //функция, которая для пары (точка,результат) подсчитывает метрику, которая максимизируется
                return new DoubleBotMetrica(r * 2);
            },
            p -> {  //функция копирования точек
                return new Params(p);
            },
            (trainItem) -> { //Коллбек, когда найден новый лучший кандидат(для отслеживания процесса)
                System.out.println("NEW: " + trainItem.toString());
                return  null;
            });

        TrainItem<Params, Double, DoubleBotMetrica> best = bests.get(bests.size()-1);

        System.out.println("best: " + best);
        System.out.println("comp: " + comp.get());

    }

}
