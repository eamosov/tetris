package ru.efreet.trading.test;

import kotlin.Pair;
import ru.efreet.trading.trainer.CdmBotTrainer;
import ru.efreet.trading.utils.PropertyEditorFactory;

import java.util.ArrayList;
import java.util.List;

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

        PropertyEditorFactory properties = PropertyEditorFactory.of(Params.class);

        properties.of(Double.class, "x", "x", -100, 100, 0.1, false);
        properties.of(Double.class, "y", "y", -100.0, 100.0, 0.1, false);

        //Начальное множество параметров - исходных точек оптимизации
        List<Params> origin = new ArrayList<>();
        origin.add(new Params(23.0, 15.0));
        origin.add(new Params(-16.0, 12.0));

        Pair<Params, Double> best = new CdmBotTrainer().getBestParams(
            properties.getGenes(),
            origin, // исходные точки
            p -> {  // функция, которая для каждой исходной точки подсчитвает результат (любого типа)
                return 1 / (Math.abs(p.x + p.y) + 3 * Math.abs(p.y - p.x));
            },
            (p, r) -> { //функция, которая для пары (точка,результат) подсчитывает метрику, которая максимизируется
                return r * 2;
            },
            p -> {  //функция копирования точек
                return new Params(p);
            },
            (p, r) -> { //Коллбек, когда найден новый лучший кандидат(для отслеживания процесса)
                System.out.println("NEW: " + p.toString() + " " + r.toString());
                return  null;
            });

        System.out.println("best: " + best);

    }

}
