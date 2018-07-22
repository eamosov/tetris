package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.*;
import ru.gustos.trading.global.MomentData;
import ru.gustos.trading.global.MomentDataHelper;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;
import weka.core.Instances;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Random;

public class DataPlayer {
    Sheet sheet;
    MomentData[] data;
    MomentDataHelper helper;
    Volumes volumes;
    RecurrentValues values;


    public DataPlayer(Sheet sheet){
        this.sheet = sheet;
        volumes = new Volumes(sheet, false, false);
        data = new MomentData[sheet.size()];
        values = new RecurrentValues(sheet);
        initHelper();
    }

    public void doIt(){
        int count = 0, profit = 0;
        for (int i = 0;i<sheet.size();i++){
            volumes.calc(i);
            values.feed(i);
            ZonedDateTime time = sheet.bar(i).getBeginTime();

            if (time.getYear()>=2018) {
                data[i] = new MomentData(helper.size());
                if (play(i)) profit++;
                count++;

            }
        }
        System.out.println(String.format("profitable=%g", profit*1.0/count));
        Instances set = helper.makeSet(data, 0, Integer.MAX_VALUE,0,9);
        Exporter.string2file("d:/weka/test.arff",set.toString());
    }

    public void daysMatrix(){
        volumes = new Volumes(sheet, false, false);
        data = new MomentData[sheet.size()];
        values = new RecurrentValues(sheet);
        boolean[][] result = new boolean[10][10];



    }

    public double[] dayByDay(int trainDays, boolean prediction) throws Exception {
        volumes = new Volumes(sheet, false, false);
        values = new RecurrentValues(sheet);
        data = new MomentData[sheet.size()];
        double[] result = new double[sheet.size()];
        int i = 0;
        ZonedDateTime start = sheet.bar(0).getBeginTime();
        ZonedDateTime calcStart = null;
        Classifier classifier = new Logistic();
        LogisticImprover improver = null;
        int learnFrom;
        Instances test = null;
        for (;i<sheet.size();i++) {
            volumes.calc(i);
            values.feed(i);
            if (i < 1000) continue;
            if (calcStart == null)
                calcStart = sheet.bar(i).getBeginTime();
            data[i] = new MomentData(helper.size());
            play(i);
            if (!sheet.bar(i).getBeginTime().minusDays(trainDays).isAfter(calcStart)) continue;
            if (start.plusDays(trainDays + 1).isBefore(sheet.bar(i).getBeginTime())) {
                start = sheet.bar(i).getBeginTime().minusDays(trainDays);
                learnFrom = sheet.getBarIndex(start);
                Instances set = helper.makeSet(data, learnFrom, i,0,9);
                improver = new LogisticImprover(set, null, 2);
                classifier.buildClassifier(improver.prepare(set));
                test = improver.prepare(helper.makeEmptySet(0,9));
            }
            Instance instance = improver.prepareInstance(helper.makeInstance(data[i],0,9).toDoubleArray());
            test.add(instance);
            instance.setDataset(test);
            boolean b = classifier.classifyInstance(instance) > 0.5;
            if (prediction)
                result[i] =  b?1:-1;
            else
                result[i] = b==instance.value(instance.numAttributes() - 1) > 0.5 ? 1 : -1;
        }

        return result;
    }

