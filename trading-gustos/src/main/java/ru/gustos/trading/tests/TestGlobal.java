package ru.gustos.trading.tests;

import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.global.*;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import ru.gustos.trading.ml.J48AttributeFilter;
import weka.core.Instances;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TestGlobal{

    public static Instrument[] instruments = new Instrument[]{
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
            new Instrument("TRX","USDT"),

//            new Instrument("QTUM","USDT"),
//            new Instrument("ADA","USDT"),
//            new Instrument("ETC","USDT"),
//            new Instrument("EOS","USDT"),
    };

    static ZonedDateTime loadFrom = ZonedDateTime.of(2017,12,15,0,0,0,0, ZoneId.systemDefault());
    static ZonedDateTime loadTo = ZonedDateTime.of(2018,9,13,6,0,0,0, ZoneId.systemDefault());

    public static ExperimentData init(Instrument[] instruments, boolean withml){
        return init(instruments,withml,false, false);
    }
    public static ExperimentData init(Instrument[] instruments, boolean withml, boolean withbuysell, boolean withMarketBars){
        ExperimentData experimentData = null;
        try {
            experimentData = new ExperimentData(loadFrom, loadTo, fullSave, "experiment");
        } catch (IOException e) {
            e.printStackTrace();
        }
        experimentData.loadInstruments(withMarketBars, instruments, withml, withbuysell);
        return experimentData;
    }



    public static void saveResults(ExperimentData experimentData) throws Exception {
        String name = "pl/pl.out";
        int cc = 1;
        while (new File(name).exists()){
            name = "pl/pl"+cc+".out";
            cc++;
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(name))) {
            experimentData.saveGeneral(out);
        }
        experimentData.save();
        double moneyPart = 1.0/Math.max(1, experimentData.planalyzer1.histories.size());
        TimeSeriesDouble h2 = experimentData.planalyzer2.makeHistory(false, moneyPart, null);
        TimeSeriesDouble h3 = experimentData.planalyzer3.makeHistory(false, moneyPart, null);
        System.out.println(h2.lastOrZero());
        System.out.println(h3.lastOrZero());
        String res = name+"\n";
        String out = ZonedDateTime.now().toLocalDateTime().toString()+"\n";
        out+=res;
        out+=TestGlobalConfig.config.toString()+"\n";
        out+=h2.lastOrZero()+"\n";
        out+=h3.lastOrZero()+"\n";
        out+="base:"+ experimentData.planalyzer1.profits()+"\n";
        out+= experimentData.planalyzer2.profits()+"\n";
        out+= J48AttributeFilter.printUse();
//        out+="train kappas "+Arrays.toString(kappas)+"\n";
        try (FileWriter f = new FileWriter("testres.txt",true)) {
            f.write(out);
        }
        System.out.println(out);
//        Exporter.string2file("d:/weka/prev.arff",global.planalyzer.trainset.toString());

    }

    public static void main(String[] args) throws Exception {
        doIt(args);
    }

    static boolean withExport = false;
    static boolean fullSave = true;

    private static void doIt(String[] args) throws Exception {
        int cpus = args.length>0?Integer.parseInt(args[0]):4;

        CalcConfig config = CalcConfig.load("testconf.json");
//        MomentDataHelper.loadIgnore("ignore.txt");

        ExperimentData experimentData = init(instruments,false,false,true);

        clearExportDir();


        ZonedDateTime from = ZonedDateTime.of(2018,5,15,0,0,0,0, ZoneId.systemDefault());
        int buysToExport = 60*24*30;
        int buysTail = 60*24*5;
        int lag = 20;

        Instances corrTest = prepareCorrelationSet(lag, buysToExport);
        double[][] corrResults = new double[instruments.length][buysToExport];

        for (int i = 0;i<instruments.length;i++) {
            InstrumentData data = experimentData.getInstrument(instruments[i].toString());
            int bars = DecisionManager.calcAllFrom;
            int fromIndex = data.getBarIndex(from);
            DecisionManager c = new DecisionManager(config,experimentData, new InstrumentData(data, bars, true,false), cpus, false, fromIndex);
            if (withExport)
                c.makeExport();

            for (;bars<data.size();bars++){
                c.checkNeedRenew(false);
                c.addBar(data.bar(bars), data.marketBars.get(bars));
            }
            experimentData.setResult(instruments[i].toString(),c.data);

//            InstrumentData findata = c.data;
//            for (int j = 0;j<buysToExport;j++){
//                for (int l = 0;l<lag;l++) {
//                    double v = findata.helper.get(findata.data.get(findata.size() - buysTail - buysToExport + j-l), "@goodBuy|main");
//                    corrTest.get(j).setValue(i*lag+l,v);
//                }
//                corrResults[i][j] = findata.helper.get(findata.data.get(findata.size() - buysTail - buysToExport + j), "_goodBuy");
////                buysSet.get(j).setValue(i,v);
//            }



            System.out.println(experimentData.planalyzer2.profits());
            System.out.println(J48AttributeFilter.printUse());

            exportData(c);
        }
//        for (int i = 0;i<instruments.length;i++) {
//            for (int j = 0;j<buysToExport;j++)
//                corrTest.get(j).setValue(corrTest.numAttributes()-1,corrResults[i][j]);
//            Exporter.export2arff("buysSet_"+instruments[i].component1()+".arff",corrTest);
//        }
        saveResults(experimentData);
    }

    private static void clearExportDir() throws IOException {
        if (withExport){
            File file = new File("export");
            if (file.exists())
                FileUtils.deleteDirectory(file);
            file.mkdir();
        }

    }

    private static void exportData(DecisionManager c) {
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

    private static Instances prepareCorrelationSet(int lag, int size) {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0;i<instruments.length;i++){
            for (int j = 0;j<lag;j++)
                names.add(instruments[i].component1()+"_"+j);
        }
        names.add("result");
        return Exporter.makeBoolSet(names.toArray(new String[0]),size);
    }


}

