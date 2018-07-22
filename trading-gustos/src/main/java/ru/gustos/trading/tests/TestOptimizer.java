package ru.gustos.trading.tests;

import ru.gustos.trading.global.Global;
import ru.gustos.trading.global.GustosLogicOptimizator;
import ru.gustos.trading.global.InstrumentData;

public class TestOptimizer {

    public static void main(String[] args) {
        Global global = TestGlobal.init(TestGlobal.instruments);
        for (InstrumentData data : global.sheets.values()) {
            System.out.println(data.instrument);
            GustosLogicOptimizator[] ops = new GustosLogicOptimizator[10];
            for (int j = 0;j<10;j++) {
                ops[j] = new GustosLogicOptimizator(data, data.size() * j / 10, data.size() * (j + 1) / 10);
                System.out.println(ops[j].optimize(new GustosLogicOptimizator.Params()).getSecond());
            }
//            GustosLogicOptimizator.Params p = new GustosLogicOptimizator.Params();
//            for (int i = 0;i<p.params.length;i++) {
//                SimpleCharts charts = new SimpleCharts(data.instrument.toString()+" "+paramNames[i]);
//                for (int j = 0;j<10;j++){
//                    double[] d = ops[j].cut1d(p, i);
//                    String s = Arrays.toString(d).replace(" ", "").replace("[","").replace("]","");
//                    String paramName = paramNames[i];
//                    System.out.println(paramName+":" +s);
//                    charts.addChart(""+j,d);
//
//                }
//            }
        }
    }

}