    public double testDay(int day, int train, int skip, int exam){
        volumes = new Volumes(sheet, false, false);
        data = new MomentData[sheet.size()];
        values = new RecurrentValues(sheet);
        int count = 0, profit = 0;
        int i = 0;
        for (;i<sheet.size();i++){
            volumes.calc(i);
            values.feed(i);
            ZonedDateTime time = sheet.bar(i).getBeginTime();

            if (time.getYear()>=2018 && time.getDayOfYear()>=day-train-skip) {
                data[i] = new MomentData(helper.size());
                if (play(i)) profit++;
                count++;
            }
            if (time.getYear()>=2018 && time.getDayOfYear()>=day-skip) break;
        }

        Instances set = helper.makeSet(data, 0, i,0,9);
        Instances testSet = helper.makeEmptySet(0,9);
        double result = 0;

        try {
            count = 0;profit = 0;
            for (;i<sheet.size();i++){
                volumes.calc(i);
                values.feed(i);
                data[i] = new MomentData(helper.size());
                ZonedDateTime time = sheet.bar(i).getBeginTime();
                if (data[i].whenWillKnow<sheet.size() && time.getYear()>=2018 && time.getDayOfYear()>=day) {
                    count++;
                    if (play(i))
                        profit++;
                    testSet.add(helper.makeInstance(data[i],0,9));
                }
                if (time.getYear()>=2018 && time.getDayOfYear()>day+exam) break;
            }
            ArrayList<LogisticImprover.WhatToAddResult> results = new LogisticImprover(set, testSet, 2).doIt(false);
            for (int j = 0;j<10;j++)
                result += results.get(j).examKappa;
//            System.out.println(String.format("profitable=%g", profit*1.0/count));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void learnAndPlay(){
        int count = 0, profit = 0;
        int i = 0;
        System.out.println("start playing bars...");
        for (;i<sheet.size();i++){
            volumes.calc(i);
            values.feed(i);
            ZonedDateTime time = sheet.bar(i).getBeginTime();

            if (time.getYear()>=2018) {
                data[i] = new MomentData(helper.size());
                if (play(i)) profit++;
                count++;
            }
            if (time.getYear()>=2018 && time.getDayOfYear()>110) break;
        }
        System.out.println("will make classifier now...");
        Instances set = helper.makeSet(data, 0, i,0,9);
        Instances testSet = helper.makeEmptySet(0,9);

        try {
            count = 0;profit = 0;
            for (;i<sheet.size();i++){
                volumes.calc(i);
                values.feed(i);
                data[i] = new MomentData(helper.size());
                if (data[i].whenWillKnow<sheet.size()) {
                    count++;
                    if (play(i))
                        profit++;
                    testSet.add(helper.makeInstance(data[i],0,9));
                }
                ZonedDateTime time = sheet.bar(i).getBeginTime();
                if (time.getYear()>=2018 && time.getDayOfYear()>140) break;
            }
            System.out.println(String.format("train set: %d, exam set: %d (%d/%d)", set.size(),testSet.size(),profit,(count-profit)));
            new LogisticImprover(set,testSet,2).doIt(true);
//            System.out.println(String.format("profitable=%g", profit*1.0/count));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initHelper() {
        helper = new MomentDataHelper();
        helper.register("toAvgSd");
        helper.register("sd");
        helper.register("toAvgSd2");
        helper.register("sd2");
        helper.register("macd1");
        helper.register("macd2");
        helper.register("stoh1");
        helper.register("stoh2");
//        helper.register("rising",true);
//        helper.register("falling",true);
        helper.register("integralUp");
        helper.register("integralDown");
//        helper.register("hasLevelUp",true);
//        helper.register("hasLevelDown",true);
        helper.register("ballanceOnBuy1");
        helper.register("ballanceOnBuy2");
        helper.register("ballanceOnTarget1");
        helper.register("ballanceOnTarget2");
        helper.register("ballanceOnSl1");
        helper.register("ballanceOnSl2");
        helper.register("toAvg1");
        helper.register("toAvg2");
        helper.register("toAvg3");
//        helper.register("hour");
//        helper.register("day");
//        helper.register("r1");
//        helper.register("r2");
//        helper.register("r3");
//        helper.register("holiday", true);
//        helper.register("monday",true);
//        helper.register("friday",true);
        helper.register("volumeBurst");
//        helper.register("slope1_min");
//        helper.register("slope1_minerr");
//        helper.register("slope1_minto");
//        helper.register("slope1_max");
//        helper.register("slope1_maxerr");
//        helper.register("slope1_maxto");
//        helper.register("slope1_minmax");
//
//        helper.register("slope2_min");
//        helper.register("slope2_minerr");
//        helper.register("slope2_minto");
//        helper.register("slope2_max");
//        helper.register("slope2_maxerr");
//        helper.register("slope2_maxto");
//        helper.register("slope2_minmax");
//        helper.register("slope1_er");
//        helper.register("slope1_conf");
//        helper.register("slope1_minus");
//        helper.register("slope1_plus");
//        helper.register("slope1_up",true);
//        helper.register("slope1_down",true);
//        helper.register("slope1_upc",true);
//        helper.register("slope1_downc",true);
//
//        helper.register("slope2");
//        helper.register("slope2_er");
//        helper.register("slope2_conf");
//        helper.register("slope2_minus");
//        helper.register("slope2_plus");
//        helper.register("slope2_up",true);
//        helper.register("slope2_down",true);
//        helper.register("slope2_upc",true);
//        helper.register("slope2_downc",true);
//
//        helper.register("slope3");
//        helper.register("slope3_er");
//        helper.register("slope3_conf");
//        helper.register("slope3_minus");
//        helper.register("slope3_plus");
//        helper.register("slope3_up",true);
//        helper.register("slope3_down",true);
//        helper.register("slope3_upc",true);
//        helper.register("slope3_downc",true);



        helper.register("_profit",true);

//        helper.register("_profit1");
//        helper.register("_profit2");
//        helper.register("_profit3");
//        helper.register("_profit4");
//        helper.register("_profit5");
//        helper.register("_profit6");
//
//        helper.register("upper");
//        helper.register("upper2");
//        helper.register("upper3");
//        helper.register("lower");
//        helper.register("lower2");
//        helper.register("lower3");
//
//        helper.register("macd1");
//        helper.register("macd2");
//
//        helper.register("stoh1");
//        helper.register("stoh2");
//
//        helper.register("gAvgSd");
//
//        helper.register("integralDown1");
//        helper.register("integralDown2");
//        helper.register("integralDown3");
//        helper.register("integralUp1");
//        helper.register("integralUp2");
//        helper.register("integralUp3");
    }

    private boolean play(int index) {
        PlayerMoment moment = new PlayerMoment(index);
        moment.saveData();
        return moment.profit;
    }

    private class PlayerMoment {
        int index;
        double buyPrice;

        double target,sl;
        boolean profit;

        double[] stopLosses, sellPrices;
        int[] beforeStopLoss;
        boolean[] stopLossRiched;
        int upperlevel, upperlevel2, upperlevel3, lowerlevel, lowerlevel2, lowerlevel3, lowerlevel4;

        double lowerPrice, upperPrice;
        int willKnow;


        public PlayerMoment(int index) {
            this.index = index;
            XBar bar = sheet.bar(index);
            buyPrice = bar.getClosePrice();
            target = buyPrice*1.02;
            sl = buyPrice*0.96;

            Pair<Boolean, Integer> profit = sheet.willBeProfit(index + 1, target, sl,index+300);
            willKnow = profit.getSecond();
            this.profit = profit.getFirst();

//            int powClose = volumes.price2pow(bar.getClosePrice());

//            Pair<double[], double[]> vv = volumes.get();
//            double[] v = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
//            int[] levels = VecUtils.listLevels(v, 36, 12);
//            double[] integrals = VecUtils.integrals.stream().mapToDouble(Double::doubleValue).toArray();
//            int lowerIndex = VecUtils.findBaseInLevels(levels, powClose);
//            lowerPrice = volumes.pow2price(levels[lowerIndex]);
//            upperPrice = volumes.pow2price(levels[lowerIndex+1]);

        }

        Random random = new Random();
        public void saveData() {
            helper.put(data[index],"toAvgSd",((buyPrice-values.gustosAvg.value())/values.gustosAvg.sd()));
            helper.put(data[index],"sd",values.gustosAvg.sd()/buyPrice*10);
            helper.put(data[index],"toAvgSd2",((buyPrice-values.gustosAvg2.value())/values.gustosAvg2.sd()));
            helper.put(data[index],"sd2",values.gustosAvg2.sd()/buyPrice*10);
            helper.put(data[index],"macd1",values.macd1.value());
            helper.put(data[index],"macd2",values.macd2.value());
            helper.put(data[index],"stoh1",values.stoh1.percent());
            helper.put(data[index],"stoh2",values.stoh2.percent());
//            helper.put(data[index],"rising",sheet.bar(index-1).getClosePrice() < sheet.bar(index).getMinPrice()?1:0);
//            helper.put(data[index],"falling",sheet.bar(index-1).getClosePrice() >= sheet.bar(index).getMaxPrice()?1:0);
            volumes.prepareForIntegral();
            helper.put(data[index],"integralUp",volumes.integral(buyPrice,target));
            helper.put(data[index],"integralDown",volumes.integral(sl,buyPrice));
//            helper.put(data[index],"hasLevelUp",upperPrice<target?1.0:0);
//            helper.put(data[index],"hasLevelDown",lowerPrice>sl?1.0:0);
            helper.put(data[index],"ballanceOnBuy1", SheetUtils.volumesAroundLevel(sheet, buyPrice, index, 100));
            helper.put(data[index],"ballanceOnBuy2", SheetUtils.volumesAroundLevel(sheet, buyPrice, index, 500));
            helper.put(data[index],"ballanceOnTarget1", SheetUtils.volumesAroundLevel(sheet, target, index, 100));
            helper.put(data[index],"ballanceOnTarget2", SheetUtils.volumesAroundLevel(sheet, target, index, 500));
            helper.put(data[index],"ballanceOnSl1", SheetUtils.volumesAroundLevel(sheet, sl, index, 100));
            helper.put(data[index],"ballanceOnSl2", SheetUtils.volumesAroundLevel(sheet, sl, index, 500));

            helper.put(data[index],"toAvg1",((buyPrice-values.ema1.value())/buyPrice));
            helper.put(data[index],"toAvg2",((buyPrice-values.ema2.value())/buyPrice));
            helper.put(data[index],"toAvg3",((buyPrice-values.ema3.value())/buyPrice));

//            helper.put(data[index],"hour",sheet.bar(index).getBeginTime().getHour());
//            helper.put(data[index],"day",sheet.bar(index).getBeginTime().getDayOfWeek().getValue());
//            helper.put(data[index],"holiday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SUNDAY||sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SATURDAY?1:0);
//            helper.put(data[index],"monday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.MONDAY?1:0);
//            helper.put(data[index],"friday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.FRIDAY?1:0);
//            helper.put(data[index],"r1", random.nextDouble());
//            helper.put(data[index],"r2", random.nextDouble()*2-1);
//            helper.put(data[index],"r3", random.nextDouble()*2);

            helper.put(data[index],"volumeBurst",values.volumeShort.value()/values.volumeLong.value());

//            SimpleRegression smin = PredictLinear.optimums(sheet, index, 30,-1,2);
//            helper.put(data[index],"slope1_min", smin.getSlope());
//            helper.put(data[index],"slope1_minerr", smin.getSlopeStdErr());
//            helper.put(data[index],"slope1_minto", (buyPrice-smin.predict(index))/buyPrice);
//            SimpleRegression smax = PredictLinear.optimums(sheet, index, 30,1,2);
//            helper.put(data[index],"slope1_max", smax.getSlope());
//            helper.put(data[index],"slope1_maxerr", smax.getSlopeStdErr());
//            helper.put(data[index],"slope1_maxto", (buyPrice-smax.predict(index))/buyPrice);
//            helper.put(data[index],"slope1_minmax", (buyPrice-smin.predict(index))/(smax.predict(index)-smin.predict(index)));
//
//            smin = PredictLinear.optimums(sheet, index, 250,-1,2);
//            helper.put(data[index],"slope2_min", smin.getSlope());
//            helper.put(data[index],"slope2_minerr", smin.getSlopeStdErr());
//            helper.put(data[index],"slope2_minto", (buyPrice-smin.predict(index))/buyPrice);
//            smax = PredictLinear.optimums(sheet, index, 250,1,2);
//            helper.put(data[index],"slope2_max", smax.getSlope());
//            helper.put(data[index],"slope2_maxerr", smax.getSlopeStdErr());
//            helper.put(data[index],"slope2_maxto", (buyPrice-smax.predict(index))/buyPrice);
//            helper.put(data[index],"slope2_minmax", (buyPrice-smin.predict(index))/(smax.predict(index)-smin.predict(index)));

//            helper.put(data[index],"slope1_er", r.getSlopeStdErr());
//            helper.put(data[index],"slope1_conf", r.getSlopeConfidenceInterval());
//            helper.put(data[index],"slope1_minus", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope1_plus", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope1_up", r.getSlope()>0.1?1:0);
//            helper.put(data[index],"slope1_down", r.getSlope()<-0.1?1:0);
//            helper.put(data[index],"slope1_upc", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr()>0.1?1:0);
//            helper.put(data[index],"slope1_downc", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr()<-0.1?1:0);
//
//            r = PredictLinear.whereWillGo(sheet, index, 50);
//            helper.put(data[index],"slope2", r.getSlope());
//            helper.put(data[index],"slope2_er", r.getSlopeStdErr());
//            helper.put(data[index],"slope2_conf", r.getSlopeConfidenceInterval());
//            helper.put(data[index],"slope2_minus", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope2_plus", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope2_up", r.getSlope()>0.1?1:0);
//            helper.put(data[index],"slope2_down", r.getSlope()<-0.1?1:0);
//            helper.put(data[index],"slope2_upc", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr()>0.1?1:0);
//            helper.put(data[index],"slope2_downc", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr()<-0.1?1:0);
//
//            r = PredictLinear.whereWillGo(sheet, index, 300);
//            helper.put(data[index],"slope3", r.getSlope());
//            helper.put(data[index],"slope3_er", r.getSlopeStdErr());
//            helper.put(data[index],"slope3_conf", r.getSlopeConfidenceInterval());
//            helper.put(data[index],"slope3_minus", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope3_plus", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr());
//            helper.put(data[index],"slope3_up", r.getSlope()>0.1?1:0);
//            helper.put(data[index],"slope3_down", r.getSlope()<-0.1?1:0);
//            helper.put(data[index],"slope3_upc", r.getSlope()-r.getSlopeConfidenceInterval()*r.getSlopeStdErr()>0.1?1:0);
//            helper.put(data[index],"slope3_downc", r.getSlope()+r.getSlopeConfidenceInterval()*r.getSlopeStdErr()<-0.1?1:0);


            helper.put(data[index],"_profit",profit?1.0:0);


            data[index].whenWillKnow = willKnow;
        }
    }

    public static void main(String[] args) throws Exception {
        DataPlayer dp = new DataPlayer(TestUtils.makeSheet("indicators_simple.json", new Instrument("BTC", "USDT")));
        dp.learnAndPlay();
//        System.out.println("train 4+(0..10) exam 3");
//        ArrayList<Double> result = new ArrayList<>();
//        for (int skip = 0;skip<10;skip++) {
//            for (int day = 33; day < 160; day++) {
//                double r = dp.testDay(day, 4+skip, 0, 3);
//                result.add(r);
//                if (day!=33)
//                    System.out.print(",");
//                System.out.print(r);
//            }
//            System.out.println();
//        }
//        dp.learnAndPlay();
//        new DataPlayer(TestUtils.makeSheet("indicators_simple.json", new Instrument("BTC", "USDT"))).learnAndPlay();

//        new DataPlayer(TestUtils.makeSheet("indicators_simple.json", new Instrument("BCC", "USDT"))).doIt();
//        new DataPlayer(TestUtils.makeSheet("indicators_simple.json", new Instrument("BNB", "USDT"))).doIt();
    }
}
