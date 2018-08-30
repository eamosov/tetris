package ru.gustos.trading.global;

import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import static ru.gustos.trading.global.DecisionModel.MAIN;

public class DecisionModels{
    DecisionManager manager;
    DecisionModel model;
    DecisionModel newModel = null;

    long prevLastMoment = 0;
    int maxTrainIndex;

    public double kappas;
    public int kappascnt;

    InstrumentData data;

    public DecisionModels(DecisionManager manager){
        this.manager = manager;
        data = manager.data;
        model = new DecisionModel();

    }

    public void checkNeedRenew(boolean thread) {
        if (manager.calcIndex() >= manager.calcModelFrom && manager.calcIndex() - model.index >= 60 && manager.calcIndex() < manager.dontRenewAfter) {

            ArrayList<PLHistory.CriticalMoment> moments = makeGoodBadMoments();
            if (moments.size() > 0 && moments.get(moments.size() - 1).timeBuy != prevLastMoment) {
                prevLastMoment = moments.get(moments.size() - 1).timeBuy;
                renewModel(thread);
            }
        }
    }

    private void renewModel(boolean thread) {
        if (manager.LOGS)
            System.out.println("renew model on " + data.instrument + ", day " + (manager.calcIndex() / (60 * 24)));
        final int currentIndex = manager.calcIndex();
        final DecisionModel model = new DecisionModel(this.model, manager.calcIndex());
        if (thread)
            new Thread(() -> doRenewModel(model, currentIndex)).start();
        else
            doRenewModel(model, currentIndex);

    }

    private void doRenewModel(DecisionModel model, int calcIndex) {

        if (calcIndex >= manager.calcModelFrom)
            prepareModel(model, calcIndex);

        newModel = model;
    }

    void checkTakeNewModel() {
        if (newModel != null) {
            model = newModel;
            newModel = null;
            manager.plhistoryClassifiedBuy.newModel(data.bar(manager.calcIndex() - 1).getBeginTime());
        }
    }


    private void prepareModel(DecisionModel model, int calcIndex) {
        long endtime = data.bar(calcIndex - 1).getEndTime().toEpochSecond();
        try {
            model.clear();
            makeGoodBadModel(model, calcIndex, endtime, 9);
            correctNewModel(model);
            if (data.buydata!=null)
                makeBuySellModel(model, calcIndex, endtime, 9);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        set1 = data.helper.makeEmptySet(ignore, futureAttribute, level);
        moments = makeGoodBadMoments();

        for (int j = 0; j < moments.size(); j++) {
            PLHistory.CriticalMoment m = moments.get(j);
            int index = data.getBarIndex(m.time(buy));
            if (index > manager.calcAllFrom) {
                int fromIndex = Math.max(manager.calcAllFrom, index - period);
                int toIndex = Math.min(manager.calc.targetCalcedTo, index + period);
                if (toIndex > maxTrainIndex)
                    maxTrainIndex = toIndex;
                Instances settemp = data.helper.makeSet(data.data, ignore, fromIndex, toIndex, endtime, futureAttribute, level);
                set1.addAll(settemp);
            }
        }
        return set1;
    }


    private void makeBuySellModel(DecisionModel model, int calcIndex, long endtime, int level) throws Exception {
        for (int i = 0; i < data.buyhelper.futureAttributes(); i++) {
//            Instances set = buyhelper.makeEmptySet(null, i, level);
            Instances set = data.buyhelper.makeSet(data.buydata(), null, calcIndex - 60 * 24 * 14, calcIndex - 60 * 12, endtime, i, level);

            RandomForestWithExam rf = new RandomForestWithExam();
            rf.setNumExecutionSlots(manager.cpus);
            rf.setNumIterations(100);
            rf.setMaxDepth(15);
            rf.setSeed(calcIndex + (int) System.currentTimeMillis());

            rf.buildClassifier(set);
            Evaluation evaluation = new Evaluation(set);
            evaluation.evaluateModel(rf, set);
            if (manager.LOGS)
                System.out.println(String.format("buy sell model kappa: %.3g, classes %d/%d (%.3g/%.3g)",
                        evaluation.kappa(),
                        CalcUtils.countWithValue(set, set.numAttributes() - 1, 0),
                        CalcUtils.countWithValue(set, set.numAttributes() - 1, 1),
                        CalcUtils.weightWithValue(set, set.numAttributes() - 1, 0) / 100,
                        CalcUtils.weightWithValue(set, set.numAttributes() - 1, 1) / 100));
            model.models2.get(MAIN).add(rf);

        }

    }

    private void makeGoodBadModel(DecisionModel model, int calcIndex, long endtime, int level) throws Exception {
        for (int i = 0; i < data.helper.futureAttributes(); i++) {
            boolean full = true;
            boolean buy = i == 0;
            Instances set1 = makeGoodBadSet(buy, i, endtime, level);
            if (set1.size() < 10) {
                model.full = false;
                System.out.println(String.format("use simple model! instrument: %s, attribute: %d, size: %d", data.instrument, i, set1.size()));
            } else {
                model.full = full;
                if (set1.numDistinctValues(set1.classIndex()) == 1) System.out.println("no distinct values!");
                int trees = buy ? manager.config.treesBuy : manager.config.treesSell;
                int kValue = Math.min(buy ? manager.config.kValueBuy : manager.config.kValueSell, set1.numAttributes() - 1);

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

                    model.models.get(MAIN).add(rf);
                } else {
                    model.models.get(MAIN).add(new AllClassifer());
                }
            }
        }

    }



    private void correctNewModel(DecisionModel model) {
        if (manager.calc.targetCalcedTo - maxTrainIndex > 10) {
            System.out.println(String.format("checking for correction: %d bars", manager.calc.targetCalcedTo - maxTrainIndex));
            int ind = Math.max(manager.calcAllFrom, maxTrainIndex);
            while (ind < manager.calc.targetCalcedTo) {
                MomentData mldata = data.data.get(ind);

                correctModelForMoment(mldata);

                ind++;
            }
        }
    }

    void correctModelForMoment(MomentData mldata){
        if (hasModel() && data.helper.get(mldata, "gustosBuy") > 0.5) {
            Object classifier = model.models.get(MAIN).get(0);
            if (classifier instanceof RandomForestWithExam) {
                RandomForestWithExam rf = (RandomForestWithExam) classifier;
                try {
                    rf.updateCorrectness(data.helper.prepareInstance(mldata, manager.ignore(true), 0, 9), data.helper.get(mldata, "_goodBuy") > 0.5);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public boolean hasModel() {
        return model != null && model.models.get(MAIN).size() > 0;
    }
}
