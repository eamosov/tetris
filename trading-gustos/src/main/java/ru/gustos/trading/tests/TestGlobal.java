package ru.gustos.trading.tests;

import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.MarketBarFactory;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.global.*;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import ru.gustos.trading.ml.J48AttributeFilter;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

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
            new Instrument("BTC","USDT"),
            new Instrument("ETH","USDT"),
            new Instrument("BNB","USDT"),
            new Instrument("LTC","USDT"),
            new Instrument("BCC","USDT"),
            new Instrument("NEO","USDT"),
            new Instrument("XRP","USDT"),
            new Instrument("XLM","USDT"),
            new Instrument("ICX","USDT"),
            new Instrument("ONT","USDT"),
            new Instrument("NULS","USDT"),
            new Instrument("VET","USDT"),
            new Instrument("IOTA","USDT"),

//            new Instrument("QTUM","USDT"),
//            new Instrument("IOTA","USDT"),
//            new Instrument("ADA","USDT"),
//            new Instrument("ETC","USDT"),
//            new Instrument("EOS","USDT"),
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

    public static Global init(Instrument[] instruments, boolean withml){
        return init(instruments,withml,false);
    }
    public static Global init(Instrument[] instruments, boolean withml, boolean withbuysell){
        Global global = new Global();



        for (Instrument ii : instruments) {
            System.gc();
            addInstrument(global,ii,withml,withbuysell);
        }
        System.out.println("Calc global data "+Arrays.deepToString(instruments));
        global.calcData();
        return global;
    }
    static ZonedDateTime loadFrom = ZonedDateTime.of(2017,12,15,0,0,0,0, ZoneId.systemDefault());
    static ZonedDateTime loadTo = ZonedDateTime.of(2018,9,13,6,0,0,0, ZoneId.systemDefault());

    static ArrayList<MarketBar> syncBars(ArrayList<? extends XBar> bars, Global global){
        int marketFrom = global.marketBarIndex(bars.get(0).getEndTime());
        ArrayList<MarketBar> result = new ArrayList<>(bars.size());
        int next = marketFrom+1;
        for (int i = 0;i<bars.size();i++){
            while (next<global.marketBars.size() && bars.get(i).getEndTime().isAfter(global.marketBars.get(next).getEndTime())) next++;
            result.add(global.marketBars.get(next-1));

        }
        return result;

    }

    public static void addInstrument(Global global, Instrument ii, boolean withml, boolean withbuysell){
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;
        if (DecisionManager.LOGS)
            System.out.println("Loading instrument: "+ii.toString());
        BarsCache cache = new BarsCache("cache.sqlite3");
        ArrayList<? extends XBar> bars = new ArrayList<>(cache.getBars(exch.getName(), ii, interval, loadFrom, loadTo));
//            bars = BarsPacker.packBars(bars,5);
//        int marketFrom = global.marketBarIndex(bars.get(0).getEndTime());
//
//        while (marketFrom+bars.size()>global.marketBars.size()) bars.remove(bars.size()-1);
//        List<MarketBar> mb = global.marketBars.subList(marketFrom, marketFrom + bars.size());
        List<MarketBar> mb = null;
        if (global.marketBars!=null)
            mb = syncBars(bars,global);


        global.addInstrumentData(ii.toString(),new InstrumentData(exch,ii,bars, mb, global, withml,withbuysell));
    }

    private static List<MarketBar> initMarketBars() {
        BarsCache cache = new BarsCache("cache.sqlite3");
        MarketBarFactory market = new MarketBarFactory(cache, BarInterval.ONE_MIN, "binance");
        List<MarketBar> marketBars = market.build(loadFrom, loadTo);
//        marketBars.add(0, marketBars.get(0)); // simulate time lag
        return marketBars;
    }



    public static void saveResults(Global global) throws IOException {
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
//            TradeMethodsSolver.saveAnalyzers(out);
            TreePizdunstvo.p.save(out);
            global.planalyzer2.saveModelTimes(out);
//            PizdunstvoData.pdbuy.save(out);
//            PizdunstvoData.pdsell.save(out);
        } catch (Exception e){
            e.printStackTrace();
        }
        double moneyPart = 1.0/Math.max(1,global.planalyzer1.histories.size());
        TimeSeriesDouble h2 = global.planalyzer2.makeHistory(false, moneyPart, null);
        TimeSeriesDouble h3 = global.planalyzer3.makeHistory(false, moneyPart, null);
        System.out.println(h2.lastOrZero());
        System.out.println(h3.lastOrZero());
        String res = name+"\n";
        String out = ZonedDateTime.now().toLocalDateTime().toString()+"\n";
        out+=res;
        out+=TestGlobalConfig.config.toString()+"\n";
        out+=h2.lastOrZero()+"\n";
        out+=h3.lastOrZero()+"\n";
        out+="base:"+global.planalyzer1.profits()+"\n";
        out+=global.planalyzer2.profits()+"\n";
        out+= J48AttributeFilter.printUse();
