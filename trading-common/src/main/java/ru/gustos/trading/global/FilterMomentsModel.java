package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.ml.J48AttributeFilter;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;

public class FilterMomentsModel {


    int index;
    boolean full = false;
    Classifier classifier;
    DecisionManager manager;
    InstrumentData data;

    int maxTrainIndex;

    public double kappas;
    public int kappascnt;
    long prevLastMoment = 0;
    J48AttributeFilter attFilter;


    public FilterMomentsModel(DecisionManager manager) {
        this.manager = manager;
        data = manager.data;
        index = manager.calcIndex();
    }

    public void clear() {
        full = false;
        classifier = null;
    }

    public int index(){
        return index;
    }


    private ArrayList<PLHistory.CriticalMoment> makeMoments(){
//        return manager.calc.gustosProfit.getMostCriticalBuyMoments(60*60*24*90);
        return manager.calc.gustosProfit.makeGoodBadMoments(manager.limit(),Long.MAX_VALUE,manager.config.goodMoments,manager.config.badMoments);
    }

    int goodfound, badfound;
    int daysLength;
    private Instances makeGoodBadSet(boolean buy, int futureAttribute, long endtime, int level) {
        Instances set1;
        int period = buy ? manager.config.learnIntervalBuy : manager.config.learnIntervalSell;
        HashSet<String> ignore = manager.ignore(buy);
        ArrayList<PLHistory.CriticalMoment> moments;
        set1 = data.helper.makeEmptySet(ignore, null, futureAttribute, level);
        moments = makeMoments();
        goodfound = manager.calc.gustosProfit.goodfound;
        badfound = manager.calc.gustosProfit.badfound;
        prevLastMoment = moments.get(moments.size() - 1).timeBuy;
        daysLength = (int)((prevLastMoment-moments.get(0).timeBuy)/(60*60*24));
        for (int j = 0; j < moments.size(); j++) {
            PLHistory.CriticalMoment m = moments.get(j);
            int index = data.getBarIndex(m.time(buy));
            if (index > calcAllFrom) {
                int fromIndex = Math.max(calcAllFrom, index - period);
//                if (fromIndex<maxTrainIndex) fromIndex = maxTrainIndex;

                int toIndex = Math.min(manager.calc.targetCalcedTo, index + period);
                if (toIndex > maxTrainIndex)
                    maxTrainIndex = toIndex;
                Instances settemp = data.helper.makeSet(data.data, ignore, null, fromIndex, toIndex, endtime, futureAttribute, level);
                set1.addAll(settemp);
            }
        }

//        double badw = CalcUtils.weightWithValue(set1, set1.classIndex(), 0);
//        double goodw = CalcUtils.weightWithValue(set1, set1.classIndex(), 1);
//        CalcUtils.mulWeightsWhenValue(set1,2*goodw/badw,set1.classIndex(), 0);
// 134 192 / 119 157 / 103 145 (209 293),
// 117 160 / 106 144 / (171 242)
// 229 293 / 224 289 (242 310)
        return set1;
    }


    void makeGoodBadModel(int calcIndex, long endtime, int level) throws Exception {
//        for (int i = 0; i < data.helper.futureAttributes(); i++) {
        int i = 0;
        boolean full = true;
        boolean buy = i == 0;
        if (data.helper.futureAttributes() == 0) {
            this.full = false;
            System.out.println("no targets");
            return;
        }
        Instances set1 = makeGoodBadSet(buy, i, endtime, level);
        int modelDays = daysLength;
        int goodFound = goodfound;
        int badFound = badfound;
        if (set1.numDistinctValues(set1.classIndex()) == 1) {
            this.full = false;
            System.out.println("no distinct values!");
        }else if (set1.size() < 10) {
            this.full = false;
            System.out.println(String.format("use simple model! instrument: %s, attribute: %d, size: %d", data.instrument, i, set1.size()));
        } else {
            this.full = full;
            int trees = buy ? manager.config.treesBuy : manager.config.treesSell;
            int kValue = Math.min(buy ? manager.config.kValueBuy : manager.config.kValueSell, set1.numAttributes() - 1);

            if (manager.export!=null) {
                attFilter = null;
                manager.export.add(new Pair<>(set1, data.helper.makeEmptySet(manager.ignoreBuy, attFilter, 0, 9)));
            } else {

                attFilter = new J48AttributeFilter(3, 0.4);
                if (manager.calc.oldInstr()) {
                    Instances settemp = data.helper.makeSet(data.data, manager.ignoreBuy, null, Math.max(calcAllFrom,calcIndex-60*24*30), calcIndex, endtime, 0, level);
                    attFilter.prepare(settemp,false);
                } else
//                    attFilter.prepare(set1,true);
                    attFilter.prepare(set1,true);
                set1 = attFilter.filter(set1);
                if (set1.numAttributes()<=1){
                    this.full = false;
                    System.out.println("all attributes removed after filtering");
                    return;
                }
            }


            RandomForestWithExam rf = new RandomForestWithExam();
            rf.setNumExecutionSlots(manager.cpus);
            rf.setNumIterations(trees);
            rf.setMaxDepth(manager.config.maxDepth);
            rf.setNumFeatures(kValue);
            rf.setSeed(calcIndex + (int) System.currentTimeMillis());


            if (buy) {
//                        RandomForest f = CalcUtils.makeSmileRandomForest(set1, 0, trees, kValue);
                rf.buildClassifier(set1);
                Evaluation evaluation = new Evaluation(set1);
                evaluation.evaluateModel(rf, set1);
                kappas += evaluation.kappa();
                kappascnt++;
                if (manager.LOGS)
                    System.out.println(String.format("model kappa: %.3g, classes %d/%d (%.3g/%.3g), days %d, bad %d, good %d", evaluation.kappa(),
                            CalcUtils.countWithValue(set1, set1.classIndex(), 0),
                            CalcUtils.countWithValue(set1, set1.classIndex(), 1),
                            CalcUtils.weightWithValue(set1, set1.classIndex(), 0) / 100,
                            CalcUtils.weightWithValue(set1, set1.classIndex(), 1) / 100,
                            modelDays, badFound, goodFound));

                classifier = rf;
            } else {
                classifier = new AllClassifer();
            }
        }
//        }

    }


    void correctNewModel() {
        if (manager.calc.targetCalcedTo - maxTrainIndex > 10) {
            System.out.println(String.format("checking for correction: %d bars", manager.calc.targetCalcedTo - maxTrainIndex));
            int ind = Math.max(calcAllFrom, maxTrainIndex);
            while (ind < manager.calc.targetCalcedTo) {
                correctModelForMoment(ind);
                ind++;
            }
        }
    }

    void correctModelForMoment(int index) {
        if (classifier != null && data.buys.get(index)) {
            if (classifier instanceof RandomForestWithExam) {
                RandomForestWithExam rf = (RandomForestWithExam) classifier;
                try {
                    MomentData mldata = data.data.get(index);
                    Instance instance = data.helper.prepareInstance(mldata, manager.ignore(true), attFilter, 0, 9);
                    rf.updateCorrectness(instance, data.helper.get(mldata, "_goodBuy") > 0.5);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public boolean canRenewModel() {
        ArrayList<PLHistory.CriticalMoment> moments = makeMoments();
        return moments.size() > 0 && moments.get(moments.size() - 1).timeBuy != prevLastMoment;
    }
}


