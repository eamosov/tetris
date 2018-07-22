package ru.gustos.trading.global;

import kotlin.Pair;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.*;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;

public class StandardInstrumentCalc {

    InstrumentData data;
    MomentDataHelper helper;
    public PLHistory plhistory1;
    public PLHistory plhistory2;
    public PLHistory plhistory3;

    int calcIndex = 0;

    Volumes volumes;
    RecurrentValues values;
    PLHistory gustosProfit;

    class Model {
        double sd = 0.02;
        int index;
        ArrayList<Classifier> models = new ArrayList<>();
        ArrayList<Classifier> simpleModels = new ArrayList<>();
        GustosLogicsOptimizedHistory optimized = new GustosLogicsOptimizedHistory(data);

        public Model(){}
        public Model(Model model, int index) {
            this.index = index;
            sd = model.sd;
            optimized = model.optimized.clone();
        }
    }

    Model model;
    Model newModel = null;

    public StandardInstrumentCalc(InstrumentData data) {
        this.helper = data.helper;
        this.data = data;
        plhistory1 = new PLHistory(data.instrument.toString(), data.global.planalyzer1);
        plhistory2 = new PLHistory(data.instrument.toString(), data.global.planalyzer2);
        plhistory3 = new PLHistory(data.instrument.toString(), data.global.planalyzer3);
        model = new Model();
        initForCalc();

    }

    private void initForCalc() {
        gustosProfit = new PLHistory(null, null);
        volumes = new Volumes(data, false, false);
        values = new RecurrentValues(data);
    }


    public void precalc() {
        int pw = 0;
        for (int i = 0; i < data.size(); i++) {
            int w = i / (60 * 24);
            if (w != pw) {
                pw = w;
//                values.setGustosParams(new GustosLogicOptimizator(data, Math.max(0, i - 60 * 24 * 7 * 3), i).optimize(values.gustosParams).getFirst());
//                paramsCache.add(values.gustosParams);
            }
            volumes.calc(i);
            values.feed(i);
            calc(i, true);
        }
        initForCalc();
    }

    public static final int calcAllFrom = 60 * 24 * 7 * 2;
    public static final int calcSimpleFrom = 60 * 24 * 16;
    public static final int calcModelFrom = 60 * 24 * 7 * 6;
    public static final int optimizePeriod = 60 * 24 * 7 * 2;

    int optimizeSkip = 0;

    public void calcTo(long time) {
        if (calcIndex >= data.size()) return;

        checkNeedRenew(false);
        checkTakeNewModel();

        while (calcIndex < data.size() && data.bar(calcIndex).getEndTime().toEpochSecond() <= time)
            doNext();

    }

    public void addBar(XBaseBar bar){
        data.addBar(bar);
        while (calcIndex < data.size())
            doNext();

    }

    private void doNext() {
        volumes.calc(calcIndex);
        values.feed(calcIndex);
        model.optimized.feed(calcIndex);
        if (calcIndex >= calcAllFrom)
            calc(calcIndex, false);
        calcIndex++;
    }

    public void checkNeedRenew(boolean thread){
        if (calcIndex >= calcAllFrom && calcIndex-model.index>=60*12)
            renewModel(thread);
    }

    private void renewModel(boolean thread) {
        final int currentIndex = calcIndex;
        final Model model = new Model(this.model, calcIndex);
        if (thread)
            new Thread(() -> doRenewModel(model, currentIndex)).start();
        else {
            doRenewModel(model,currentIndex);
        }
    }

    private void doRenewModel(Model model, int calcIndex) {
        model.sd = calcBestSd(model.sd);

//        if (calcIndex >= calcSimpleFrom)
//            prepareSimpleModel(model,calcIndex);

        if (calcIndex >= calcModelFrom)
            prepareModel(model,calcIndex);

        if (calcIndex >= optimizePeriod) {
            optimizeSkip--;
            if (optimizeSkip < 0) {
//                model.optimized.add(calcIndex, optimizePeriod);
                optimizeSkip = 3;
            }
        }


        newModel = model;
    }

    private void checkTakeNewModel() {
        if (newModel != null) {
            model = newModel;
            newModel = null;
            while (model.optimized.needIndex() < calcIndex)
                model.optimized.feed(model.optimized.needIndex());
        }
    }


