package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bot.TradesStats;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.Volumes;
import ru.gustos.trading.book.indicators.VecUtils;
import ru.gustos.trading.book.indicators.VolumePikeIndicator;
import ru.gustos.trading.visual.Visualizator;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PikesAnalyzer {
    static Sheet sheet;



    public static ArrayList<Attribute> makeAttributes(){
        ArrayList<Attribute> res = new ArrayList<>();
        res.add(new Attribute("upper"));
        res.add(new Attribute("upper2"));
        res.add(new Attribute("upper3"));
        res.add(new Attribute("lower"));
        res.add(new Attribute("lower2"));
        res.add(new Attribute("toUpper"));
        res.add(new Attribute("toUpper2"));
        res.add(new Attribute("toUpper3"));
        res.add(new Attribute("toLower"));
        res.add(new Attribute("toLower2"));
//        res.add(new Attribute("fromUp1", Arrays.asList("false", "true")));
//        res.add(new Attribute("fromUp2", Arrays.asList("false", "true")));
//        res.add(new Attribute("fromDown1", Arrays.asList("false", "true")));
//        res.add(new Attribute("fromDown2", Arrays.asList("false", "true")));
        res.add(new Attribute("fromUp1"));
        res.add(new Attribute("fromUp2"));
        res.add(new Attribute("fromUp3"));
        res.add(new Attribute("fromDown1"));
        res.add(new Attribute("fromDown2"));

        res.add(new Attribute("profit", Arrays.asList("false", "true")));

//        res.add(new Attribute("metrica"));
        return res;
    }

    public static boolean check(double[] v) {
//        if (v[0]>15) return false;
//        if (v[1]>15) return false;
//        if (v[2]>15) return false;
//        if (v[3]>15) return false;
//        if (v[4]>15) return false;
//        if (v[5]<0 || v[5]>26) return false;
//        if (v[6]<0 || v[6]>37) return false;
//        if (v[7]<0 || v[7]>60) return false;
//        if (v[8]>0 || v[8]<-18) return false;
//        if (v[9]>0 || v[9]<-40) return false;
        return true;
    }



    public static Instances initDataSet(){
        ArrayList<Attribute> infos = makeAttributes();
        Instances data = new Instances("data", infos, 10);
        return data;
    }

    private static DenseInstance addInstance(Instances set, double[] v) throws InvocationTargetException, IllegalAccessException {
        DenseInstance instance = new DenseInstance(1, v);
        set.add(instance);
        return instance;
    }

    public static void main(String[] args) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json",new Instrument("BNB", "USDT"));
//        List<double[]> data = ((VolumePikeIndicator) sheet.getLib().get(12)).mldata;
//        Instances set = initDataSet();
//        for (int i =0;i<data.size();i++) {
//            double[] v = data.get(i);
//            if (check(v))
//                addInstance(set, v);
//        }
//        MlUtils.normalizeData(set);
//        LogisticImprover imp = new LogisticImprover(set,4);
//        imp.doIt();
//        set = imp.prepare();
//        System.out.println(imp.bestKappa+" "+Arrays.toString(imp.pows));
        PikesPlayer.play(sheet);

    }

}

