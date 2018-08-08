package ru.gustos.trading.global;

import com.google.common.collect.Sets;
import kotlin.Pair;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.jetbrains.annotations.NotNull;
import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.*;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;

public class StandardInstrumentCalc {
    public static final int calcAllFrom = 60 * 24 * 7 * 2;
    public static final int calcSimpleFrom = 60 * 24 * 16;
    public static final int calcModelFrom = 60 * 24 * 7 * 6;
    public static final int optimizeInterval = 60 * 24 * 7 * 2;
    public static final int optimizePeriod = 60 * 24 * 2;

    public static final boolean LOGS = true;

    public static final int TREES = 300;

    public static boolean withOptimize = false;

//    public static HashSet<String> ignoreBuy = Sets.newHashSet();
//    public static HashSet<String> ignoreSell = Sets.newHashSet();
    public static HashSet<String> ignoreBuy = Sets.newHashSet("gustosBuy","gustosSell");//,"sd_lag1","sd_delta1","sd_lag2","sd_delta2","macd0","macd1","macd2","macd3");
    public static HashSet<String> ignoreSell = Sets.newHashSet("gustosBuy","gustosSell");//,"d2vol","mm2vol","d2vol_n","mm2vol_n");
//    ,"macd4","toAvgSdMax2_delta2","toAvgSdMax2_delta1","toAvgSd2_delta2","toAvgSdMin2_delta1","toAvgSd2_lag1","toAvgSdMax2","toAvgSdMin2","sd_delta1",
//            "toAvgSdMin_delta1","toAvgSdMax2_lag1","toAvgSdMax_delta1","toAvgSd2","sd2","toAvgSd_delta2","sd2_delta1","toAvgSdMin2_delta2","toAvgSdMin2_lag1","toAvgSd_lag1",
//            "sd_delta2","toAvgSdMin2_lag2","toAvgSd2_lag2");

    static HashSet<String> ignore(boolean buy){
        return buy?ignoreBuy:ignoreSell;
    }

    static final int OPTION_ALL = 0;
    static final int OPTION_STD = 1;
    static final int OPTION_PRECISION = 2;
    static final int OPTION_RECALL = 3;

    static final HashMap<String,Integer> buyOptions = new HashMap<>();
    static final HashMap<String,Integer> sellOptions = new HashMap<>();
    static {

        buyOptions.put("ETH_USDT",OPTION_RECALL);
        sellOptions.put("ETH_USDT",OPTION_PRECISION);

        buyOptions.put("BNB_USDT",OPTION_STD);
        sellOptions.put("BNB_USDT",OPTION_RECALL);

        buyOptions.put("NEO_USDT",OPTION_PRECISION);
        sellOptions.put("NEO_USDT",OPTION_RECALL);

    }
    static int option(boolean buy, String instrument){
        int result;
        if (buy)
            result = buyOptions.getOrDefault(instrument,OPTION_RECALL);
        else
            result = sellOptions.getOrDefault(instrument,OPTION_ALL);

        if (result==OPTION_RECALL)
            result = OPTION_STD;

        return result;
    }

    static final String MAIN = "main";
    static final boolean USE_OPTIONS = true;
    static final String PRECISION = "precision";
    static final String RECALL = "recall";
    static final String[] logics = new String[]{MAIN,PRECISION,RECALL};

    static boolean usePrecision = false;
    static boolean useRecall = false;

    InstrumentData data;

    public InstrumentData futuredata; // for pizdunstvo check

    MomentDataHelper helper;
    public PLHistory plhistoryBase;
    public PLHistory plhistoryClassifiedBuy;
    public PLHistory plhistoryClassifiedSelected;

    int calcIndex = 0;
    int targetCalcedTo = 0;
    int lastOptimize = 0;
    long prevLastMoment = 0;

    boolean gbuy, gsell;
    double price, minprice, maxprice;
    long time;

    int cpus;



    //    Volumes volumes;
    RecurrentValues values;
    PLHistory gustosProfit;

    TradeMethodsSolver methods;

    Model model;
    Model newModel = null;
    boolean onlyUsual;

