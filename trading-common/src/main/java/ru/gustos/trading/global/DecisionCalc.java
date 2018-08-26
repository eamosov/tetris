package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.book.RecurrentValues;
import ru.gustos.trading.book.SheetUtils;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;
import static ru.gustos.trading.global.DecisionModel.MAIN;

public class DecisionCalc {
    public static boolean DETECTOR = false;
    int calcIndex = 0;
    int targetCalcedTo = 0;

    boolean gbuy, gsell;
    double price, minprice, maxprice;
    long time;
    RecurrentValues values;
    PLHistory gustosProfit;
    ArrayList<Integer> goodMoments = new ArrayList<>();
    ArrayList<Integer> badMoments = new ArrayList<>();

    InstrumentData data;
    MomentDataHelper helper;
    MomentDataHelper helper2;
    DecisionManager manager;

    boolean onlyCalc;

    public DecisionCalc(DecisionManager manager, boolean onlyCalc) {
        this.manager = manager;
        this.data = manager.data;
        this.onlyCalc = onlyCalc;
        helper = data.helper;
        helper2 = data.helper2;
        gustosProfit = new PLHistory(null, null);
//        volumes = new Volumes(data, false, false);
        values = new RecurrentValues(data);
    }


    public void calcTillEnd() {
        while (calcIndex < data.size())
            doNext();
    }

    private void doNext() {
//        volumes.calc(calcIndex);
        values.feed(calcIndex);
        calcUsual(calcIndex);
        if (DETECTOR)
            calcUsualDetector(calcIndex);
        if (calcIndex >= calcAllFrom && !onlyCalc) {
            calcTargets(calcIndex);
            calcPredictions(calcIndex);
//            if (DETECTOR) {
//            calcTargetsDetector(calcIndex);
//            calcPredictionsDetector(calcIndex);
//            }
        }
        calcIndex++;
    }

    public void calcAllDetectorTargets(){
        for (int i = calcAllFrom;i<calcIndex;i++)
            calcTargetsDetector(i);
    }


