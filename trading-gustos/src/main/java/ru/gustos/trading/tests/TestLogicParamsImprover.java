package ru.gustos.trading.tests;

import kotlin.Pair;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.global.*;
import ru.gustos.trading.visual.SimpleCharts;
import ru.gustos.trading.visual.SimpleProfitGraph;

import java.util.ArrayList;
import java.util.Arrays;

public class TestLogicParamsImprover {

    public static void main(String[] args) {
        Global global = TestGlobal.init(new Instrument[]{new Instrument("BTC","USDT")});
        for (InstrumentData data : global.sheets.values()) {
            System.out.println(data.instrument);

            int optInterval = 60 * 24 * 30;
            int exam = 60 * 24 * 2;
            int step = 60*24;

            int to = optInterval;

            GustosLogicOptimizator opt = new GustosLogicOptimizator(data, to-GustosLogicOptimizator.INIT_CALC_FROM, data.size());
            PLHistory stdHistory = new PLHistory(data.instrument.toString(),null);
            opt.calc(new GustosLogicOptimizator.Params(),1,stdHistory);
            PLHistoryAnalyzer anal1 = new PLHistoryAnalyzer(false);
            PLHistoryAnalyzer anal2 = new PLHistoryAnalyzer(false);
            anal1.add(stdHistory);

            PLHistory optHistory = new PLHistory(data.instrument.toString(),null);
            anal2.add(optHistory);

            GustosLogicOptimizator.Params p = new GustosLogicOptimizator.Params();
            opt = new GustosLogicOptimizator(data, 0, to - exam);
            p = opt.optimize(p).getFirst();

            while (to<data.size()) {
//                GustosLogicOptimizator examOpt = new GustosLogicOptimizator(data, to-exam-GustosLogicOptimizator.INIT_CALC_FROM, to);
//                boolean profit = examOpt.calc(p,1).profit>1;
//                if (profit) {
                    GustosLogicOptimizator nextStep = new GustosLogicOptimizator(data, to-GustosLogicOptimizator.INIT_CALC_FROM, to+step);
                    nextStep.calc(p,1, optHistory);
//                }
                to+=step;
                opt = new GustosLogicOptimizator(data, to-optInterval, to);
                GustosLogicOptimizator.Params pp = new GustosLogicOptimizator.Params(p);
                p = opt.localOptimize(p).getFirst();
                System.out.println(pp.dif(p));
//                p = opt.optimize(p).getFirst();

                System.out.print(".");
            }
            System.out.println();

            SimpleProfitGraph graph = new SimpleProfitGraph();
            ArrayList<ArrayList<Pair<Long,Double>>>  graphs = new ArrayList<>();
            ArrayList<Pair<Long, Double>> h1 = anal1.makeHistory(false, 1, null);
            graphs.add(h1);
            ArrayList<Pair<Long, Double>> h2 = anal2.makeHistory(false, 1, null);
            graphs.add(h2);
            graph.drawHistory(TestGlobal.makeMarketAveragePrice(global, anal1, h1, null), graphs);
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
