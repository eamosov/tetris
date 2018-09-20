package ru.gustos.trading.tests;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.global.ExperimentData;
import ru.gustos.trading.global.InstrumentData;
import ru.gustos.trading.global.MomentData;
import ru.gustos.trading.global.PLHistoryAnalyzer;
import weka.classifiers.functions.Logistic;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;

public class ResultsReplayTest {
    static ExperimentData experimentData;


    public static void main(String[] args) throws Exception {
        experimentData = ExperimentData.loadGeneral("d:/tetris/pl/pl1318.out");
        replay();
        System.out.println(experimentData.planalyzer3.profits());
    }

    static void replay() throws Exception {
        PLHistoryAnalyzer anal = experimentData.planalyzer3;
        anal.clearHistories();
        ArrayList<InstrumentData> results = experimentData.data;
        int min = results.stream().mapToInt(d -> d.resultdata.size()).min().getAsInt()-calcAllFrom;
//        ArrayList<String> attributes = new ArrayList<>(experimentData.instruments);
        ArrayList<String> attributes = new ArrayList<>();
        for (int i = 0;i<20;i++)
            attributes.add(""+i);
        attributes.add("result");
        Instances set = Exporter.makeBoolSet(attributes.toArray(new String[0]), 0);
        set.setClassIndex(attributes.size()-1);
        Logistic[] logistics = new Logistic[results.size()];
        for (int i = 0;i<min;i++){
            for (int j = 0;j<results.size();j++){
                String key = experimentData.instruments.get(j);
                InstrumentData d = results.get(j);
                int index = d.resultdata.size() - min + i;
                MomentData m = d.resultdata.get(index);
                XBar bar = experimentData.data.get(j).bar(index);
//                if (d.resulthelper.get(m,"price")!=bar.getClosePrice())
//                    System.out.println("uneq "+d.resulthelper.get(m,"price")+" "+bar.getClosePrice()+" "+i);
                if (i%(60*24)==0) {
                    set.clear();
//                    for (int l = 0;l<60*24*30;l++) {
//                        Instance instance = makeInst20(set, results, min, i - l, j, bar.getBeginTime().toEpochSecond());
//                        if (instance!=null)
//                            set.add(instance);
//                    }
//                    logistics[j] = new Logistic();
//                    logistics[j].buildClassifier(set);
                }
                Instance inst = makeInst20(set,results,min,i,j, Long.MAX_VALUE);

                if (d.buys.get(index) && d.resulthelper.get(m,"goodBuy0",0)>0.5 && (logistics[j]==null || logistics[j].classifyInstance(inst)>0.5))
//                if (d.resulthelper.get(m,"goodBuy0",0)>0.5)
                    anal.get(key).buyMoment(bar.getClosePrice(),bar.getEndTime().toEpochSecond());
//                else if (d.resulthelper.get(m,"_sell",0)>0.5)
                else if (d.sells.get(index))
                    anal.get(key).sellMoment(bar.getClosePrice(),bar.getEndTime().toEpochSecond());
                anal.get(key).minMaxCost(bar.getClosePrice(),bar.getEndTime().toEpochSecond());
            }

        }
    }


    static Instance makeInst(Instances set, ArrayList<InstrumentData> results, int min, int i, int instrument, long time){
        InstrumentData d = results.get(instrument);
        if (d.resultdata.size() - min + i<0) return null;
        double[] vv = new double[set.numAttributes()];
        for (int r = 0;r<vv.length-1;r++) {
            InstrumentData dd = results.get(r);
            int index = dd.resultdata.size() - min + i;
            if (index>=0) {
                MomentData m = dd.resultdata.get(index);
                if (m.whenWillKnow >= time) return null;
                vv[r] = dd.resulthelper.get(m, "goodBuy0");
            }
        }

        int index = d.resultdata.size() - min + i;

        vv[vv.length-1] = d.resulthelper.get(d.resultdata.get(index),"_goodBuy");
        DenseInstance inst = new DenseInstance(1, vv);
        inst.setDataset(set);
        return inst;
    }

    static Instance makeInst20(Instances set, ArrayList<InstrumentData> results, int min, int i, int instrument, long time){
        InstrumentData d = results.get(instrument);
        if (d.resultdata.size() - min + i<0) return null;
        double[] vv = new double[set.numAttributes()];
        for (int r = 0;r<vv.length-1;r++) {
            InstrumentData dd = results.get(instrument);
            int index = dd.resultdata.size() - min + i;
            if (index>=0) {
                MomentData m = dd.resultdata.get(index);
                if (m.whenWillKnow >= time) return null;
                vv[r] = dd.resulthelper.get(m, "goodBuy"+r);
            }
        }

        int index = d.resultdata.size() - min + i;

        vv[vv.length-1] = d.resulthelper.get(d.resultdata.get(index),"_goodBuy");
        DenseInstance inst = new DenseInstance(1, vv);
        inst.setDataset(set);
        return inst;
    }
}
