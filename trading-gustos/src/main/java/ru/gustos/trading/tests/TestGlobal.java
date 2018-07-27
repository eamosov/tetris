package ru.gustos.trading.tests;

import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
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
//            new Instrument("EOS","BTC"),
//            new Instrument("ONT","BTC"),
//            new Instrument("XRP","BTC"),
            new Instrument("BTC","USDT"),
            new Instrument("ETH","USDT"),
            new Instrument("BCC","USDT"),
            new Instrument("BNB","USDT"),
            new Instrument("NEO","USDT"),
            new Instrument("QTUM","USDT"),
            new Instrument("LTC","USDT"),
//            new Instrument("TRX","BTC"),
//            new Instrument("ADA","BTC"),
//            new Instrument("VEN","BTC"),
//            new Instrument("ZIL","BTC"),
//            new Instrument("GTO","BTC"),
//            new Instrument("GNT","BTC"),
//            new Instrument("LOOM","BTC"),
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
        ZonedDateTime to = ZonedDateTime.of(2018,7,10,0,0,0,0, ZoneId.systemDefault());
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;


        for (Instrument ii : instruments) {
            System.gc();
            System.out.println("Loading instrument: "+ii.toString());
            BarsCache cache = new BarsCache("cache.sqlite3");
            List<XBaseBar> bars = cache.getBars(exch.getName(), ii, interval, from, to);
//            bars = BarsPacker.packBars(bars,5);
            global.addInstrumentData(ii.toString(),new InstrumentData(exch,ii,bars, global));
        }
        System.out.println("Calc global data");
        global.calcData();
        return global;
    }

    public static void main(String[] args) throws IOException {

        int calcPeriod = 3600*12;
        Global global = init(instruments);
        int cpus = args.length>0?Integer.parseInt(args[0]):0;


        File ignoreFile = new File("ignore.txt");
        String ignorestring = "";
        if (ignoreFile.exists()) {
            ignorestring = FileUtils.readFileToString(ignoreFile).trim();
            MomentDataHelper.ignore.addAll(Arrays.stream(ignorestring.split(",")).collect(Collectors.toList()));
        }

//        StandardInstrumentCalc[] calcs = new StandardInstrumentCalc[instruments.length];
        for (int i = 0;i<instruments.length;i++) {
            InstrumentData data = global.getInstrument(instruments[i].toString());
            int bars = StandardInstrumentCalc.calcAllFrom;
            StandardInstrumentCalc c = new StandardInstrumentCalc(new InstrumentData(data, bars), cpus, false);

            new StandardInstrumentCalc(data, cpus, true); // calc future
            c.futuredata = data;
            for (;bars<data.size();bars++){
                if ((bars-StandardInstrumentCalc.calcAllFrom)%(60*12)==0)
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
        String res = name+" ignores "+ignorestring+"\n";
        try (FileWriter f = new FileWriter("testres.txt",true)) {
            f.write(res);
        }


//        Exporter.string2file("d:/weka/prev.arff",global.planalyzer.trainset.toString());
    }


}

