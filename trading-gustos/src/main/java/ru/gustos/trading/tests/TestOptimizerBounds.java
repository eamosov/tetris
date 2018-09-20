package ru.gustos.trading.tests;

import kotlin.Pair;
import ru.gustos.trading.global.ExperimentData;
import ru.gustos.trading.global.GustosLogicOptimizator;
import ru.gustos.trading.global.InstrumentData;
import ru.gustos.trading.visual.SimpleCharts;

public class TestOptimizerBounds {

    public static void main(String[] args) {
        ExperimentData experimentData = TestGlobal.init(TestGlobal.instruments,false);
        for (InstrumentData data : experimentData.data) {
            System.out.println(data.instrument);
            GustosLogicOptimizator[] ops = new GustosLogicOptimizator[10];
            SimpleCharts charts = new SimpleCharts(data.instrument.toString(),5);

            for (int j = 2; j < 8; j++) {
                ops[j] = new GustosLogicOptimizator(data, data.size() * (j-2) / 10, data.size() * (j + 1) / 10);
                Pair<GustosLogicOptimizator.Params, Double> opt = ops[j].optimize(new GustosLogicOptimizator.Params());
                double[] dd = new double[20];
                for (int k = 0;k<dd.length;k++){
                    GustosLogicOptimizator.Stat calc = new GustosLogicOptimizator(data, data.size() * (j-2) / 10, data.size() * (j + 1) / 10 + (k - 10) * 60 * 24).calc(opt.getFirst(), 1);
                    dd[k] = calc.profit;
                    System.out.print(String.format(" %.3g", calc.profit));
                }
                charts.addChart(""+j,dd);
                System.out.println();
            }
        }
    }
}

