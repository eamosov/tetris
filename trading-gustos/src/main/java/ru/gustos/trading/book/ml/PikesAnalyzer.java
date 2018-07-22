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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PikesAnalyzer {
    static Sheet sheet;






    public static Instances initDataSet(){
        ArrayList<Attribute> infos = PikesPlayer.makeAttributes();
        Instances data = new Instances("data", infos, 10);
        return data;
    }

    private static DenseInstance addInstance(Instances set, double[] v) throws InvocationTargetException, IllegalAccessException {
        DenseInstance instance = new DenseInstance(1, v);
        set.add(instance);
        return instance;
    }

    public static void main(String[] args) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json", new Instrument("BTC", "USDT"));
        PikesPlayer.play(sheet);
//        MlUtils.normalizeData(set);
//        LogisticImprover imp = new LogisticImprover(set,4);
//        imp.doIt();
//        set = imp.prepare();
//        System.out.println(imp.bestKappa+" "+Arrays.toString(imp.pows));

    }

}

