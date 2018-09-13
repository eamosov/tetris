package ru.gustos.trading.global;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;

public class SellNowModel{
    DecisionManager manager;
    Classifier classifier;

    public SellNowModel(DecisionManager manager) {
        this.manager = manager;
    }

    private ArrayList<PLHistory.CriticalMoment> makeGoodBadMoments() {
        ArrayList<PLHistory.CriticalMoment> moments = new ArrayList<>();
        ArrayList<PLHistory.CriticalMoment> tmpmoments;
        double limit = manager.limit();
        tmpmoments = manager.calc.gustosProfit.getCriticalBuyMoments(limit, true, false);
//        while (tmpmoments.size() > manager.config.goodMoments) tmpmoments.remove(0);
        while (tmpmoments.size() > 20) tmpmoments.remove(0);
        moments.addAll(tmpmoments);
        tmpmoments = manager.calc.gustosProfit.getCriticalBuyMoments(limit, false, true);
//        while (tmpmoments.size() > manager.config.badMoments) tmpmoments.remove(0);
        while (tmpmoments.size() > 20) tmpmoments.remove(0);
        moments.addAll(tmpmoments);
        moments.sort(Comparator.comparingLong(c -> c.timeBuy));
        return moments;
    }

    private Instances makeGoodBadSet(int futureAttribute, long endtime, int level) {
        Instances set1;
        int period = manager.config.learnIntervalBuy;
        HashSet<String> ignore = manager.ignore(true);
        ArrayList<PLHistory.CriticalMoment> moments;
        InstrumentData data = manager.data;
        set1 = data.helper.makeEmptySet(ignore, null, futureAttribute, level);
        moments = makeGoodBadMoments();

        for (int j = 0; j < moments.size(); j++) {
            PLHistory.CriticalMoment m = moments.get(j);
            int index = data.getBarIndex(m.timeBuy);
            if (index > manager.calcAllFrom) {
                int fromIndex = Math.max(manager.calcAllFrom, index);

//                if (fromIndex<maxTrainIndex) fromIndex = maxTrainIndex;

                int toIndex = Math.min(manager.calc.targetCalcedTo, data.getBarIndex(m.timeSell));
                Instances settemp = data.helper.makeSet(data.data, ignore, null, fromIndex, toIndex, endtime, futureAttribute, level);
                set1.addAll(settemp);
            }
        }
        return set1;
    }


    public void prepare(int calcIndex, long endtime) throws Exception {
//        Instances set = manager.data.helper.makeSet(manager.data.data, null, null, Math.max(calcAllFrom,calcIndex-30*24*60), calcIndex, endtime, 1, 9);
        classifier = null;
        if (true) return;
        if (manager.data.helper.futureAttributes()==0) return;
        Instances set = makeGoodBadSet(1, endtime, 9);
        if (set.numDistinctValues(set.classIndex()) > 1  && set.size() > 100) {

            CalcUtils.resetWeights(set);
            RandomForestWithExam rf = new RandomForestWithExam();
            rf.setNumExecutionSlots(manager.cpus);
            rf.setNumIterations(200);
            rf.setMaxDepth(10);
            rf.setSeed(calcIndex + (int) System.currentTimeMillis());
            rf.buildClassifier(set);
            Evaluation evaluation = new Evaluation(set);
            evaluation.evaluateModel(rf, set);
            if (manager.LOGS)
                System.out.println(String.format("sell now model kappa: %.3g, classes %d/%d (%.3g/%.3g)", evaluation.kappa(),
                        CalcUtils.countWithValue(set, set.classIndex(), 0),
                        CalcUtils.countWithValue(set, set.classIndex(), 1),
                        CalcUtils.weightWithValue(set, set.classIndex(), 0) / 100,
                        CalcUtils.weightWithValue(set, set.classIndex(), 1) / 100));
            classifier = rf;
        }
    }
}