//        out+="train kappas "+Arrays.toString(kappas)+"\n";
        try (FileWriter f = new FileWriter("testres.txt",true)) {
            f.write(out);
        }
        System.out.println(out);
//        Exporter.string2file("d:/weka/prev.arff",global.planalyzer.trainset.toString());

    }

    public static void main(String[] args) throws IOException {
        doIt(args);
    }

    private static void doIt(String[] args) throws IOException {
        Global global = new Global();
        int cpus = args.length>0?Integer.parseInt(args[0]):4;

        CalcConfig config = CalcConfig.load("testconf.json");
        File ignoreFile = new File("ignore.txt");
        String ignorestring = "";
        if (ignoreFile.exists()) {
            ignorestring = FileUtils.readFileToString(ignoreFile);
            String[] ss = ignorestring.split("\n");
            MomentDataHelper.ignore.addAll(Arrays.stream(ss[0].trim().split(",")).collect(Collectors.toList()));
        }

        boolean withExport = false;

        if (withExport){

            File file = new File("export");
            if (file.exists())
                FileUtils.deleteDirectory(file);
            file.mkdir();
        }
        global.setMarket(initMarketBars());
        ZonedDateTime from = ZonedDateTime.of(2018,5,15,0,0,0,0, ZoneId.systemDefault());
//        ZonedDateTime dontRenewAfter = ZonedDateTime.of(2018,6,20,0,0,0,0, ZoneId.systemDefault());
        double[] kappas = new double[instruments.length];
        for (int i = 0;i<instruments.length;i++) {
            addInstrument(global,instruments[i],true,false);
            InstrumentData data = global.getInstrument(instruments[i].toString());
            int bars = DecisionManager.calcAllFrom;
            int fromIndex = data.getBarIndex(from);
            DecisionManager c = new DecisionManager(config,new InstrumentData(data, bars), cpus, false, fromIndex);
            if (withExport)
                c.makeExport();
//            c.dontRenewAfter = data.getBarIndex(dontRenewAfter);
            data.global = null;
//            new DecisionManager(config, data, cpus, true,0); // calc future
//            c.futuredata = data;
            for (;bars<data.size();bars++){
                c.checkNeedRenew(false);
                c.addBar(data.bar(bars), data.marketBars.get(bars));
            }
            kappas[i] = c.models.model.kappas/Math.max(1,c.models.model.kappascnt);
            System.out.println(global.planalyzer2.profits());

            if (withExport){
                ArrayList<Pair<Instances, Instances>> export = c.export;
                for (int j = 0;j<export.size();j++) {
                    Pair<Instances, Instances> p = export.get(j);

                    Exporter.export2tdf("export/"+c.data.instrument+"_train_"+j+".tdf",p.getFirst());
                    Exporter.export2tdf("export/"+c.data.instrument+"_exam_"+j+".tdf",p.getSecond());

                    Exporter.export2arff("export/"+c.data.instrument+"_train_"+j+".arff",p.getFirst());
                    Exporter.export2arff("export/"+c.data.instrument+"_exam_"+j+".arff",p.getSecond());

                }
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
//        double moneyPart = 0.1;
//        ArrayList<TimeSeriesDouble>  graphs = new ArrayList<>();
//        TimeSeriesDouble h1 = global.planalyzer1.makeHistory(false, moneyPart,null);
//        graphs.add(h1);
//        graphs.add(global.planalyzer1.makeHistoryNormalized(true, moneyPart,h1,null));
//        graphs.add(global.planalyzer2.makeHistory(false, moneyPart,null));
//        graphs.add(global.planalyzer2.makeHistoryNormalized(true, moneyPart,h1));
//        graphs.add(global.planalyzer3.makeHistory(false, moneyPart,null));
//        graphs.add(global.planalyzer3.makeHistoryNormalized(true, moneyPart,h1));
//        new SimpleProfitGraph().drawHistory(makeMarketAveragePrice(global,global.planalyzer1,h1, null),graphs);
        saveResults(global);
    }


}

