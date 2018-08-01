package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.global.timeseries.TimeSeries;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class TradeMethodsSolver {
    public static final long BIG_INTERVAL = 60 * 60 * 24 * 90;
    public static final long SMALL_INTERVAL = 60 * 60 * 24 * 30;
    String instrument;
    PLHistory[][] history;
    boolean wasNotNull;
    static PLHistoryAnalyzer[][] analyzers;

    TradeMethodsSolver(String instrument){
        this.instrument = instrument;
    }
    long firstTime;

    public void buy(long time, double price, boolean[] classifiers) {
        if (history==null) {
            firstTime = time;
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
        if (time-firstTime<BIG_INTERVAL) return null;
        int ni = 0, nj= 0;
        double best = 0;
        ArrayList<Pair<Integer,Double>> rates = new ArrayList<>();
        for (int i = 0;i<history.length;i++)
            for (int j = 0;j<history[i].length;j++){
            PLHistory cut = new PLHistory(history[i][j], time - BIG_INTERVAL, time);
            double p = cut.all.profit * cut.all.drawdown;
            if (p>1.03)
                rates.add(new Pair<>(j,p));
        }
        rates.sort(Comparator.comparing(Pair::getSecond));
        if (rates.size()>1)
            rates.subList(0,rates.size()/2).clear();
        Set<Integer> top = rates.stream().map(Pair::getFirst).collect(Collectors.toSet());
        for (int i = 0;i<history.length;i++)
            for (int j = 0;j<history[i].length;j++){
            PLHistory cut = new PLHistory(history[i][j], time - SMALL_INTERVAL, time);
            double p = cut.all.profit*cut.all.drawdown;
            if (!top.contains(j))
//                    if (p<1.1 || (!top.contains(j) && hh[j].findByBuyTime(t)==null))
                p -=1;
            else
                p = cut.all.profit;
            if (p > best) {
                best = p;
                ni = i;
                nj = j;
            }
        }
        if (best==0) return null;

        wasNotNull = true;

        return new Pair<>(ni-1,nj-1);
    }

    public static void saveAnalyzers(DataOutputStream out) throws IOException {
        for (int i = 0;i<analyzers.length;i++)
            for (int j = 0;j<analyzers[i].length;j++)
                analyzers[i][j].saveHistories(out);
    }

}