    public StandardInstrumentCalc(InstrumentData data, int cpus, boolean onlyUsual, boolean withModelSolver) {
        this.helper = data.helper;
        this.onlyUsual = onlyUsual;
        methods = new TradeMethodsSolver(data.instrument.toString());
        this.data = data;
        this.cpus = cpus;
        plhistoryBase = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer1 : null);
        plhistoryClassifiedBuy = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer2 : null);
        plhistoryClassifiedSelected = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer3 : null);
        model = new Model();
        initForCalc();
        calcTillEnd(withModelSolver);

    }

    private void calcTillEnd(boolean withModelSolver) {
        while (calcIndex < data.size()) {
            if (withModelSolver && data.size()-calcIndex<TradeMethodsSolver.BIG_INTERVAL/60+60*24*5)
                checkNeedRenew(false);
            doNext();
        }
    }

    private void initForCalc() {
        gustosProfit = new PLHistory(null, null);
//        volumes = new Volumes(data, false, false);
        values = new RecurrentValues(data);
    }


//    public void precalc() {
//        for (int i = 0; i < data.size(); i++) {
//            volumes.calc(i);
//            values.feed(i);
//            calc(i, true);
//        }
//        initForCalc();
//    }

    public void calcTo(long time) {
        if (calcIndex >= data.size()) return;

        checkNeedRenew(false);
        checkTakeNewModel();

        while (calcIndex < data.size() && data.bar(calcIndex).getEndTime().toEpochSecond() <= time)
            doNext();

    }

    public void addBar(XBar bar) {
        checkTakeNewModel();
        data.addBar(bar);
        calcTillEnd(false);

    }

    private void doNext() {
//        volumes.calc(calcIndex);
        values.feed(calcIndex);
        if (withOptimize)
            model.optimized.feed(calcIndex);
        if (calcIndex >= calcAllFrom && !onlyUsual) {
            calcUsual(calcIndex);
            calcTargets(calcIndex);
            calcPredictions(calcIndex);
        } else
            calcUsual(calcIndex);
        calcIndex++;
    }

    public void checkNeedRenew(boolean thread) {
        if (calcIndex >= calcAllFrom && calcIndex - model.index >= 60) {
            ArrayList<Long> moments = makeGoodBadMoments(true, true, true);
            if (moments.size()>0 && moments.get(moments.size()-1).doubleValue()!=prevLastMoment) {
                prevLastMoment = moments.get(moments.size()-1);
                renewModel(thread);
            }
        }
    }

    private void renewModel(boolean thread) {
        if (LOGS)
            System.out.println("renew model on " + data.instrument + ", day " + (calcIndex / (60 * 24)));
        final int currentIndex = calcIndex;
        final Model model = new Model(this.model, calcIndex);
        if (thread)
            new Thread(() -> doRenewModel(model, currentIndex)).start();
        else {
            doRenewModel(model, currentIndex);
        }
    }

    private void doRenewModel(Model model, int calcIndex) {
        model.sd = calcBestSd(model.sd);

//        if (calcIndex >= calcSimpleFrom)
//            prepareSimpleModel(model,calcIndex);

        if (calcIndex >= calcModelFrom)
            prepareModel(model, calcIndex);

        if (withOptimize) {
            if (calcIndex >= optimizeInterval && calcIndex - lastOptimize > optimizePeriod) {
                model.optimized.add(calcIndex, optimizeInterval);
                lastOptimize = calcIndex;
            }
        }

        newModel = model;
    }

    private void checkTakeNewModel() {
        if (newModel != null) {
            model = newModel;
            newModel = null;
            if (withOptimize) {
                while (model.optimized.needIndex() < calcIndex)
                    model.optimized.feed(model.optimized.needIndex());
            }
        }
    }


    private void prepareModel(Model model, int calcIndex) {
        int period = 60 * 3;
        long endtime = data.bar(calcIndex - 1).getEndTime().toEpochSecond();
//        int period = 60 * 24 * 2;
        try {
//            makeSimpleModel(models,period,endtime);
            makeGoodBadModel(model, calcIndex, period, endtime, true, true, 9);
//            makeGoodBadModel(models2,period,endtime, false, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ArrayList<Long> makeGoodBadMoments(boolean good, boolean bad, boolean buyTime) {
        ArrayList<Long> moments = new ArrayList<>();
        double limit = 0.03;
//        if (data.instrument.toString().equalsIgnoreCase("BTC_USDT"))
//            limit = 0.025;
        if (good) {
            ArrayList<Long> tmpmoments;
            tmpmoments = gustosProfit.getCriticalBuyMoments(limit, true, false, buyTime);
            while (tmpmoments.size() > 30) tmpmoments.remove(0);
            moments.addAll(tmpmoments);
        }

        if (bad) {
            ArrayList<Long> tmpmoments;
            tmpmoments = gustosProfit.getCriticalBuyMoments(limit, false, true, buyTime);
            while (tmpmoments.size() > 30) tmpmoments.remove(0);
            moments.addAll(tmpmoments);
            moments.sort(Long::compare);
        }
        return moments;
    }


    private void makeGoodBadModel(Model model, int calcIndex, int period, long endtime, boolean good, boolean bad, int level) {
        try {
            model.clear();
            for (int i = 0; i < helper.futureAttributes(); i++) {
                boolean full = false;
                HashSet<String> ignore = ignore(i==0);
                Instances set1 = helper.makeEmptySet(ignore, i, level);
                ArrayList<Long> moments = makeGoodBadMoments(good, bad, i == 0);
//                System.out.println(moments.size());
                full = moments.size()>=10;
                for (int j = 0; j < moments.size(); j++) {
                    long time = moments.get(j);
                    int index = data.getBarIndex(time);
                    if (index > calcAllFrom) {
                        Instances settemp = helper.makeSet(data.bars.direct(), ignore,Math.max(calcAllFrom, index - period), Math.min(targetCalcedTo, index + period), endtime, i, level);
                        set1.addAll(settemp);
                    }
                }
                if (set1.size() < 10) {
                    System.out.println(String.format("use simple model! instrument: %s, attribute: %d, size: %d", data.instrument, i, set1.size()));
                    model.models.get(MAIN).add(makeSimpleModel(ignore, period, endtime, i, level));
                    if (usePrecision)
                        model.models.get(PRECISION).add(makeSimpleModel(ignore,period, endtime, i, level));
                    if (useRecall)
                        model.models.get(RECALL).add(makeSimpleModel(ignore,period, endtime, i, level));
                }else {
                    model.full = full;
                    if (set1.numDistinctValues(set1.classIndex()) == 1) System.out.println("no distinct values!");
                    RandomForestWithExam rf = new RandomForestWithExam();
                    rf.setNumExecutionSlots(cpus);
                    rf.setNumIterations(TREES);
                    rf.setSeed(calcIndex+(int)System.currentTimeMillis());

                    int opt = option(i == 0,data.instrument.toString());
                    if (USE_OPTIONS && opt!=OPTION_STD) {
                        if (opt == OPTION_PRECISION || opt == OPTION_RECALL) {
                            CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                            CostMatrix costMatrix = new CostMatrix(2);
                            if (opt == OPTION_PRECISION)
                                costMatrix.setElement(0, 1, 2);
                            else
                                costMatrix.setElement(1, 0, 2);
                            costSensitiveClassifier.setClassifier(rf);
                            costSensitiveClassifier.setCostMatrix(costMatrix);
                            costSensitiveClassifier.buildClassifier(set1);
                            model.models.get(MAIN).add(rf);
                        } else {
                            model.models.get(MAIN).add(new AllClassifer());
                        }
                    } else {
                        rf.buildClassifier(set1);
                        model.models.get(MAIN).add(rf);
                    }
                    if (usePrecision) {
                        rf = new RandomForestWithExam();
                        rf.setNumExecutionSlots(cpus);
                        rf.setNumIterations(TREES);
                        rf.setSeed(calcIndex);

                        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                        CostMatrix costMatrix = new CostMatrix(2);
                        costMatrix.setElement(0, 1, 2);
                        costSensitiveClassifier.setClassifier(rf);
                        costSensitiveClassifier.setCostMatrix(costMatrix);
                        costSensitiveClassifier.buildClassifier(set1);
                        model.models.get(PRECISION).add(costSensitiveClassifier);
                    }
                    if (useRecall) {
                        rf = new RandomForestWithExam();
                        rf.setNumExecutionSlots(cpus);
                        rf.setNumIterations(TREES);
                        rf.setSeed(calcIndex);
                        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                        CostMatrix costMatrix = new CostMatrix(2);
//                        costMatrix.setElement(0, 1, 2);
                        costMatrix.setElement(1, 0, 2);
                        costSensitiveClassifier.setClassifier(rf);
                        costSensitiveClassifier.setCostMatrix(costMatrix);
                        costSensitiveClassifier.buildClassifier(set1);
                        model.models.get(RECALL).add(costSensitiveClassifier);
                    }


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NullPointerException("error making classifier");
        }

    }

    private Classifier makeSimpleModel(HashSet<String> ignore, int period, long endtime, int attribute, int level) {
        try {
            Instances set1;
            set1 = helper.makeSet(data.bars.direct(), ignore, calcIndex - period, targetCalcedTo, endtime, attribute, level);
            int plus = 1;
            while (set1.size() == 0 && calcIndex - period * (1 + plus) >= 0) {
                set1 = helper.makeSet(data.bars.direct(), ignore,calcIndex - period * (1 + plus), targetCalcedTo, endtime, attribute, level);
                plus++;
            }
            if (set1.size() == 0)
                throw new NullPointerException("cant make set for simple model: " + calcIndex + ", " + targetCalcedTo);

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
        int from = data.getBarIndex(data.bar(calcIndex - 1).getBeginTime().toEpochSecond() - 3600 * 24 * 4);
//        System.out.println(calcIndex+" "+from+" "+data.size()+" "+(data.bar(calcIndex-1).getBeginTime().toEpochSecond() - 3600 * 24 * 4));
        while (numberOfTrades(from, calcIndex - 1, sd) > 20)
            sd *= 1.1;
        while (numberOfTrades(from, calcIndex - 1, sd) < 20)
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


    private void calcUsual(int index) {
        InstrumentMoment m = data.bars.get(index);
        MomentData mldata = m.mldata;
        MomentData prevmldata = index>0?data.bars.get(index-1).mldata:mldata;
        MomentData prevmldata2 = index>1?data.bars.get(index-2).mldata:mldata;
        price = m.bar.getClosePrice();
        maxprice = m.bar.getMaxPrice();
        minprice = m.bar.getMinPrice();
        time = m.bar.getEndTime().toEpochSecond();

        gbuy = CalcUtils.gustosBuy(data, index, values.gustosAvg, values.gustosParams);
        gsell = CalcUtils.gustosSell(data, index, values.gustosAvg4, values.gustosParams);
//        gbuy = CalcUtils.gustosBuy(data, index, values.gustosAvgBuy, values.gustosParams);
//        gsell = CalcUtils.gustosSell(data, index, values.gustosAvgSell, values.gustosParams);

        helper.put(mldata, "gustosBuy", gbuy ? 1.0 : 0, true);
        helper.put(mldata, "gustosSell", gsell ? 1.0 : 0, true);

//            helper.put(mldata, "1simple0", 0, true);
//            helper.put(mldata, "1simple1", 0, true);

        helper.put(mldata, "toAvgSd", div(price - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper.put(mldata, "toAvgSdMax", div(maxprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper.put(mldata, "toAvgSdMin", div(minprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper.put(mldata, "sd", values.gustosAvg.sd() / price * 10);

        helper.put(mldata, "toAvgSd2", div(price - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper.put(mldata, "toAvgSdMax2", div(maxprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper.put(mldata, "toAvgSdMin2", div(minprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper.put(mldata, "sd2", values.gustosAvg2.sd() / price * 10);

        helper.put(mldata, "toAvgSd3", div(price - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper.put(mldata, "toAvgSdMax3", div(maxprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper.put(mldata, "toAvgSdMin3", div(minprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper.put(mldata, "sd3", values.gustosAvg3.sd() / price * 10);

        helper.put(mldata, "toAvgSd4", div(price - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper.put(mldata, "toAvgSdMax4", div(maxprice - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper.put(mldata, "toAvgSdMin4", div(minprice - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper.put(mldata, "sd4", values.gustosAvg4.sd() / price * 10);

//        helper.putLagged(mldata,"toAvgSd",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSd",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSd",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSd2",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSdMin",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSdMax",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSdMin2",prevmldata,1);
//        helper.putLagged(mldata,"toAvgSdMax2",prevmldata,1);
//        helper.putLagged(mldata,"sd",prevmldata,1);
//        helper.putLagged(mldata,"sd2",prevmldata,1);

//        helper.putLagged(mldata,"toAvgSd",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSd2",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSdMin",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSdMax",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSdMin2",prevmldata2,2);
//        helper.putLagged(mldata,"toAvgSdMax2",prevmldata2,2);
//        helper.putLagged(mldata,"sd",prevmldata2,2);
//        helper.putLagged(mldata,"sd2",prevmldata2,2);

//        helper.put(mldata, "toAvgSdB", div(price - values.gustosAvgBuy.value(), values.gustosAvgBuy.sd()));
//        helper.put(mldata, "toAvgSdS", div(price - values.gustosAvgSell.value(), values.gustosAvgSell.sd()));
//        helper.put(mldata, "sdB", values.gustosAvgBuy.sd() / price * 10);
//        helper.put(mldata, "sdS", values.gustosAvgSell.sd() / price * 10);

        helper.put(mldata, "d2vol", values.deltaToVolumeShort.value() / values.deltaToVolume.value());
        helper.put(mldata, "mm2vol", values.maxminToVolumeShort.value() / values.maxminToVolume.value());
        helper.put(mldata, "d2vol_n", values.deltaToVolumeShort.value());
        helper.put(mldata, "mm2vol_n", values.maxminToVolumeShort.value());

        helper.put(mldata, "mm", values.maxminShort.value() / values.maxmin.value());



        helper.put(mldata, "macd0", values.macd0.value());
        helper.put(mldata, "macd1", values.macd1.value());
        helper.put(mldata, "macd2", values.macd2.value());
        helper.put(mldata, "macd3", values.macd3.value());
        helper.put(mldata, "macd4", values.macd4.value());
//        helper.put(mldata, "pmacd0", values.macd0.pvalue());
//        helper.put(mldata, "pmacd1", values.macd1.pvalue());
//        helper.put(mldata, "pmacd2", values.macd2.pvalue());
//        helper.put(mldata, "pmacd3", values.macd3.pvalue());
//        helper.put(mldata, "pmacd4", values.macd4.pvalue());

        helper.put(mldata, "vdema0", values.vdema0.value());
        helper.put(mldata, "vdema1", values.vdema1.value());
        helper.put(mldata, "vdema2", values.vdema2.value());
        helper.put(mldata, "vdema3", values.vdema3.value());
        helper.put(mldata, "vdema4", values.vdema4.value());
//        helper.putLagged(mldata,"vdema2",prevmldata,1);
//        helper.putLagged(mldata,"vdema3",prevmldata,1);
//        helper.putLagged(mldata,"vdema4",prevmldata,1);
//        helper.put(mldata, "pvdema0", values.vdema0.pvalue());
//        helper.put(mldata, "pvdema1", values.vdema1.pvalue());
//        helper.put(mldata, "pvdema2", values.vdema2.pvalue());
//        helper.put(mldata, "pvdema3", values.vdema3.pvalue());
//        helper.put(mldata, "pvdema4", values.vdema4.pvalue());
//        helper.put(mldata, "vmacd0", values.vmacd0.value());
//        helper.put(mldata, "vmacd1", values.vmacd1.value());
//        helper.put(mldata, "vmacd2", values.vmacd2.value());

        //        helper.put(mldata, "pmacd0", values.macd0.value() - values.macd0.pvalue());
//        helper.put(mldata, "pmacd1", values.macd1.value() - values.macd1.pvalue());
//        helper.put(mldata, "pmacd2", values.macd2.value() - values.macd2.pvalue());
        helper.put(mldata, "rsi0", values.rsi0.value());
        helper.put(mldata, "rsi1", values.rsi1.value());
        helper.put(mldata, "rsi2", values.rsi2.value());
        helper.put(mldata, "rsiv0", values.rsiv0.value());
        helper.put(mldata, "rsiv1", values.rsiv1.value());
        helper.put(mldata, "rsiv2", values.rsiv2.value());
        helper.put(mldata, "stoh0", values.stoh0.percent());
        helper.put(mldata, "stoh1", values.stoh1.percent());
        helper.put(mldata, "stoh2", values.stoh2.percent());
        helper.put(mldata, "change0", values.change0.value());
        helper.put(mldata, "change1", values.change1.value());
        helper.put(mldata, "change2", values.change2.value());
        helper.put(mldata, "change3", values.change3.value());
        helper.put(mldata, "change4", values.change4.value());

        helper.putDelta(mldata,"vdema0",prevmldata,1);
        helper.putDelta(mldata,"vdema1",prevmldata,1);
        helper.putDelta(mldata,"macd0",prevmldata,1);
        helper.putDelta(mldata,"macd1",prevmldata,1);
        helper.putDelta(mldata,"rsi0",prevmldata,1);
        helper.putDelta(mldata,"rsi1",prevmldata,1);
        helper.putDelta(mldata,"stoh0",prevmldata,1);
        helper.putDelta(mldata,"stoh1",prevmldata,1);
        //            helper.put(mldata,"rising",sheet.bar(index-1).getClosePrice() < sheet.bar(index).getMinPrice()?1:0);
//            helper.put(mldata,"falling",sheet.bar(index-1).getClosePrice() >= sheet.bar(index).getMaxPrice()?1:0);

//        helper.put(mldata, "toAvg0", ((price - values.ema0.value()) / price));
//        helper.put(mldata, "toAvg1", ((price - values.ema1.value()) / price));
//        helper.put(mldata, "toAvg2", ((price - values.ema2.value()) / price));
//        helper.put(mldata, "toAvg3", ((price - values.ema3.value()) / price));
//        helper.put(mldata, "toAvgSdU0", (div(price - values.ema0.value(), values.sd0.value())));
//        helper.put(mldata, "toAvgSdU1", (div(price - values.ema1.value(), values.sd1.value())));
//        helper.put(mldata, "toAvgSdU2", (div(price - values.ema2.value(), values.sd2.value())));
//        helper.put(mldata, "toAvgSdU3", (div(price - values.ema3.value(), values.sd3.value())));
//        helper.put(mldata, "sd_usual0", Math.sqrt(values.sd0.value()) / price * 10);
//        helper.put(mldata, "sd_usual1", Math.sqrt(values.sd1.value()) / price * 10);
//        helper.put(mldata, "sd_usual2", Math.sqrt(values.sd2.value()) / price * 10);
//        helper.put(mldata, "sd_usual3", Math.sqrt(values.sd3.value()) / price * 10);

//        helper.put(mldata, "volumeBurst", values.volumeShort.value() / values.volumeLong.value());
//        helper.put(mldata, "volumeBurst2", values.volumeShort2.value() / values.volumeLong2.value());
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

//            helper.put(mldata,"hour",data.bar(index).getBeginTime().getHour());
//            helper.put(mldata,"day",sheet.bar(index).getBeginTime().getDayOfWeek().getValue());
//            helper.put(mldata,"holiday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SUNDAY||sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SATURDAY?1:0);
//            helper.put(mldata,"monday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.MONDAY?1:0);
//            helper.put(mldata,"friday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.FRIDAY?1:0);

        if (index > calcAllFrom) {
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

            if (withOptimize) {
                helper.put(mldata, "optBuys", model.optimized.countBuys(index));
                helper.put(mldata, "optSells", model.optimized.countSells(index));
            }


//            doWithMinsMaxes(mldata, 5);
//            doWithMinsMaxes(mldata,10);

        }
        if (model.simpleModels.size() > 0) {
            for (int i = 0; i < model.simpleModels.size(); i++) {
                boolean cc = helper.classify(mldata, ignore(i==0),model.simpleModels.get(i), i, 0);
                helper.put(mldata, "1simple" + i, cc ? 1 : 0, true);
            }
        }

    }

    private void calcTargets(int index) {

        if (gsell) {
            for (int i = targetCalcedTo; i < index; i++) {
                MomentData mldata = data.bars.get(i).mldata;
                GustosLogicStrategy strategy;
                Pair<Double, Integer> p;
                int willKnow = 0;
                strategy = new GustosLogicStrategy();
                p = strategy.calcProfit(data, i);
                willKnow = Math.max(willKnow, p.getSecond());
                helper.put(mldata, "_goodBuy", p.getFirst() > 1 ? 1.0 : 0, true);
                mldata.weight = 1;//p.getFirst()>1?p.getFirst()-1:1/p.getFirst()-1;

                int nextSell = strategy.nextSell(data, i);
                double nextPrice = nextSell >= data.size() ? price : data.bar(nextSell).getClosePrice();
                double curPrice = data.bar(i).getClosePrice();
                helper.put(mldata, "_goodSell", curPrice > nextPrice ? 1.0 : 0, true);
                willKnow = Math.max(willKnow, nextSell);
                if (willKnow == Integer.MAX_VALUE) {
                    targetCalcedTo = i;
                    return;
                }

                mldata.whenWillKnow = data.bar(willKnow).getEndTime().toEpochSecond();
            }

            targetCalcedTo = index;
        }


    }

    public void calcPredictions(int index) {
        if (model.models.get(MAIN).size() > 0) {
            MomentData mldata = data.bars.get(index).mldata;
            boolean[][] results = new boolean[2][1+(usePrecision?1:0)+(useRecall?1:0)];
            for (int i = 0; i < model.models.get(MAIN).size(); i++) {

                int c = 0;
                helper.putResult(mldata, i, MAIN, results[i][c++] = model.full && helper.classify(mldata, ignore(i==0),model.models.get(MAIN).get(i), i, 9));
                if (usePrecision)
                    helper.putResult(mldata, i, PRECISION, results[i][c++] = model.full && helper.classify(mldata, ignore(i==0), model.models.get(PRECISION).get(i), i, 9));
                if (useRecall)
                    helper.putResult(mldata, i, RECALL, results[i][c++] = model.full && helper.classify(mldata, ignore(i==0), model.models.get(RECALL).get(i), i, 9));
            }



            boolean classifiedBuy = helper.get(mldata, "@goodBuy|main") > 0.5;
            boolean classifiedSell = helper.get(mldata, "@goodSell|main") > 0.5;

            if (gbuy && futuredata!=null){
                Classifier classifier = model.models.get(MAIN).get(0);
                if (classifier instanceof RandomForestWithExam) {
                    RandomForestWithExam r = (RandomForestWithExam) classifier;
                    Instance inst = helper.makeInstance(mldata, ignoreBuy, 0, 9);
                    Instances set = helper.makeEmptySet(ignoreBuy,0, 9);
                    inst.setDataset(set);
                    Pair<Double, Integer> pp = new GustosLogicStrategy().calcProfit(futuredata, index);
                    if (pp.getSecond()<futuredata.size()) {
                        inst.setValue(inst.classIndex(), pp.getFirst() > 1 ? 1 : 0);
                        try {
                            double[][] p = r.computePizdunstvo(inst);
                            double[][] p2 = r.computePizdunstvo2(inst);
                            PizdunstvoData.pdbuy.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (gsell && futuredata!=null){
                Classifier classifier = model.models.get(MAIN).get(1);
                if (classifier instanceof RandomForestWithExam) {
                    RandomForestWithExam r = (RandomForestWithExam) classifier;
                    Instance inst = helper.makeInstance(mldata, ignoreSell, 1, 9);
                    Instances set = helper.makeEmptySet(ignoreSell, 1, 9);
                    inst.setDataset(set);
                    int next = new GustosLogicStrategy().nextSell(futuredata, index);
                    if (next<futuredata.size()) {
                        inst.setValue(inst.classIndex(), futuredata.bar(calcIndex).getClosePrice() > futuredata.bar(next).getClosePrice() ? 1 : 0);
                        try {
                            double[][] p = r.computePizdunstvo(inst);
                            double[][] p2 = r.computePizdunstvo2(inst);
                            PizdunstvoData.pdsell.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }


//            Pair<Integer, Integer> strategy = methods.chooseStrategy(time);

            if (gbuy) {
//                if (methods.wasNotNull()) {
                    plhistoryBase.buyMoment(price, time);
                    if (classifiedBuy) {
                        plhistoryClassifiedBuy.buyMoment(price, time);
                        plhistoryClassifiedSelected.buyMoment(price, time);
                    }

//                    if (strategy != null && (strategy.getFirst() == -1 || results[0][strategy.getFirst()]))
//                }
                methods.buy(time,price,results[0]);

            }

            if (gsell) {
//                if (methods.wasNotNull()) {
                    plhistoryBase.sellMoment(price, time);
                    plhistoryClassifiedBuy.sellMoment(price, time);
//                    if (strategy != null && (strategy.getSecond() == -1 || results[1][strategy.getSecond()]))
                if (classifiedSell)
                        plhistoryClassifiedSelected.sellMoment(price, time);
//                }
                methods.sell(time,price,results[1]);
            }


        }

    }

    private void doWithMinsMaxes(MomentData mldata, int r) {
        int index = this.calcIndex;
        ArrayList<Integer> mins = SheetUtils.findPrevMinimums(data, index, r, 3);

        ArrayList<Integer> maxs = SheetUtils.findPrevMaximums(data, index, r, 3);
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);

        double[] fitMin = fitter.fit(Arrays.asList(new WeightedObservedPoint(1, mins.get(0), data.bar(mins.get(0)).getMinPrice()),
                new WeightedObservedPoint(1, mins.get(1), data.bar(mins.get(1)).getMinPrice()),
                new WeightedObservedPoint(1, mins.get(2), data.bar(mins.get(2)).getMinPrice())));

        double[] fitMax = fitter.fit(Arrays.asList(new WeightedObservedPoint(1, maxs.get(0), data.bar(maxs.get(0)).getMaxPrice()),
                new WeightedObservedPoint(1, maxs.get(1), data.bar(maxs.get(1)).getMaxPrice()),
                new WeightedObservedPoint(1, maxs.get(2), data.bar(maxs.get(2)).getMaxPrice())));
        double v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        double v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok", v1 < v2 ? 1 : 0, true);
        index++;
        v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok1", v1 < v2 ? 1 : 0, true);
        index++;
        v1 = fitMin[0] + fitMin[1] * index + fitMin[2] * index * index;
        v2 = fitMax[0] + fitMax[1] * index + fitMax[2] * index * index;
        helper.put(mldata, "minmax_" + r + "_ok2", v1 < v2 ? 1 : 0, true);


    }

    private double div(double v, double v1) {
        if (v1 == 0)
            return 0;
        return v / v1;
    }

    @NotNull
    public Decision decision() {
        MomentData mldata = data.bars.get(calcIndex - 1).mldata;
//        Pair<Integer, Integer> p = methods.chooseStrategy(time);
        boolean classifiedBuy;
        boolean classifiedSell;
//        if (p.getFirst()<0)
//            classifiedBuy = true;
//        else
//            classifiedBuy = helper.get(mldata, "@goodBuy|"+logics[p.getFirst()]) > 0.5;
//        if (p.getSecond()<0)
//            classifiedSell = true;
//        else
//            classifiedSell = helper.get(mldata, "@goodSell|"+logics[p.getSecond()]) > 0.5;
        classifiedBuy = helper.get(mldata,"@goodBuy|main") > 0.5;
        classifiedSell = helper.get(mldata,"@goodSell|main") > 0.5;

        boolean gbuy = helper.get(mldata, "gustosBuy") > 0.5;
        boolean gsell = helper.get(mldata, "gustosSell") > 0.5;
        if (classifiedBuy && gbuy) return Decision.BUY;
        if (classifiedSell && gsell) return Decision.SELL;
        return Decision.NONE;
    }


    class Model {
        double sd = 0.02;
        int index;
        boolean full = false;
        Hashtable<String,ArrayList<Classifier>> models = new Hashtable<>();
        ArrayList<Classifier> simpleModels = new ArrayList<>();
        GustosLogicsOptimizedHistory optimized;

        public Model() {
            initModels();
            if (withOptimize)
                optimized = new GustosLogicsOptimizedHistory(data);
        }


        public Model(Model model, int index) {
            initModels();
            this.index = index;
            sd = model.sd;
            if (withOptimize)
                optimized = model.optimized.clone();
        }

        private void initModels() {
            models.put(MAIN, new ArrayList<>());
            if (usePrecision)
                models.put(PRECISION, new ArrayList<>());
            if (useRecall)
                models.put(RECALL, new ArrayList<>());
        }


        public void clear() {
            full = false;
            models.get(MAIN).clear();
            if (usePrecision)
                models.get(PRECISION).clear();
            if (useRecall)
                models.get(RECALL).clear();

        }
    }


}