    private void prepareModel(Model model, int calcIndex) {
        int period = 60 * 3;
        long endtime = data.bar(calcIndex - 1).getEndTime().toEpochSecond();
//        int period = 60 * 24 * 2;
        try {
//            makeSimpleModel(models,period,endtime);
            makeGoodBadModel(model.models, calcIndex, period, endtime, true, true, 9);
//            makeGoodBadModel(models2,period,endtime, false, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareSimpleModel(Model model, int calcIndex) {
        long endtime = data.bar(calcIndex - 1).getEndTime().toEpochSecond();
        try {

            model.simpleModels.clear();
            for (int i = 0; i < helper.futureAttributes(); i++)
                model.simpleModels.add(makeSimpleModel(60 * 2, endtime, i, 0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ArrayList<Long> makeGoodBadMoments(boolean good, boolean bad, boolean buyTime) {
        ArrayList<Long> moments = new ArrayList<>();
        if (good) {
            double limit = 0.03;
            ArrayList<Long> tmpmoments;
            do {
                limit *= 0.9;
                tmpmoments = plhistory1.getCriticalBuyMoments(limit, true, false, buyTime);
            } while (tmpmoments.size() < 7 && limit >= 0.001);
            while (tmpmoments.size() > 20) tmpmoments.remove(0);
            moments.addAll(tmpmoments);
        }

        if (bad) {
            double limit = 0.03;
            ArrayList<Long> tmpmoments;
            do {
                limit *= 0.9;
                tmpmoments = plhistory1.getCriticalBuyMoments(limit, false, true, buyTime);
            } while (tmpmoments.size() < 7 && limit >= 0.001);
            while (tmpmoments.size() > 20) tmpmoments.remove(0);
            moments.addAll(tmpmoments);
            moments.sort(Long::compare);
        }
        return moments;
    }

    static double[][] impurityDecreaseSum;

    private void makeGoodBadModel(ArrayList<Classifier> models, int calcIndex, int period, long endtime, boolean good, boolean bad, int level) {
        try {
            models.clear();
            if (impurityDecreaseSum == null)
                impurityDecreaseSum = new double[helper.futureAttributes()][];
            for (int i = 0; i < helper.futureAttributes(); i++) {
//                if (i == 1) {
//                    models.add(makeSimpleModel(models, period, endtime, i));
//                    continue;
//                }

                Instances set1 = helper.makeEmptySet(i, level);
                ArrayList<Long> moments = makeGoodBadMoments(good, bad, i == 0);
                for (int j = 0; j < moments.size(); j++) {
                    long time = moments.get(j);
                    int index = data.getBarIndex(time);
                    if (index > calcAllFrom) {
                        Instances settemp = helper.makeSet(data.bars.data, Math.max(calcAllFrom, index - period), Math.min(calcIndex, index + period), endtime, i, level);
                        set1.addAll(settemp);
                    }
                }
                if (set1.size() < 10)
                    models.add(makeSimpleModel(period, endtime, i, level));
                else {
                    if (set1.numDistinctValues(set1.classIndex()) == 1) System.out.println("no distinct values!");
                    RandomForest rf = new RandomForest();
                    rf.setNumExecutionSlots(4);
                    rf.setNumIterations(300);
                    rf.setSeed(calcIndex);
                    rf.setOptions(new String[]{"-U"});
//                    rf.setComputeAttributeImportance(true);

                    //                    CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
//                    CostMatrix costMatrix = new CostMatrix(2);
//                    costMatrix.setElement(0,1,1.5);
//                    costSensitiveClassifier.setClassifier(rf);
//                    costSensitiveClassifier.setCostMatrix(costMatrix);
//                    costSensitiveClassifier.buildClassifier(set1);
//                    models.add(costSensitiveClassifier);
                    rf.buildClassifier(set1);
                    models.add(rf);
//                    double[] impurityDecrease = rf.computeAverageImpurityDecreasePerAttribute(null);
//                    if (impurityDecreaseSum[i] == null)
//                        impurityDecreaseSum[i] = impurityDecrease.clone();
//                    else {
//                        impurityDecreaseSum[i] = VecUtils.add(impurityDecreaseSum[i], impurityDecrease, 1);
//                        helper.printImpurity(set1, impurityDecreaseSum[i], "model " + i + ": ");
//                    }

//                        Logistic l = new Logistic();
//                        l.buildClassifier(set1);
//                        models.add(l);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Classifier makeSimpleModel(int period, long endtime, int attribute, int level) {
        try {
            Instances set1;
            set1 = helper.makeSet(data.bars.data, calcIndex - period, calcIndex, endtime, attribute, level);
            int plus = 0;
            while (set1.size() == 0)
                set1 = helper.makeSet(data.bars.data, calcIndex - period * (1 + (++plus)), calcIndex, endtime, attribute, level);

//            RandomForest rf = new RandomForest();
//            rf.setOptions(new String[]{"-U"});
//            rf.buildClassifier(set1);
//            rf.setNumExecutionSlots(4);
//            return rf;
            Logistic l = new Logistic();
            l.buildClassifier(set1);
            return l;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    private double calcBestSd(double sd) {
        // вычисляем с каким сд у нас будет порядка 20-30 операций, если прям сразу покупать.
        int from = data.getBarIndex(data.bar(calcIndex).getBeginTime().toEpochSecond() - 3600 * 24 * 4);
        while (numberOfTrades(from, calcIndex, sd) > 20)
            sd *= 1.1;
        while (numberOfTrades(from, calcIndex, sd) < 20)
            sd *= 0.9;
        return sd;
    }

    private int numberOfTrades(int from, int to, double sd) {
        SimpleStrategy strategy = new SimpleStrategy(1 + sd, 1 - sd);
        int time = from;
        int cc = 0;
        do {
            Pair<Double, Integer> pp = strategy.calcProfit(data, time);
            time = pp.getSecond();
            if (time < Integer.MAX_VALUE) time++;
            cc++;
        } while (time < to);
        return cc;
    }

    private void calc(int index, boolean asFirst) {
        InstrumentMoment m = data.bars.get(index);
        MomentData mldata = m.mldata;
        double price = m.bar.getClosePrice();
        double maxprice = m.bar.getMaxPrice();
        double minprice = m.bar.getMinPrice();
        boolean gbuy;
        boolean gsell;
        long time = m.bar.getEndTime().toEpochSecond();

        if (asFirst) {
            gbuy = gustosBuy(index);
            gsell = gustosSell(index);
            helper.put(mldata, "gustosBuy", gbuy ? 1.0 : 0, true);
            helper.put(mldata, "gustosSell", gsell ? 1.0 : 0, true);

            helper.put(mldata, "1simple0", 0, true);
            helper.put(mldata, "1simple1", 0, true);

            helper.put(mldata, "toAvgSd", div(price - values.gustosAvg.value(), values.gustosAvg.sd()));
            helper.put(mldata, "sd", values.gustosAvg.sd() / price * 10);
            helper.put(mldata, "toAvgSdMax", div(maxprice - values.gustosAvg.value(), values.gustosAvg.sd()));
            helper.put(mldata, "toAvgSdMin", div(minprice - values.gustosAvg.value(), values.gustosAvg.sd()));
            helper.put(mldata, "toAvgSd2", div(price - values.gustosAvg2.value(), values.gustosAvg2.sd()));
            helper.put(mldata, "sd2", values.gustosAvg2.sd() / price * 10);
            helper.put(mldata, "toAvgSdB", div(price - values.gustosAvgBuy.value(), values.gustosAvgBuy.sd()));
            helper.put(mldata, "sdB", values.gustosAvgBuy.sd() / price * 10);
            helper.put(mldata, "toAvgSdS", div(price - values.gustosAvgSell.value(), values.gustosAvgSell.sd()));
            helper.put(mldata, "sdS", values.gustosAvgSell.sd() / price * 10);

            helper.put(mldata, "d2vol", values.deltaToVolumeShort.value() / values.deltaToVolume.value());
            helper.put(mldata, "mm2vol", values.maxminToVolumeShort.value() / values.maxminToVolume.value());
            helper.put(mldata, "d2vol_n", values.deltaToVolumeShort.value());
            helper.put(mldata, "mm2vol_n", values.maxminToVolumeShort.value());

            helper.put(mldata, "mm", values.maxminShort.value() / values.maxmin.value());


//            helper.put(mldata, "sd_usual1", Math.sqrt(values.sd1.value()) / price * 10);
//            helper.put(mldata, "sd_usual2", Math.sqrt(values.sd2.value()) / price * 10);
//            helper.put(mldata, "sd_usual3", Math.sqrt(values.sd3.value()) / price * 10);
//            helper.put(mldata, "macd1", values.macd1.value());
//            helper.put(mldata, "macd2", values.macd2.value());
//            helper.put(mldata, "pmacd1", values.macd1.value() - values.macd1.pvalue());
//            helper.put(mldata, "pmacd2", values.macd2.value() - values.macd2.pvalue());
//            helper.put(mldata, "rsi1", values.rsi1.value());
//            helper.put(mldata, "rsi2", values.rsi2.value());
//            helper.put(mldata, "rsiv1", values.rsiv1.value());
//            helper.put(mldata, "rsiv2", values.rsiv2.value());
//            helper.put(mldata, "stoh1", values.stoh1.percent());
//            helper.put(mldata, "stoh2", values.stoh2.percent());
//            helper.put(mldata, "change1", values.change1.value());
//            helper.put(mldata, "change2", values.change2.value());
//            helper.put(mldata, "change3", values.change3.value());

            //            helper.put(mldata,"rising",sheet.bar(index-1).getClosePrice() < sheet.bar(index).getMinPrice()?1:0);
//            helper.put(mldata,"falling",sheet.bar(index-1).getClosePrice() >= sheet.bar(index).getMaxPrice()?1:0);

            helper.put(mldata, "toAvg1", ((price - values.ema1.value()) / price));
            helper.put(mldata, "toAvg2", ((price - values.ema2.value()) / price));
//            helper.put(mldata, "toAvg3", ((price - values.ema3.value()) / price));
            helper.put(mldata, "toAvgSdU1", (div(price - values.ema1.value(), values.sd1.value())));
            helper.put(mldata, "toAvgSdU2", (div(price - values.ema2.value(), values.sd2.value())));
//            helper.put(mldata, "toAvgSdU3", (div(price - values.ema3.value(), values.sd3.value())));
            helper.put(mldata, "volumeBurst", values.volumeShort.value() / values.volumeLong.value());
//            helper.put(mldata, "volumeBurstBool", values.volumeShort.value() / values.volumeLong.value() > 3 ? 1 : 0, true);


            if (gbuy)
                gustosProfit.buyMoment(price, time);
            if (gsell)
                gustosProfit.sellMoment(price, time);

//            helper.put(mldata, "prevProfit1", gustosProfit.lastProfit(0));
//            helper.put(mldata, "prevProfit2", gustosProfit.lastProfit(1));
//            helper.put(mldata, "prevProfit3", gustosProfit.lastProfit(2));
//            helper.put(mldata, "prevProfit4", gustosProfit.lastProfit(3));
//            helper.put(mldata, "prevProfit5", gustosProfit.lastProfit(4));
//            helper.put(mldata, "prevProfitShouldBuy",gustosProfit.shouldBuy()?1:0,true);
//            helper.put(mldata, "prevProfitSimpleTest", gustosProfit.simpleTest() ? 1 : 0, true);
        } else {
            gbuy = helper.get(mldata, "gustosBuy") > 0;
            gsell = helper.get(mldata, "gustosSell") > 0;
        }

//            helper.put(mldata,"hour",data.bar(index).getBeginTime().getHour());
//            helper.put(mldata,"day",sheet.bar(index).getBeginTime().getDayOfWeek().getValue());
//            helper.put(mldata,"holiday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SUNDAY||sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SATURDAY?1:0);
//            helper.put(mldata,"monday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.MONDAY?1:0);
//            helper.put(mldata,"friday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.FRIDAY?1:0);


        if (!asFirst) {
            double targetPercent = 1 + model.sd;
            double stopLossPercent = 1 - model.sd;
            double target = price * targetPercent;
            double sl = price * stopLossPercent;


//            volumes.prepareForIntegral();
//            helper.put(mldata, "integralUp", volumes.integral(price, target));
//            helper.put(mldata, "integralUp2", volumes.integral(target, target * targetPercent));
//            helper.put(mldata, "integralDown", volumes.integral(sl, price));
//            helper.put(mldata, "integralDown2", volumes.integral(sl * stopLossPercent, sl));
//            helper.put(mldata, "ballanceOnBuy1", SheetUtils.volumesAroundLevel(data, price, index, 30));
//            helper.put(mldata, "ballanceOnBuy2", SheetUtils.volumesAroundLevel(data, price, index, 100));
//            helper.put(mldata, "ballanceOnBuy3", SheetUtils.volumesAroundLevel(data, price, index, 500));
//            helper.put(mldata, "ballanceOnTarget1", SheetUtils.volumesAroundLevel(data, target, index, 30));
//            helper.put(mldata, "ballanceOnTarget2", SheetUtils.volumesAroundLevel(data, target, index, 100));
//            helper.put(mldata, "ballanceOnTarget3", SheetUtils.volumesAroundLevel(data, target, index, 500));
//            helper.put(mldata, "ballanceOnSl1", SheetUtils.volumesAroundLevel(data, sl, index, 30));
//            helper.put(mldata, "ballanceOnSl2", SheetUtils.volumesAroundLevel(data, sl, index, 100));
//            helper.put(mldata, "ballanceOnSl3", SheetUtils.volumesAroundLevel(data, sl, index, 500));
//
//
//            helper.put(mldata, "upCandles1", SheetUtils.upCandles(data, index, 1));
//            helper.put(mldata, "upCandles2", SheetUtils.upCandles(data, index, 5));
//            helper.put(mldata, "upCandles3", SheetUtils.upCandles(data, index, 20));
//            helper.put(mldata, "downCandles1", SheetUtils.downCandles(data, index, 1));
//            helper.put(mldata, "downCandles2", SheetUtils.downCandles(data, index, 5));
//            helper.put(mldata, "downCandles3", SheetUtils.downCandles(data, index, 20));

            helper.put(mldata, "optBuys", model.optimized.countBuys(index));
            helper.put(mldata, "optSells", model.optimized.countSells(index));


            doWithMinsMaxes(mldata, 5);
//            doWithMinsMaxes(mldata,10);

            PlayStrategy strategy;
            Pair<Double, Integer> p;
            int willKnow = 0;
//            PlayStrategy strategy = new SimpleStrategy(targetPercent, stopLossPercent);
//            Pair<Double, Integer> p = strategy.calcProfit(data, index);
//            int willKnow = p.getSecond();
//            helper.put(mldata, "_profit", p.getFirst() > 1 ? 1.0 : 0, true);
//
//            for (int i = 1; i <= 3; i++) {
//                strategy = new SimpleStrategyWithBackup(targetPercent, stopLossPercent, i);
//                p = strategy.calcProfit(data, index);
//                willKnow = Math.max(willKnow, p.getSecond());
//                helper.put(mldata, "_profitWithBackup" + i, p.getFirst() > 1 ? 1.0 : 0, true);
//            }
            strategy = new GustosLogicStrategy();
            p = strategy.calcProfit(data, index);
            willKnow = Math.max(willKnow, p.getSecond());
            helper.put(mldata, "_goodBuy", p.getFirst() > 1 ? 1.0 : 0, true);
            mldata.weight = 1;//p.getFirst()>1?p.getFirst()-1:1/p.getFirst()-1;

            int nextSell = ((GustosLogicStrategy) strategy).nextSell(data, index);
            double nextPrice = nextSell >= data.size() ? price : data.bar(nextSell).getClosePrice();
            helper.put(mldata, "_goodSell", price > nextPrice ? 1.0 : 0, true);
            willKnow = Math.max(willKnow, nextSell);

            mldata.whenWillKnow = willKnow == Integer.MAX_VALUE ? Long.MAX_VALUE : data.bar(willKnow).getEndTime().toEpochSecond() + 120;
        }

        if (model.simpleModels.size() > 0) {
            for (int i = 0; i < model.simpleModels.size(); i++) {
                boolean cc = helper.classify(mldata, model.simpleModels.get(i), i, 0);
                helper.put(mldata, "1simple" + i, cc ? 1 : 0, true);
            }

        }

        if (model.models.size() > 0) {
            for (int i = 0; i < model.models.size(); i++)
                helper.putResult(mldata, i, helper.classify(mldata, model.models.get(i), i, 9));
            if (gbuy)
                plhistory1.buyMoment(price, time);
            if (gsell)
                plhistory1.sellMoment(price, time);

            boolean classifiedBuy = helper.get(mldata, "@goodBuy") > 0.5;
            boolean classifiedSell = helper.get(mldata, "@goodSell") > 0.5;
            if (gbuy && classifiedBuy)
                plhistory2.buyMoment(price, time);
            if (gsell)
                plhistory2.sellMoment(price, time);

            if (gbuy && classifiedBuy)
                plhistory3.buyMoment(price, time);

            if (gsell && classifiedSell)
                plhistory3.sellMoment(price, time);

        }

    }

    private void doWithMinsMaxes(MomentData mldata, int r) {
        int index = this.calcIndex;
        ArrayList<Integer> mins = SheetUtils.findPrevMinimums(data, index, r, 3);
//        helper.put(mldata,"mins_"+r+"_1",(data.bar(mins.get(0)).getMinPrice()/data.bar(mins.get(1)).getMinPrice()-1)*10);
//        helper.put(mldata,"mins_"+r+"_2",(data.bar(mins.get(0)).getMinPrice()/data.bar(mins.get(2)).getMinPrice()-1)*10);
//        helper.put(mldata,"mins_"+r+"_3",(data.bar(mins.get(1)).getMinPrice()/data.bar(mins.get(2)).getMinPrice()-1)*10);

        ArrayList<Integer> maxs = SheetUtils.findPrevMaximums(data, index, r, 3);
//        helper.put(mldata,"maxs_"+r+"_1",(data.bar(maxs.get(0)).getMaxPrice()/data.bar(maxs.get(1)).getMaxPrice()-1)*10);
//        helper.put(mldata,"maxs_"+r+"_2",(data.bar(maxs.get(0)).getMaxPrice()/data.bar(maxs.get(2)).getMaxPrice()-1)*10);
//        helper.put(mldata,"maxs_"+r+"_3",(data.bar(maxs.get(1)).getMaxPrice()/data.bar(maxs.get(2)).getMaxPrice()-1)*10);
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);

        double[] fitMin = fitter.fit(Arrays.asList(new WeightedObservedPoint(1, mins.get(0), data.bar(mins.get(0)).getMinPrice()),
                new WeightedObservedPoint(1, mins.get(1), data.bar(mins.get(1)).getMinPrice()),
                new WeightedObservedPoint(1, mins.get(2), data.bar(mins.get(2)).getMinPrice())));

        double[] fitMax = fitter.fit(Arrays.asList(new WeightedObservedPoint(1, maxs.get(0), data.bar(maxs.get(0)).getMaxPrice()),
                new WeightedObservedPoint(1, maxs.get(1), data.bar(maxs.get(1)).getMaxPrice()),
                new WeightedObservedPoint(1, maxs.get(2), data.bar(maxs.get(2)).getMaxPrice())));
//        helper.put(mldata,"mins_"+r+"_fit",fitMin[2]);
//        helper.put(mldata,"maxs_"+r+"_fit",fitMax[2]);
        double v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        double v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok", v1 < v2 ? 1 : 0, true);
//        helper.put(mldata,"minmax_"+r+"_pos", v1<v2?(data.bar(index).getClosePrice()-v1)/(v2-v1):0);
        index++;
        v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok1", v1 < v2 ? 1 : 0, true);
//        helper.put(mldata,"minmax_"+r+"_pos1", v1<v2?(data.bar(index).getClosePrice()-v1)/(v2-v1):0);
        index++;
        v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok2", v1 < v2 ? 1 : 0, true);
//        helper.put(mldata,"minmax_"+r+"_pos2", v1<v2?(data.bar(index).getClosePrice()-v1)/(v2-v1):0);


    }

    private double div(double v, double v1) {
        if (v1 == 0)
            return 0;
        return v / v1;
    }

    private boolean gustosSell(int index) {
        return CalcUtils.gustosSell(data, index, values.gustosAvgSell, values.gustosParams);
//        if (index == 0) return false;
//        XBar pbar = data.bar(index - 1);
//        XBar bar = data.bar(index);
//        double sma = values.gustosAvgSell.value();
//        double sd = values.gustosAvgSell.sd();
////        double p = sma - sd * values.gustosParams.sellDiv()*0.1;
//        return /*bar.getMaxPrice() >= p && */bar.getClosePrice() > sma + sd * values.gustosParams.sellBoundDiv() * 0.1 && pbar.getClosePrice() >= bar.getMinPrice();
    }

    private boolean gustosBuy(int index) {
        return CalcUtils.gustosBuy(data, index, values.gustosAvgBuy, values.gustosParams);
//        if (index == 0) return false;
//        XBar pbar = data.bar(index - 1);
//        XBar bar = data.bar(index);
//
//        double p = values.gustosAvgBuy.pvalue() - values.gustosAvgBuy.psd() * values.gustosParams.buyDiv() * 0.1;
//        return bar.getMinPrice() <= p && bar.getMaxPrice() >= p && bar.getClosePrice() < values.gustosAvgBuy.value() - values.gustosAvgBuy.sd() * values.gustosParams.buyBoundDiv() * 0.1 && pbar.getClosePrice() < bar.getMaxPrice();
    }

}

