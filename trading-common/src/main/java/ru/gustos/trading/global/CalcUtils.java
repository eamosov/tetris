package ru.gustos.trading.global;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.GustosAverageRecurrent;
import smile.classification.RandomForest;
import smile.data.Attribute;
import smile.data.NominalAttribute;
import smile.data.NumericAttribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.logging.LogManager;

public class CalcUtils {

    public static boolean gustosSell(InstrumentData data, int index, GustosAverageRecurrent sellGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar pbar = data.bar(index - 1);
        XBar bar = data.bar(index);
        double sma = sellGar.value();
        double sd = sellGar.sd();
//        double p = sma - sd * values.gustosParams.sellDiv()*0.1;
        return /*bar.getMaxPrice() >= p && */bar.getClosePrice() > sma + sd * params.sellBoundDiv() * 0.1 && pbar.getClosePrice() >= bar.getMinPrice();
    }

    public static boolean gustosBuy(InstrumentData data, int index, GustosAverageRecurrent buyGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar pbar = data.bar(index - 1);
        XBar bar = data.bar(index);

        double p = buyGar.pvalue() - buyGar.psd() * params.buyDiv() * 0.1;
        return bar.getMinPrice() <= p && bar.getMaxPrice() >= p && bar.getClosePrice() < buyGar.value() - buyGar.sd() * params.buyBoundDiv() * 0.1 && pbar.getClosePrice() < bar.getMaxPrice();
    }

    public static boolean gustosSellEasy(InstrumentData data, int index, GustosAverageRecurrent sellGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar bar = data.bar(index);
        double sma = sellGar.value();
        double sd = sellGar.sd();
        return bar.getClosePrice() > sma + sd * params.sellBoundDiv() * 0.1;
    }

    public static boolean gustosBuyEasy(InstrumentData data, int index, GustosAverageRecurrent buyGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar bar = data.bar(index);

        return bar.getClosePrice() < buyGar.value() - buyGar.sd() * params.buyBoundDiv() * 0.1;
    }

    public static Attribute[] smileAttributes(Instances data) {
        Attribute[] res = new Attribute[data.numAttributes() - 1];
        for (int i = 0; i < res.length; i++) {
            weka.core.Attribute a = data.attribute(i);
            if (a.isNumeric())
                res[i] = new NumericAttribute(a.name());
            else
                res[i] = new NominalAttribute(a.name());
        }
        return res;
    }

    public static double[][] smileData(Instances data) {
        double[][] res = new double[data.size()][data.numAttributes() - 1];
        for (int i = 0; i < res.length; i++)
            for (int j = 0; j < res[i].length; j++)
                res[i][j] = data.get(i).value(j);
        return res;
    }

    public static int[] smileClasses(Instances data) {
        int[] res = new int[data.size()];
        for (int i = 0; i < data.size(); i++)
            res[i] = (int) data.get(i).classValue();
        return res;
    }

    public static RandomForest makeSmileRandomForest(Instances data, int cpus, int trees, int kValue) {
        if (kValue == 0)
            kValue = (int) Utils.log2((double) (data.numAttributes() - 1)) + 1;
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RandomForest.class)).setLevel(Level.WARN);
        return new RandomForest(smileAttributes(data), smileData(data), smileClasses(data), trees, kValue);

    }

    public static double[] smileInstance(Instance instance) {
        double[] res = new double[instance.numAttributes() - 1];
        for (int i = 0; i < res.length; i++)
            res[i] = instance.value(i);
        return res;
    }

    public static void setWeights(Instances set, double w) {
        for (int i = 0; i < set.size(); i++)
            set.get(i).setWeight(w);
    }

    public static void mulWeightsWhenValue(Instances set, double k, int index, double v) {
        for (int i = 0; i < set.size(); i++)
            if (set.get(i).value(index)==v)
                set.get(i).setWeight(set.get(i).weight()*k);
    }

    public static int countWithValue(Instances set, int index, double v) {
        int res = 0;
        for (int i = 0; i < set.size(); i++)
            if (set.get(i).value(index) == v) res++;
        return res;
    }

    public static double weightWithValue(Instances set, int index, double v) {
        double res = 0;
        for (int i = 0; i < set.size(); i++)
            if (set.get(i).value(index) == v) res+=set.get(i).weight();
        return res;
    }
}
