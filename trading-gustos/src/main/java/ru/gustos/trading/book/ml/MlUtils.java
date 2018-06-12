package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.VecUtils;
import weka.core.Instance;
import weka.core.Instances;

public class MlUtils{

    public static void applyMinMax(Instances ii, double[] min, double[] max){
        for (int i = 0;i<ii.size();i++){
            Instance inst = ii.get(i);
            for (int j= 0;j<min.length;j++) if (ii.attribute(j).isNumeric()){
                double v = inst.value(j);
                v = (v-min[j])/(max[j]-min[j])*2-1;
                inst.setValue(j,v);
            }
        }
    }

    public static void center(Instances ii){

    }

    public static Pair<double[],double[]> getMinMax(Instances set){
        double[] dd = set.get(0).toDoubleArray();
        double[] min = dd.clone();
        double[] max = dd.clone();
        for (int i = 1;i<set.size();i++){
            dd = set.get(i).toDoubleArray();
            VecUtils.minMax(dd,min,max);
        }
        return new Pair<>(min,max);
    }


    public static void normalizeData(Instances set){
        Pair<double[], double[]> minMax = getMinMax(set);
        applyMinMax(set,minMax.getFirst(),minMax.getSecond());
    }


}

