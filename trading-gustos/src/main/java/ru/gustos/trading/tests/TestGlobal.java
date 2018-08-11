package ru.gustos.trading.tests;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.gustos.trading.book.indicators.IndicatorInitData;
import ru.gustos.trading.global.*;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import ru.gustos.trading.visual.SimpleProfitGraph;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class TestGlobal{

    static Instrument[] instruments = new Instrument[]{
//            new Instrument("BTC","USDT"),
//            new Instrument("ETH","USDT"),
//            new Instrument("BCC","USDT"),
//            new Instrument("BNB","USDT"),
//            new Instrument("LTC","USDT"),
//            new Instrument("NEO","USDT"),
//            new Instrument("QTUM","USDT"),
            new Instrument("XRP","USDT"),
            new Instrument("IOTA","USDT"),
            new Instrument("XLM","USDT"),
            new Instrument("ADA","USDT"),
//            new Instrument("ETC","USDT"),
//            new Instrument("TUSD","USDT"),
//            new Instrument("ICX","USDT"),
//            new Instrument("EOS","USDT"),
//            new Instrument("ONT","USDT"),
//            new Instrument("TRX","USDT"),
    };

    public static TimeSeriesDouble makeMarketAveragePrice(Global global, PLHistoryAnalyzer pl, TimeSeriesDouble trades, HashSet<String> ignore) {
        TimeSeriesDouble result = new TimeSeriesDouble(trades.size());
        double[] k = new double[pl.histories.size()];
        for (int j = 0;j<pl.histories.size();j++){
            PLHistory plHistory = pl.histories.get(j);
            k[j] = plHistory.profitHistory.size()==0?0:1.0/plHistory.profitHistory.get(0).buyCost;
        }

        for (int i = 0;i<trades.size();i++){
            long time = trades.time(i);
            double sum = 0;
            int cc = 0;
            for (int j = 0;j<pl.histories.size();j++) if (ignore==null || !ignore.contains(pl.histories.get(j).instrument)){
                XBar bar = global.getInstrument(pl.histories.get(j).instrument).getBarAt(time);
                if (bar!=null) {
                    sum += bar.getClosePrice() * k[j];
                    cc++;
                }
            }
            if (cc>0)
                sum/=cc;
            else
                sum = 1;
            result.add(sum,time);
        }
        return result;
    }

    public static Global init(Instrument[] instruments){
        Global global = new Global();

        ZonedDateTime from = ZonedDateTime.of(2017,12,15,0,0,0,0, ZoneId.systemDefault());
        ZonedDateTime to = ZonedDateTime.of(2018,8,11,0,0,0,0, ZoneId.systemDefault());
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;


        for (Instrument ii : instruments) {
            System.gc();
            if (StandardInstrumentCalc.LOGS)
                System.out.println("Loading instrument: "+ii.toString());
            BarsCache cache = new BarsCache("cache.sqlite3");
            List<XBaseBar> bars = cache.getBars(exch.getName(), ii, interval, from, to);
//            bars = BarsPacker.packBars(bars,5);
            global.addInstrumentData(ii.toString(),new InstrumentData(exch,ii,bars, global));
        }
        System.out.println("Calc global data "+Arrays.deepToString(instruments));
        global.calcData();
        return global;
    }

    public static void main(String[] args) throws IOException {
        doIt(args);
    }

    private static void doIt(String[] args) throws IOException {
        Global global = init(instruments);
        int cpus = args.length>0?Integer.parseInt(args[0]):4;

        StandardInstrumentCalc.goodmomentscount = TestGlobalConfig.config.goodMoments;
        StandardInstrumentCalc.badmomentscount = TestGlobalConfig.config.badMoments;
        StandardInstrumentCalc.TREES = TestGlobalConfig.config.trees;
        MomentDataHelper.threshold = TestGlobalConfig.config.threshold;
        File ignoreFile = new File("ignore.txt");
        String ignorestring = "";
        if (ignoreFile.exists()) {
            ignorestring = FileUtils.readFileToString(ignoreFile);
            String[] ss = ignorestring.split("\n");
            MomentDataHelper.ignore.addAll(Arrays.stream(ss[0].trim().split(",")).collect(Collectors.toList()));
        }

//        StandardInstrumentCalc[] calcs = new StandardInstrumentCalc[instruments.length];
        for (int i = 0;i<instruments.length;i++) {
            InstrumentData data = global.getInstrument(instruments[i].toString());
            int bars = StandardInstrumentCalc.calcAllFrom;
            StandardInstrumentCalc c = new StandardInstrumentCalc(new InstrumentData(data, bars), cpus, false,false);

            data.global = null;
            new StandardInstrumentCalc(data, cpus, true,false); // calc future
            c.futuredata = data;
            for (;bars<data.size();bars++){
                c.checkNeedRenew(false);
                c.addBar(data.bar(bars));
            }

        }
//        long toTime = ZonedDateTime.now().toEpochSecond();
//        long time = global.minTime;
//        int cc = 0;
//        while (time<toTime && !new File("stop").exists()) {
//            cc++;
//            System.out.println("go next 12h ("+cc+")");
//            for (int i = 0; i < calcs.length; i++) {
//                InstrumentData instrument = global.getInstrument(instruments[i].toString());
//                calcs[i].calcTo(time + calcPeriod);
//            }
//            time+=calcPeriod;
//        }

//        for (int i = 0;i<calcs.length;i++)
//            System.out.println(instruments[i]+" "+calcs[i].plhistoryBase.toPlusMinusString());
        double moneyPart = 0.1;
        ArrayList<TimeSeriesDouble>  graphs = new ArrayList<>();
        TimeSeriesDouble h1 = global.planalyzer1.makeHistory(false, moneyPart,null);
        graphs.add(h1);
//        graphs.add(global.planalyzer1.makeHistoryNormalized(true, moneyPart,h1,null));
        graphs.add(global.planalyzer2.makeHistory(false, moneyPart,null));
//        graphs.add(global.planalyzer2.makeHistoryNormalized(true, moneyPart,h1));
        graphs.add(global.planalyzer3.makeHistory(false, moneyPart,null));
//        graphs.add(global.planalyzer3.makeHistoryNormalized(true, moneyPart,h1));
//        new SimpleProfitGraph().drawHistory(makeMarketAveragePrice(global,global.planalyzer1,h1, null),graphs);
        String name = "pl/pl.out";
        int cc = 1;
        while (new File(name).exists()){
            name = "pl/pl"+cc+".out";
            cc++;
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(name))) {
            global.planalyzer1.saveHistories(out);
            global.planalyzer2.saveHistories(out);
            global.planalyzer3.saveHistories(out);
            TradeMethodsSolver.saveAnalyzers(out);
            PizdunstvoData.pdbuy.save(out);
            PizdunstvoData.pdsell.save(out);
        } catch (Exception e){
            e.printStackTrace();
        }
        moneyPart = 1.0/Math.max(1,global.planalyzer1.histories.size());
        TimeSeriesDouble h2 = global.planalyzer2.makeHistory(false, moneyPart, null);
        TimeSeriesDouble h3 = global.planalyzer3.makeHistory(false, moneyPart, null);
        System.out.println(h2.lastOrZero());
        System.out.println(h3.lastOrZero());
        String res = name+" ignores "+ignorestring+"\n";
        try (FileWriter f = new FileWriter("testres.txt",true)) {
            f.write(""+ZonedDateTime.now().toLocalDateTime().toString()+"\n");
            f.write(res);
            f.write(TestGlobalConfig.config.toString()+"\n");
            f.write(""+h2.lastOrZero()+"\n");
            f.write(""+h3.lastOrZero()+"\n");
            f.write(global.planalyzer3.profits()+"\n");
        }

//        Exporter.string2file("d:/weka/prev.arff",global.planalyzer.trainset.toString());

    }


}

