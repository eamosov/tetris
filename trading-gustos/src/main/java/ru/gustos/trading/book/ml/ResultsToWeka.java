package ru.gustos.trading.book.ml;

import com.google.gson.Gson;
import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.bot.TradesStatsShort;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.ProfitCalculator;
import ru.efreet.trading.trainer.Metrica;
import ru.gustos.trading.GustosBotLogicParams;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ResultsToWeka {

    static ArrayList<Method> fields = new ArrayList<>();

    static HashMap<String,Integer> limits = new HashMap<>();
    static HashMap<String,Double> muls = new HashMap<>();
    private static Sheet sheet;

    public static void initFields(){

//        for (Method f : GustosBotLogicParams.class.getMethods()){
//            if (f.getReturnType()==Integer.class && f.getName().startsWith("get")){
//                fields.add(f);
//            }
//        }
        try {
//            fields.add(GustosBotLogicParams.class.getMethod("getBuyBoundDiv"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellBoundDiv"));
            fields.add(GustosBotLogicParams.class.getMethod("getVolumeShort"));
            fields.add(GustosBotLogicParams.class.getMethod("getBuyWindow"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellWindow"));
            fields.add(GustosBotLogicParams.class.getMethod("getBuyDiv"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellDiv"));
//            fields.add(GustosBotLogicParams.class.getMethod("getBuyVolumeWindow"));
//            fields.add(GustosBotLogicParams.class.getMethod("getSellVolumeWindow"));
            fields.add(GustosBotLogicParams.class.getMethod("getVolumePow1"));
            fields.add(GustosBotLogicParams.class.getMethod("getVolumePow2"));

            fields.add(GustosBotLogicParams.class.getMethod("getVolumePow1Sq"));
            fields.add(GustosBotLogicParams.class.getMethod("getVolumePow2Sq"));
            fields.add(GustosBotLogicParams.class.getMethod("getBuyWindowSq"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellWindowSq"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellBoundDivSq"));
            fields.add(GustosBotLogicParams.class.getMethod("getVolumeShortSq"));
            fields.add(GustosBotLogicParams.class.getMethod("getBuyDivSq"));
            fields.add(GustosBotLogicParams.class.getMethod("getSellDivSq"));


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        limits.put("BuyBoundDiv",60);
        limits.put("SellBoundDiv",60);
        limits.put("VolumeShort",60);
        limits.put("BuyWindow",120); // 1200
        limits.put("SellWindow",120); // 1200
        limits.put("BuyDiv",60);
        limits.put("SellDiv",60);
        limits.put("BuyVolumeWindow",200);
        limits.put("SellVolumeWindow",200);
        limits.put("VolumePow1",50);
        limits.put("VolumePow2",50);

        muls.put("BuyBoundDiv",1.0);
        muls.put("SellBoundDiv",1.0);
        muls.put("VolumeShort",1.0);
        muls.put("BuyWindow",1.0);
        muls.put("SellWindow",1.0);
        muls.put("BuyDiv",1.0);
        muls.put("SellDiv",1.0);
        muls.put("BuyVolumeWindow",1.0);
        muls.put("SellVolumeWindow",1.0);
        muls.put("VolumePow1",1.0);
        muls.put("VolumePow2",1.0);
    }

    public static int fieldsToData(double[] instance, GustosBotLogicParams args) throws InvocationTargetException, IllegalAccessException {
        int i = 0;
        for (Method f :  fields) {
            int v = (Integer) f.invoke(args);
            String key = f.getName().substring(3);
            if (limits.containsKey(key))
                v = (int)(Math.min(v,limits.get(key))*muls.get(key));
            instance[i++] = v;
        }
        return i;
    }

    public static boolean fieldsCheck(GustosBotLogicParams args) throws InvocationTargetException, IllegalAccessException {
        int i = 0;
        for (Method f :  fields) {
            int v = (Integer) f.invoke(args);
            String key = f.getName().substring(3);
            if (limits.containsKey(key) && v>limits.get(key)) return false;
        }
        return true;
    }

    public static ArrayList<Attribute> makeAttributes(){
        ArrayList<Attribute> res = new ArrayList<>();
        for (Method f :  fields) {
            res.add(new Attribute(f.getName().substring(3)));
        }
        res.add(new Attribute("metrica"));
        res.add(new Attribute("pearson"));
        res.add(new Attribute("sma"));
        res.add(new Attribute("profit_num"));
        res.add(new Attribute("trades"));
        res.add(new Attribute("profit", Arrays.asList("false", "true")));
//        res.add(new Attribute("metrica"));
        return res;
    }

    public static Instances initDataSet(){
        ArrayList<Attribute> infos = makeAttributes();
        Instances data = new Instances("data", infos, 10);
//        data.setClassIndex(infos.size()-1);
        return data;
    }


    public static class TM {
        public GustosBotLogicParams args;
        public TradesStatsShort result;
        public Metrica metrica;
    }

    public static double exam(GustosBotLogicParams params, int n){
        BarInterval barInterval = BarInterval.ONE_MIN;
        String logic = "gustos2";
        ArrayList<Pair<ZonedDateTime,ZonedDateTime>> aa = new ArrayList<>();
        ZonedDateTime t1,t2;
        if (n==0) {
            t1 = ZonedDateTime.of(2018, 3, 21, 0, 0, 0, 0, ZoneId.systemDefault());
            t2 = ZonedDateTime.of(2018, 4, 15, 0, 0, 0, 0, ZoneId.systemDefault());
        } else {
            t1 = ZonedDateTime.of(2018, 4, 15, 0, 0, 0, 0, ZoneId.systemDefault());
            t2 = ZonedDateTime.of(2018, 5, 15, 0, 0, 0, 0, ZoneId.systemDefault());
        }
        aa.add(new Pair<>(t1, t2));
        List<XBar> bars = sheet.moments.stream()
                .map(m -> m.bar)
                .collect(Collectors.toList());
        TradeHistory history = new ProfitCalculator().tradeHistory(logic, params, sheet.instrument(), barInterval, sheet.exchange().getFee(), bars, aa, false);
        return history.getEndUsd()/history.getStartUsd();
    }

    public static void main(String[] args) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json", Instrument.getBTC_USDT());

        String path = "gustoslogic2.properties.results";
        TM[] initData = new Gson().fromJson(FileUtils.readFileToString(new File(path)),TM[].class);
        initFields();
        Instances setTrain = initDataSet();
        Instances setExam = initDataSet();
        Instances setExam2 = initDataSet();
        for (int j = initData.length*9/10;j<initData.length;j++){
            TM tm = initData[j];
            double[] instance = new double[setTrain.numAttributes()];
            int i = fieldsToData(instance,tm.args);
            instance[i++] = Math.max(-1,tm.metrica.getValue());
            instance[i++] = Math.max(-1,tm.result.getPearson());
            instance[i++] = Math.max(-1,tm.metrica.get("fine_sma10"));
            instance[i++] = Math.max(-1,tm.result.getProfit());
            instance[i++] = Math.max(-1,tm.result.getTrades());
            double e = exam(tm.args,0);
            instance[i] = e >1.07?1.0:0;
            Instance inst = new DenseInstance(e/1.07, instance);
            setExam.add(inst);

            instance = instance.clone();
            e = exam(tm.args,1);
            instance[i] = e >1.07?1.0:0;
            inst = new DenseInstance(e/1.07, instance);
            setExam2.add(inst);

            instance = instance.clone();
            double profit = tm.result.getProfit();
            instance[i] = profit >4.2?1.0:0;

            inst = new DenseInstance(profit/4.2, instance);
            setTrain.add(inst);
        }
        Exporter.string2file("d:/tetrislibs/wekadata/train.arff",setTrain.toString());
        Exporter.string2file("d:/tetrislibs/wekadata/exam.arff",setExam.toString());
        Exporter.string2file("d:/tetrislibs/wekadata/exam2.arff",setExam2.toString());


    }

}

