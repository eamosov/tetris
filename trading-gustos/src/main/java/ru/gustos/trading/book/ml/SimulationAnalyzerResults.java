package ru.gustos.trading.book.ml;

import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.TestUtils;

import java.util.Arrays;

public class SimulationAnalyzerResults{

    static void historyAndPart() throws Exception {
        double[][] pp = new double[8][6];
        double total = 1;
        double total2 = 1;
        int tc = 0;
        for (int history = 1;history<=pp.length;history++) {
            double sum = 0;
            int cc = 0;
            int partFrom = 96 - pp[history - 1].length+1;
            for (int part = partFrom+1; part <= 96; part++) {
                System.out.println("history: " + history + ", part: " + part);
                SimulationAnalyzer.doSelection(history+2, part,0);
                double v = SimulationAnalyzer.totalSelected;//SimulationAnalyzer.selectedTop10 / SimulationAnalyzer.totalTopUsual;
                pp[history-1][part - partFrom] = v;
                total +=v;
                total2 += SimulationAnalyzer.totalUsual;// SimulationAnalyzer.totalSelected / SimulationAnalyzer.totalTopUsual;
                sum+=v;
                tc++;
                cc++;
            }
            pp[history-1][0] = sum/cc;
        }

        System.out.println(Arrays.deepToString(pp).replace("], ","\n").replace("[","").replace("]","").replace(", "," ").replace("  "," ").replace(".",","));
        System.out.println(total/tc);
        System.out.println(total2/tc);

    }

    static void historyAndFrom() throws Exception {
        double[][] pp = new double[10][11];
        for (int from = 0;from<10;from++) {
            double sum = 0;
            for (int history = 1; history < 11; history++) {
                System.out.println("history: " + history + ", from: " + from);
                SimulationAnalyzer.doSelection(1, 65+history,from);
                pp[from][history] = SimulationAnalyzer.totalSelected;
                sum+=SimulationAnalyzer.totalSelected;
            }
            pp[from][0] = sum/10;
        }
        System.out.println(Arrays.deepToString(pp).replace("], ","\n").replace("[","").replace("]","").replace(", "," ").replace("  "," ").replace(".",","));

    }

    public static void main(String[] args) throws Exception {
        SimulationAnalyzer.init();
        SimulationAnalyzer.loadResults();
        historyAndPart();
//        SimulationAnalyzer.doSelection(5, 97,0);

    }
}
