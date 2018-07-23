package ru.gustos.trading.tests;

import kotlin.Pair;
import org.jfree.chart.ui.ApplicationFrame;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.global.Global;
import ru.gustos.trading.global.PLHistory;
import ru.gustos.trading.global.PLHistoryAnalyzer;
import ru.gustos.trading.visual.SimpleProfitGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;

import static ru.gustos.trading.tests.TestGlobal.init;

public class TestShowResults {
    static PLHistoryAnalyzer planalyzer1 = null;
    static PLHistoryAnalyzer planalyzer2 = null;
    static PLHistoryAnalyzer planalyzer3 = null;
    static HashSet<String> ignore = new HashSet<>();
    static SimpleProfitGraph graph = new SimpleProfitGraph();
    static Global global;


    public static void main(String[] args) {
        try (DataInputStream in = new DataInputStream(new FileInputStream("d:/tetrislibs/pl/pl.out"))) {
            planalyzer1 = new PLHistoryAnalyzer(in);
            planalyzer2 = new PLHistoryAnalyzer(in);
            planalyzer3 = new PLHistoryAnalyzer(in);
        } catch (Exception e){
            e.printStackTrace();
        }


        ArrayList<PLHistory> hh = planalyzer1.histories;
        global = init(hh.stream().map(pl->Instrument.Companion.parse(pl.instrument)).toArray(Instrument[]::new));
//        System.out.println(planalyzer3.histories.get(0).profitHistory.size());
//        System.out.println(planalyzer3.histories.get(1).profitHistory.size());

        ApplicationFrame frame = graph.getFrame();
        JPanel options = new JPanel();
        for (PLHistory h : hh){
            final PLHistory hc = h;
            final JCheckBox ch = new JCheckBox(h.instrument,true);
            options.add(ch);
            ch.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ch.isSelected())
                        ignore.remove(hc.instrument);
                    else
                        ignore.add(hc.instrument);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateGraph();
                        }
                    });
                }
            });
        }
        frame.getContentPane().add(options, BorderLayout.NORTH);
        updateGraph();
    }

    private static void updateGraph() {
        double moneyPart = 1.0/Math.max(1,planalyzer1.histories.size()-ignore.size());
        ArrayList<ArrayList<Pair<Long,Double>>>  graphs = new ArrayList<>();
        ArrayList<Pair<Long, Double>> h1 = planalyzer1.makeHistory(false, moneyPart, ignore);
//        graphs.add(h1);
        graphs.add(planalyzer1.makeHistory(false, moneyPart, ignore));
        graphs.add(planalyzer2.makeHistory(false, moneyPart, ignore));
//        graphs.add(planalyzer2.makeHistory(true, moneyPart,ignore));
        graphs.add(planalyzer3.makeHistory(false, moneyPart,ignore));
//        graphs.add(planalyzer3.makeHistoryNormalized(true, moneyPart,h1));
        graph.drawHistory(TestGlobal.makeMarketAveragePrice(global, planalyzer1, h1, ignore), graphs);
    }


}
