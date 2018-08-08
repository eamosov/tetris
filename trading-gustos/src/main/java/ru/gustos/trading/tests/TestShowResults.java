package ru.gustos.trading.tests;

import kotlin.Pair;
import org.jfree.chart.ui.ApplicationFrame;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.global.Global;
import ru.gustos.trading.global.InstrumentData;
import ru.gustos.trading.global.PLHistory;
import ru.gustos.trading.global.PLHistoryAnalyzer;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import ru.gustos.trading.visual.SimpleProfitGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import static ru.gustos.trading.tests.TestGlobal.init;

public class TestShowResults {
    static PLHistoryAnalyzer planalyzer1 = null;
    static PLHistoryAnalyzer planalyzer2 = null;
    static PLHistoryAnalyzer planalyzer3 = null;
    static PLHistoryAnalyzer[] planalyzers = new PLHistoryAnalyzer[4];
    static HashSet<String> ignore = new HashSet<>();
    static SimpleProfitGraph graph = new SimpleProfitGraph();
    static Global global;


    public static void main(String[] args) {
        System.out.println((new Date()).getTime());
        try (DataInputStream in = new DataInputStream(new FileInputStream("d:/tetrislibs/pl/pl706.out"))) {
            planalyzer1 = new PLHistoryAnalyzer(in);
            planalyzer2 = new PLHistoryAnalyzer(in);
            planalyzer3 = new PLHistoryAnalyzer(in);
            for (int i = 0;i<planalyzers.length;i++)
                planalyzers[i] = new PLHistoryAnalyzer(in);
        } catch (Exception e){
            e.printStackTrace();
        }

        planalyzer1 = planalyzers[0];
        ArrayList<PLHistory> hh = planalyzer1.histories;
        global = init(hh.stream().map(pl->Instrument.Companion.parse(pl.instrument)).distinct().toArray(Instrument[]::new));
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
        ArrayList<TimeSeriesDouble>  graphs = new ArrayList<>();
        TimeSeriesDouble h1 = planalyzer1.makeHistory(false, moneyPart, ignore);
        graphs.add(h1);
//        graphs.add(planalyzer1.makeHistory(false, moneyPart, ignore));
        TimeSeriesDouble h2 = planalyzer2.makeHistory(false, moneyPart, ignore);
        graphs.add(h2);
//        graphs.add(planalyzer2.makeHistory(true, moneyPart,ignore));
        TimeSeriesDouble h3 = planalyzer3.makeHistory(false, moneyPart, ignore);
        graphs.add(h3);
        System.out.println(h2.lastOrZero());
        System.out.println(h3.lastOrZero());
//        for (int i = 0;i<planalyzers.length;i++)
//            graphs.add(planalyzers[i].makeHistory(false, moneyPart,ignore));
//        graphs.add(planalyzer3.makeHistoryNormalized(true, moneyPart,h1));
        TimeSeriesDouble price = TestGlobal.makeMarketAveragePrice(global, planalyzer1, h1, ignore);
        graph.drawHistory(price, graphs);
//        exportToWeka();
//        exportPriceToWeka("BNB_USDT");
//        exportPriceToWeka("BTC_USDT");
//        exportPriceToWeka("ETH_USDT");
//        exportPriceToWeka("QTUM_USDT");
    }

    private static void exportToWeka(){
        StringBuilder sb = new StringBuilder();
        sb.append("@relation ").append("btc").append("\n\n");
        sb.append("@attribute time numeric\n");
        sb.append("@attribute op numeric\n");
        sb.append("@attribute price numeric\n");
        sb.append("\n@data\n");
        ArrayList<PLHistory.PLTrade> h1 = planalyzer1.get("BNB_USDT").profitHistory;
        InstrumentData p = global.getInstrument("BNB_USDT");
        double money = 1;
        double firstprice = 0;
        long firsttime = 0;
        for (int i = 0;i<h1.size();i++){
            PLHistory.PLTrade t = h1.get(i);
            if (firsttime==0)
                firsttime = t.timeBuy;
            double price = p.getBarAt(t.timeBuy).getClosePrice();
            if (firstprice==0)
                firstprice = price;
            money *= t.profit;
            sb.append(t.timeBuy-firsttime).append(",");
            sb.append(money).append(",").append(price/firstprice).append("\n");
        }
        Exporter.string2file("d:/tetrislibs/wekadata/bnbtime.arff",sb.toString());
    }

    private static void exportPriceToWeka(String key){
        StringBuilder sb = new StringBuilder();
        sb.append("@relation ").append("btc").append("\n\n");
        sb.append("@attribute price numeric\n");
        sb.append("@attribute time numeric\n");
        sb.append("\n@data\n");
        InstrumentData p = global.getInstrument(key);
        long firsttime = 0;
        double firstprice = 0;
        for (int i = 0;i<p.size();i++){
            if (firsttime==0)
                firsttime = p.getBeginTime();
            double price = p.bar(i).getClosePrice();
            if (firstprice==0)
                firstprice = price;

            sb.append(price/firstprice).append(",");
            sb.append(p.bars.timeAt(i)-firsttime).append("\n");
        }
        Exporter.string2file("d:/tetrislibs/wekadata/"+key+"price.arff",sb.toString());
    }

}

