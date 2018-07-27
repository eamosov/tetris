package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.global.timeseries.TimeSeries;

import java.io.DataOutputStream;
import java.io.IOException;

public class TradeMethodsSolver {
    String instrument;
    PLHistory[][] history;
    boolean wasNotNull;
    static PLHistoryAnalyzer[][] analyzers;

    TradeMethodsSolver(String instrument){
        this.instrument = instrument;
    }


    public void buy(long time, double price, boolean[] classifiers) {
        if (history==null) {
            int n = classifiers.length + 1;
            if (analyzers==null){
                analyzers = new PLHistoryAnalyzer[n][n];
                for (int i = 0;i<analyzers.length;i++)
                    for (int j = 0;j<analyzers[i].length;j++)
                        analyzers[i][j] = new PLHistoryAnalyzer(false);

            }
            history = new PLHistory[n][n];
            for (int i = 0;i<history.length;i++)
                for (int j = 0;j<history[i].length;j++)
                    history[i][j] = new PLHistory(instrument,analyzers[i][j]);

        }
        for (int i = 0;i<1+classifiers.length;i++)
            for (int j = 0;j<1+classifiers.length;j++)
                if (i==0 || classifiers[i-1])
                    history[i][j].buyMoment(price,time);

    }

    public void sell(long time, double price, boolean[] classifiers) {
        if (history==null) return;
        for (int i = 0;i<1+classifiers.length;i++)
            for (int j = 0;j<1+classifiers.length;j++)
                if (j==0 || classifiers[j-1])
                    history[i][j].sellMoment(price,time);

    }

    public boolean wasNotNull(){
        return wasNotNull;
    }


    public Pair<Integer,Integer> chooseStrategy(long time){
        int buy = 0;
        int sell = 0;
        double best = 0;

//        for (int b = 0;b<history.length;b++)
//            for (int s = 0;s<history[b].length;s++){
//                double p = history[b][s].getPossibleProfit(time);
//                if (p>best){
//                    best = p;
//                    buy = b;
//                    sell = s;
//                }
//            }
//        if (best==0) return null;

        wasNotNull = true;

        return new Pair<>(0,0);
    }

    public static void saveAnalyzers(DataOutputStream out) throws IOException {
        for (int i = 0;i<analyzers.length;i++)
            for (int j = 0;j<analyzers[i].length;j++)
                analyzers[i][j].saveHistories(out);
    }
}