    private void calcUsualDetector(int index) {
        InstrumentMoment m = data.bars.get(index);
        MomentData mldata = m.mldata2;
//        helper2.put(mldata, "d2vol", values.deltaToVolumeShort.value() / values.deltaToVolume.value());
//        helper2.put(mldata, "d2voln", values.deltaToVolumeShort.value());
//        helper2.put(mldata, "mm2vol", values.maxminToVolumeShort.value() / values.maxminToVolume.value());
//        helper2.put(mldata, "mm2voln", values.maxminToVolumeShort.value());
//
//        helper2.put(mldata, "d2mm", values.deltaToMmShort.value() / values.deltaToMm.value());
//        helper2.put(mldata, "d2mmn", values.deltaToMmShort.value());
//
//        helper2.put(mldata, "mm", values.maxminShort.value() / values.maxmin.value());
        helper2.put(mldata, "change0", values.change0.value());
        helper2.put(mldata, "change1", values.change1.value());

//        helper2.put(mldata, "upCandles1", SheetUtils.upCandles(data, index, 1));
//        helper2.put(mldata, "upCandles2", SheetUtils.upCandles(data, index, 5));
//        helper2.put(mldata, "downCandles1", SheetUtils.downCandles(data, index, 1));
//        helper2.put(mldata, "downCandles2", SheetUtils.downCandles(data, index, 5));
//
//        helper2.put(mldata, "toAvgSd", div(price - values.gustosAvg.value(), values.gustosAvg.sd()));
//        helper2.put(mldata, "toAvgSdMax", div(maxprice - values.gustosAvg.value(), values.gustosAvg.sd()));
//        helper2.put(mldata, "toAvgSdMin", div(minprice - values.gustosAvg.value(), values.gustosAvg.sd()));
//        helper2.put(mldata, "sd", values.gustosAvg.sd() / price * 10);
//        helper2.put(mldata, "toAvgSdMaxP", div(maxprice - values.gustosAvg.pvalue(), values.gustosAvg.psd()));
//        helper2.put(mldata, "toAvgSdMinP", div(minprice - values.gustosAvg.pvalue(), values.gustosAvg.psd()));

//        helper2.put(mldata, "toAvgSd2", div(price - values.gustosAvg2.value(), values.gustosAvg2.sd()));
//        helper2.put(mldata, "toAvgSdMax2", div(maxprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
//        helper2.put(mldata, "toAvgSdMin2", div(minprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
//        helper2.put(mldata, "sd2", values.gustosAvg2.sd() / price * 10);
//        helper2.put(mldata, "toAvgSdMaxP2", div(maxprice - values.gustosAvg2.pvalue(), values.gustosAvg2.psd()));
//        helper2.put(mldata, "toAvgSdMinP2", div(minprice - values.gustosAvg2.pvalue(), values.gustosAvg2.psd()));

//        helper2.put(mldata, "toAvgSd3", div(price - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        helper2.put(mldata, "toAvgSdMax3", div(maxprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        helper2.put(mldata, "toAvgSdMin3", div(minprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        helper2.put(mldata, "sd3", values.gustosAvg3.sd() / price * 10);
//        helper2.put(mldata, "toAvgSdMaxP3", div(maxprice - values.gustosAvg3.pvalue(), values.gustosAvg3.psd()));
//        helper2.put(mldata, "toAvgSdMinP3", div(minprice - values.gustosAvg3.pvalue(), values.gustosAvg3.psd()));

        helper2.put(mldata, "macd0", values.macd0.value());
        helper2.put(mldata, "macd1", values.macd1.value());
        helper2.put(mldata, "stoh0", values.stoh0.percent());
        helper2.put(mldata, "stoh1", values.stoh1.percent());
        helper2.put(mldata, "stoh2", values.stoh2.percent());
        helper2.put(mldata, "rsi0", values.rsi0.value());
        helper2.put(mldata, "rsi1", values.rsi1.value());
        helper2.put(mldata, "rsi2", values.rsi2.value());

//        helper2.put(mldata, "volumeBurst", values.volumeShort.value() / values.volumeLong.value());
//        helper2.put(mldata, "volumeBurst2", values.volumeShort2.value() / values.volumeLong2.value());
//        helper2.put(mldata, "volume", m.bar.getVolume());
//        helper2.put(mldata, "price", m.bar.getClosePrice());

        for (int i = 1; i < Math.min(index, 5); i++)
            helper2.putLagged(mldata, data.bars.get(index - i).mldata2, i);
    }

