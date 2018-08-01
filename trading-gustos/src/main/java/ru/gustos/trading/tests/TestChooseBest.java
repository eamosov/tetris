package ru.gustos.trading.tests;

import kotlin.Pair;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.global.PLHistory;
import ru.gustos.trading.global.PLHistoryAnalyzer;
import ru.gustos.trading.global.PizdunstvoData;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestChooseBest {
    static PLHistoryAnalyzer planalyzer1 = null;
    static PLHistoryAnalyzer planalyzer2 = null;
    static PLHistoryAnalyzer planalyzer3 = null;
    static PLHistoryAnalyzer[] planalyzers = new PLHistoryAnalyzer[4];

    public static void main(String[] args) {
        try (DataInputStream in = new DataInputStream(new FileInputStream("d:/tetris/pl/pl33.out"))) {
            planalyzer1 = new PLHistoryAnalyzer(in);
            planalyzer2 = new PLHistoryAnalyzer(in);
            planalyzer3 = new PLHistoryAnalyzer(in);
            for (int i = 0;i<planalyzers.length;i++)
                planalyzers[i] = new PLHistoryAnalyzer(in);
            PizdunstvoData.pdbuy.load(in);
            PizdunstvoData.pdsell.load(in);

        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("buy");
        PizdunstvoData.pdbuy.analyze();
        System.out.println("sell");
        PizdunstvoData.pdsell.analyze();


        Instrument[] instruments = planalyzers[0].histories.stream().map(pl -> Instrument.Companion.parse(pl.instrument)).distinct().toArray(Instrument[]::new);
        List<Long> times = new ArrayList<>();
        long from = ZonedDateTime.of(2018,4,1,0,0,0,0, ZoneId.systemDefault()).toEpochSecond();
        double total = 1;
        for (Instrument instr : instruments) {
            System.out.println(instr);
            PLHistory[] hh = new PLHistory[planalyzers.length];
            for (int i = 0; i < planalyzers.length; i++) {
                hh[i] = planalyzers[i].get(instr.toString());
                times.addAll(hh[i].profitHistory.stream().map(p -> p.timeBuy).collect(Collectors.toList()));
                System.out.println(i+":"+new PLHistory(hh[i],from,Long.MAX_VALUE).totalProfit());
            }


            times.sort(Long::compare);

            for (int i = times.size() - 2; i >= 0; i--)
                if (times.get(i).equals(times.get(i + 1)) || times.get(i)<from)
                    times.remove(i + 1);

            double m = 1;
            int cc = 0;
            long nextt = 0;
            for (int i = 0; i < times.size(); i++) {
                long t = times.get(i);
                if (t<nextt) continue;
                int n = 0;
                double best = 0;
                ArrayList<Pair<Integer,Double>> rates = new ArrayList<>();
                for (int j = 0; j < hh.length; j++) {
                    PLHistory cut = new PLHistory(hh[j], t - 60 * 60 * 24 * 90, t);
                    double p = cut.all.profit * cut.all.drawdown;
                    if (p>1.03)
                        rates.add(new Pair<>(j,p));
                }
                rates.sort(Comparator.comparing(Pair::getSecond));
                if (rates.size()>1)
                    rates.subList(0,rates.size()/2).clear();
                Set<Integer> top = rates.stream().map(p -> p.getFirst()).collect(Collectors.toSet());
                for (int j = 0; j < hh.length; j++) {
                    PLHistory cut = new PLHistory(hh[j], t - 60 * 60 * 24 * 30, t);
                    double p = cut.all.profit*cut.all.drawdown;
                    if (!top.contains(j))
//                    if (p<1.1 || (!top.contains(j) && hh[j].findByBuyTime(t)==null))
                        p -=1;
                    else
                        p = cut.all.profit;
                    if (p > best) {
                        best = p;
                        n = j;
                    }
                }
                best = 1;
                n = 12;
                if (best > 0) {
                    PLHistory.PLTrade tr = hh[n].findByBuyTime(t);
                    if (tr != null) {
                        m *= tr.profit;
                        nextt = tr.timeSell+1;
                        cc++;
                    }
                }

            }
            System.out.println(m + " " + cc);
            total*=m;
        }
        System.out.println("total: "+total);
    }

}

