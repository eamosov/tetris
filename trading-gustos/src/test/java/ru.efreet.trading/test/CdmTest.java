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
        public int x;
        public double y;

        public Params(int x, double y) {
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

        properties.of(Integer.class, "x", "x", 0, 1000, 1, false);
        properties.of(Double.class, "y", "y", 0.0, 1000.0, 0.1, false);

        //Начальное множество параметров - исходных точек оптимизации
        List<Params> origin = new ArrayList<>();
        origin.add(new Params(1, 1.0));
        origin.add(new Params(2, 2.0));

        Pair<Params, Double> best = new CdmBotTrainer().getBestParams(
            properties.getGenes(),
            origin, // исходные точки
            p -> {  // функция, которая для каждой исходной точки подсчитвает результат (любого типа)
                return - Math.pow(p.x - 10, 2) - Math.pow(p.y - 10, 2);
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