    private void calcUsual(int index) {
        InstrumentMoment m = data.bars.get(index);
        MomentData mldata = m.mldata;
        MomentData prevmldata = index > 0 ? data.bars.get(index - 1).mldata : mldata;
        MomentData prevmldata2 = index > 1 ? data.bars.get(index - 2).mldata : mldata;
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

        helper.putLagged(mldata, "toAvgSd", prevmldata, 1);
        helper.putLagged(mldata, "toAvgSd2", prevmldata, 1);
        helper.putLagged(mldata, "toAvgSdMin", prevmldata, 1);
        helper.putLagged(mldata, "toAvgSdMax", prevmldata, 1);
        helper.putLagged(mldata, "toAvgSdMin2", prevmldata, 1);
        helper.putLagged(mldata, "toAvgSdMax2", prevmldata, 1);
        helper.putLagged(mldata, "sd", prevmldata, 1);
        helper.putLagged(mldata, "sd2", prevmldata, 1);

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

//        helper.put(mldata, "d2mm",values.deltaToMmShort.value()/values.deltaToMm.value());
//        helper.put(mldata, "d2mm_n",values.deltaToMmShort.value());

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
        helper.putLagged(mldata, "macd0", prevmldata, 1);
        helper.putLagged(mldata, "macd1", prevmldata, 1);
        helper.putLagged(mldata, "macd2", prevmldata, 1);
        helper.putLagged(mldata, "macd3", prevmldata, 1);
        helper.putLagged(mldata, "macd4", prevmldata, 1);

        helper.put(mldata, "vdema0", values.vdema0.value());
        helper.put(mldata, "vdema1", values.vdema1.value());
        helper.put(mldata, "vdema2", values.vdema2.value());
        helper.put(mldata, "vdema3", values.vdema3.value());
        helper.put(mldata, "vdema4", values.vdema4.value());
//        helper.putLagged(mldata,"vdema0",prevmldata,1);
//        helper.putLagged(mldata,"vdema1",prevmldata,1);
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

        helper.put(mldata, "vdema0_delta1", values.vdema0.pvalue() - values.vdema0.value());
        helper.put(mldata, "vdema1_delta1", values.vdema1.pvalue() - values.vdema1.value());
        helper.putDelta(mldata, "macd0", prevmldata, 1);
        helper.putDelta(mldata, "macd1", prevmldata, 1);
        helper.putDelta(mldata, "rsi0", prevmldata, 1);
        helper.putDelta(mldata, "rsi1", prevmldata, 1);
        helper.putDelta(mldata, "stoh0", prevmldata, 1);
        helper.putDelta(mldata, "stoh1", prevmldata, 1);
//        helper.putDelta(mldata, "vdema0", prevmldata, 2);
//        helper.putDelta(mldata, "vdema1", prevmldata, 2);
//        helper.putDelta(mldata, "macd0", prevmldata, 2);
//        helper.putDelta(mldata, "macd1", prevmldata, 2);
//        helper.putDelta(mldata, "rsi0", prevmldata, 2);
//        helper.putDelta(mldata, "rsi1", prevmldata, 2);
//        helper.putDelta(mldata, "stoh0", prevmldata, 2);
//        helper.putDelta(mldata, "stoh1", prevmldata, 2);
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
//
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
            double targetPercent = 1 + 0.02;
            double stopLossPercent = 1 - 0.02;
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


//            doWithMinsMaxes(mldata, 5);
//            doWithMinsMaxes(mldata,10);

        }

    }


    private void calcTargetsDetector(int index) {
        int horizon = 120;
        int calc = index - horizon;
        boolean hasHigher = false;
        boolean hasLower = false;
        int w = 3;
        for (int i = w; i < horizon; i++) {
            if (!hasHigher && SheetUtils.isMinimum(data, index - i, w) && data.bar(index - i).getMinPrice() > data.bar(calc).getClosePrice())
                hasHigher = true;

            if (!hasLower && SheetUtils.isMaximum(data, index - i, w) && data.bar(index - i).getMaxPrice() < data.bar(calc).getClosePrice())
                hasLower = true;
        }

        helper2.put(data.bars.get(calc).mldata2, "_buy", hasHigher && !hasLower ? 1 : 0, true);
        helper2.put(data.bars.get(calc).mldata2, "_sell", !hasHigher && hasLower ? 1 : 0, true);

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
                mldata.weight = (p.getFirst() > 1 ? p.getFirst() - 1 : 1 / p.getFirst() - 1) * 100;

                if (p.getFirst() > 1 + manager.limit())
                    goodMoments.add(i);


                if (p.getFirst() < 1 - manager.limit())
                    badMoments.add(i);

                manager.models.correctModelForMoment(mldata);


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

    private void calcPredictionsDetector(int index) {
        if (manager.hasModel()) {

            MomentData mldata = data.bars.get(index).mldata2;
            for (int i = 0; i < manager.models.model.models2.get(MAIN).size(); i++)
                helper.putResult(mldata, i, MAIN, manager.models.model.full && helper2.classify(mldata, manager.ignore(i == 0), manager.models.model.models2.get(MAIN).get(i), i, 9));

        }

    }

    public void calcPredictions(int index) {
        if (manager.hasModel()) {
            MomentData mldata = data.bars.get(index).mldata;
            MomentData mldata2 = data.bars.get(index).mldata2;
            boolean[] results = new boolean[2];
            for (int i = 0; i < manager.models.model.models.get(MAIN).size(); i++) {
                int c = 0;
                helper.putResult(mldata, i, MAIN, results[i] = manager.models.model.full && helper.classify(mldata, manager.ignore(i == 0), manager.models.model.models.get(MAIN).get(i), i, 9));
            }


            boolean classifiedBuy = helper.get(mldata, "@goodBuy|main") > 0.5;
            boolean classifiedSell = true;//helper.get(mldata, "@goodSell|main") > 0.5;

            boolean gbuy = this.gbuy;
            boolean gsell = this.gsell;
//            boolean gbuy = helper2.classify(mldata2,manager.ignore(true), manager.models.model.models2.get(MAIN).get(0),0,9);
//            boolean gsell = helper2.classify(mldata2,manager.ignore(false), manager.models.model.models2.get(MAIN).get(1),1,9);


            if (gbuy && manager.futuredata != null) {
                Object classifier = manager.models.model.models.get(MAIN).get(0);
                if (classifier instanceof RandomForestWithExam) {
                    RandomForestWithExam r = (RandomForestWithExam) classifier;
                    Instance inst = helper.makeInstance(mldata, manager.ignoreBuy, 0, 9);
                    Instances set = helper.makeEmptySet(manager.ignoreBuy, 0, 9);
                    inst.setDataset(set);
                    Pair<Double, Integer> pp = new GustosLogicStrategy().calcProfit(manager.futuredata, index);
                    if (pp.getSecond() < manager.futuredata.size()) {
                        inst.setValue(inst.classIndex(), pp.getFirst() > 1 ? 1 : 0);
                        try {
                            r.computeCombPizdunstvo(inst);
//                            double[][] p = r.computePizdunstvo(inst);
//                            double[][] p2 = r.computePizdunstvo2(inst);
//                            PizdunstvoData.pdbuy.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

//            if (gsell && futuredata != null) {
//                Object classifier = model.models.get(MAIN).get(1);
//                if (classifier instanceof RandomForestWithExam) {
//                    RandomForestWithExam r = (RandomForestWithExam) classifier;
//                    Instance inst = helper.makeInstance(mldata, ignoreSell, 1, 9);
//                    Instances set = helper.makeEmptySet(ignoreSell, 1, 9);
//                    inst.setDataset(set);
//                    int next = new GustosLogicStrategy().nextSell(futuredata, index);
//                    if (next < futuredata.size()) {
//                        inst.setValue(inst.classIndex(), futuredata.bar(calcIndex).getClosePrice() > futuredata.bar(next).getClosePrice() ? 1 : 0);
//                        try {
//                            double[][] p = r.computePizdunstvo(inst);
//                            double[][] p2 = r.computePizdunstvo2(inst);
//                            PizdunstvoData.pdsell.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//            }


//            Pair<Integer, Integer> strategy = methods.chooseStrategy(time);

            if (gbuy) {
//                if (methods.wasNotNull()) {
                manager.plhistoryBase.buyMoment(price, time);
                if (classifiedBuy) {
                    manager.plhistoryClassifiedBuy.buyMoment(price, time);
                    manager.plhistoryClassifiedSelected.buyMoment(price, time);
                }

//                    if (strategy != null && (strategy.getFirst() == -1 || results[0][strategy.getFirst()]))
//                }

            }

            if (gsell) {
//                if (methods.wasNotNull()) {
                manager.plhistoryBase.sellMoment(price, time);
                manager.plhistoryClassifiedBuy.sellMoment(price, time);
//                    if (strategy != null && (strategy.getSecond() == -1 || results[1][strategy.getSecond()]))
                if (classifiedSell)
                    manager.plhistoryClassifiedSelected.sellMoment(price, time);
//                }
            }


        }

    }

    private double div(double v, double v1) {
        if (v1 == 0)
            return 0;
        return v / v1;
    }

}
