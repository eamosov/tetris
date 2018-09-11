package ru.gustos.trading.global;

import ru.gustos.trading.ml.J48AttributeFilter;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

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

    private ArrayList<PLHistory.CriticalMoment> makeGoodBadMoments() {
        ArrayList<PLHistory.CriticalMoment> moments = new ArrayList<>();
        ArrayList<PLHistory.CriticalMoment> tmpmoments;
        double limit = manager.limit();
        tmpmoments = manager.calc.gustosProfit.getCriticalBuyMoments(limit, true, false);
        while (tmpmoments.size() > manager.config.goodMoments) tmpmoments.remove(0);
        moments.addAll(tmpmoments);
        tmpmoments = manager.calc.gustosProfit.getCriticalBuyMoments(limit, false, true);
        while (tmpmoments.size() > manager.config.badMoments) tmpmoments.remove(0);
        moments.addAll(tmpmoments);
        moments.sort(Comparator.comparingLong(c -> c.timeBuy));
        return moments;
    }

    private Instances makeGoodBadSet(boolean buy, int futureAttribute, long endtime, int level) {
        Instances set1;
        int period = buy ? manager.config.learnIntervalBuy : manager.config.learnIntervalSell;
        HashSet<String> ignore = manager.ignore(buy);
        ArrayList<PLHistory.CriticalMoment> moments;
        set1 = data.helper.makeEmptySet(ignore, null, futureAttribute, level);
        moments = makeGoodBadMoments();
        prevLastMoment = moments.get(moments.size() - 1).timeBuy;

        for (int j = 0; j < moments.size(); j++) {
            PLHistory.CriticalMoment m = moments.get(j);
            int index = data.getBarIndex(m.time(buy));
            if (index > manager.calcAllFrom) {
                int fromIndex = Math.max(manager.calcAllFrom, index - period);

//                if (fromIndex<maxTrainIndex) fromIndex = maxTrainIndex;

                int toIndex = Math.min(manager.calc.targetCalcedTo, index + period);
                if (toIndex > maxTrainIndex)
                    maxTrainIndex = toIndex;
                Instances settemp = data.helper.makeSet(data.data, ignore, null, fromIndex, toIndex, endtime, futureAttribute, level);
                set1.addAll(settemp);
            }
        }
        return set1;
    }


    void makeGoodBadModel(FilterMomentsModel model, int calcIndex, long endtime, int level) throws Exception {
//        for (int i = 0; i < data.helper.futureAttributes(); i++) {
        int i = 0;
        boolean full = true;
        boolean buy = i == 0;
        if (data.helper.futureAttributes() == 0) {
            model.full = false;
            System.out.println("no targets");
            return;
        }
        Instances set1 = makeGoodBadSet(buy, i, endtime, level);
        if (set1.numDistinctValues(set1.classIndex()) == 1) {
            model.full = false;
            System.out.println("no distinct values!");
        }else if (set1.size() < 10) {
            model.full = false;
            System.out.println(String.format("use simple model! instrument: %s, attribute: %d, size: %d", data.instrument, i, set1.size()));
        } else {
            model.full = full;
            int trees = buy ? manager.config.treesBuy : manager.config.treesSell;
            int kValue = Math.min(buy ? manager.config.kValueBuy : manager.config.kValueSell, set1.numAttributes() - 1);

            attFilter = new J48AttributeFilter(3, 0.4);
            attFilter.prepare(set1);
            set1 = attFilter.filter(set1);
            if (set1.numAttributes()<=1){
                model.full = false;
                System.out.println("all attributes removed after filtering");
                return;
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
                    System.out.println(String.format("model kappa: %.3g, classes %d/%d (%.3g/%.3g)", evaluation.kappa(), CalcUtils.countWithValue(set1, set1.numAttributes() - 1, 0), CalcUtils.countWithValue(set1, set1.numAttributes() - 1, 1), CalcUtils.weightWithValue(set1, set1.numAttributes() - 1, 0) / 100, CalcUtils.weightWithValue(set1, set1.numAttributes() - 1, 1) / 100));

                model.classifier = rf;
            } else {
                model.classifier = new AllClassifer();
            }
        }
//        }

    }


    void correctNewModel() {
        if (manager.calc.targetCalcedTo - maxTrainIndex > 10) {
            System.out.println(String.format("checking for correction: %d bars", manager.calc.targetCalcedTo - maxTrainIndex));
            int ind = Math.max(manager.calcAllFrom, maxTrainIndex);
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
        ArrayList<PLHistory.CriticalMoment> moments = makeGoodBadMoments();
        return moments.size() > 0 && moments.get(moments.size() - 1).timeBuy != prevLastMoment;
    }
}


