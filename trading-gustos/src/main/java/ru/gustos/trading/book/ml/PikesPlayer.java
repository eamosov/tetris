package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.Volumes;
import ru.gustos.trading.book.indicators.VecUtils;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.Logistic;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PikesPlayer {

    public static void play(Sheet sheet) {
        Volumes volumes = new Volumes(sheet, false);
        Instances all = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
        Instances mldata = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
        Instances test = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
        mldata.setClassIndex(mldata.numAttributes() - 1);
        test.setClassIndex(mldata.numAttributes() - 1);
        boolean money = true;
        double gotmoney = 1;
        int good = 0, bad = 0;
        boolean classifiedOk = false;
        Classifier classifier = null;

        PikePlayerMoment moment = null;

        for (int i = 0; i < sheet.size(); i++) {
            volumes.calc(i);
            if (i<5000) continue;
            XBar bar = sheet.bar(i);
            if (!(bar.getBeginTime().getMonthValue()>=2 && bar.getBeginTime().getYear()>=2018))continue;
            if (money) {
                if (moment != null && moment.buyPrice >= bar.getMinPrice() && moment.buyPrice <= bar.getMaxPrice()) {
                    money = false;
                    classifiedOk = moment.classify(classifier, test) && bar.getBeginTime().getMonthValue()>=4 && bar.getBeginTime().getYear()>=2018;
                } else {
                    if (i%(60*24)==0)
                        System.out.println(i);
                    moment = new PikePlayerMoment(sheet, volumes, i);

                }
            } else {
                if (moment.stopLoss >= bar.getMinPrice()) {
                    money = true;
                    moment.bad();
                    if (classifiedOk) {
                        gotmoney *= moment.stopLoss / moment.buyPrice * 0.999;
                        bad++;
                    }
                } else if (moment.sellPrice <= bar.getMaxPrice()) {
                    money = true;
                    moment.good();
                    if (classifiedOk) {
                        gotmoney *= moment.sellPrice / moment.buyPrice * 0.999;
                        good++;
                    }
                }
                if (money) {
                    if (classifiedOk)
                        System.out.println(gotmoney+" "+bar.getBeginTime());
                    moment.addInstance(mldata);
                    moment.addInstance(all);
                    while (mldata.size()>32)
                        mldata.remove(0);

                    if (mldata.size() > 30)
                        try {
                            classifier = new Logistic();
                            classifier.buildClassifier(mldata);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    moment = null;

                }

            }


        }
        System.out.println(String.format("pertrade: %g, trades: %d, good %g", Math.pow(gotmoney, 1.0 / (good + bad)), good + bad, good * 1.0 / (good + bad)));
        Exporter.string2file("d:/tetrislibs/wekadata/pikes.arff",all.toString());

    }
}

class PikePlayerMoment {
    double buyPrice;
    double sellPrice;
    double stopLoss;
    double[] data;

    public PikePlayerMoment(Sheet sheet, Volumes volumes, int index) {
        XBar bar = sheet.bar(index);
        int powClose = volumes.price2pow(bar.getClosePrice());
        int powOpen = volumes.price2pow(bar.getOpenPrice());
        int powMin = volumes.price2pow(bar.getMinPrice());
        int powMax = volumes.price2pow(bar.getMaxPrice());
        Pair<double[], double[]> vv = volumes.getVolumes();
        double[] v = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
        List<Integer> levels = VecUtils.listLevels(v, 6);
        int levelIndex = VecUtils.findBaseInLevels(levels,powClose);
        if (levelIndex==0){
            buyPrice = 0;
            return;
        }
        data = new double[16];

        int level = levels.get(levelIndex);
        int upperlevel = VecUtils.nextLevel(levels,levelIndex,1);
            int upperlevel2 = VecUtils.nextLevel(levels,levelIndex,2);
            int upperlevel3 = VecUtils.nextLevel(levels,levelIndex,3);
            int lowerlevel = VecUtils.nextLevel(levels,levelIndex,-1);
            // стоплос надо ставить на самом деле не на предыдущий там снизу уровень, а немного под ближним снизу
            data[0] = v[upperlevel] / v[powClose];
            data[1] = v[upperlevel2] / v[powClose];
            data[2] = v[upperlevel3] / v[powClose];
            data[3] = level <= 0 ? 0 : v[level] / v[powClose];
            data[4] = lowerlevel <= 0 ? 0 : v[lowerlevel] / v[powClose];
            data[5] = upperlevel - powClose;
            data[6] = upperlevel2 - powClose;
            data[7] = upperlevel3 - powClose;
            data[8] = level - powClose;
            data[9] = lowerlevel - powClose;
            int up1 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel));
            int down1 = sheet.whenPriceWas(index-1,buyPrice);
            int up2 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel2));
            int up3 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel3));
            int down2 = sheet.whenPriceWas(index-1,volumes.pow2price(lowerlevel));
//            data[8] = 0;
//            data[9] = 0;
//            data[10] = 0;
//            data[11] = 0;
            data[10] = up1>down1 && up1>down2?1:0;
            data[11] = up2>down1 && up2>down2?1:0;
            data[12] = up3>down1 && up3>down2?1:0;
            data[13] = down1>up1 && down1>up2?1:0;
            data[14] = down2>up1 && down2>up2?1:0;

//            if (up2<down2) {
                buyPrice = volumes.pow2price(lowerlevel);
                sellPrice = volumes.pow2price(upperlevel);
//                sellPrice = buyPrice + (sellPrice - buyPrice) * 0.9;
                if (sellPrice / buyPrice > 1.1) sellPrice = buyPrice * 1.1;
                stopLoss = volumes.pow2price(level-2);
//                stopLoss = volumes.pow2price((lowerlevel+level)/2);
                if (buyPrice / stopLoss > 1.1) stopLoss = buyPrice / 1.1;
//            } else {
//                buyPrice = 0;
//            }

//                        ml[8] = sheet.whenPriceWas(i,sellPrice);
//                        ml[9] = sheet.whenPriceWas(i,stoploss);

//                        stoploss = buyPrice + (stoploss-buyPrice)*1.1;


    }

    public void good() {
        data[data.length - 1] = 1;
    }

    public void bad() {
        data[data.length - 1] = 0;
    }

    public void addInstance(Instances mldata) {
        if (PikesAnalyzer.check(data)) {
//            data[0] = 0;
//            data[2] = 0;
            DenseInstance instance = new DenseInstance(1, data);
            mldata.add(instance);
        }
    }

    public boolean classify(Classifier classifier, Instances testset) {
        if (classifier == null) return false;
        if (!PikesAnalyzer.check(data)) return false;
        try {
//            data[0] = 0;
//            data[2] = 0;
            DenseInstance instance = new DenseInstance(1, data);
            testset.clear();
            instance.setDataset(testset);
            double[] distr = classifier.distributionForInstance(instance);
//            System.out.println(Arrays.toString(distr));
            return distr[1] > 0.55;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



}
// class PikePlayerMoment {
//    double buyPrice;
//    double sellPrice;
//    double stopLoss;
//    double[] data;
//
//    public PikePlayerMoment(Sheet sheet, Volumes volumes, int index) {
//        XBar bar = sheet.bar(index);
//        int powClose = volumes.price2pow(bar.getClosePrice());
//        int powOpen = volumes.price2pow(bar.getOpenPrice());
//        int powMin = volumes.price2pow(bar.getMinPrice());
//        int powMax = volumes.price2pow(bar.getMaxPrice());
//        Pair<double[], double[]> vv = volumes.getVolumes();
//        double[] v = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
//        v = VecUtils.ma(v,2);
//        double eps = 0.04;
//        int level = VecUtils.goToChange(v, powClose, -1, eps);
//                    level = VecUtils.goToChange(v,level,-1,eps);
//        data = new double[16];
//        int upperlevel = VecUtils.goToChange(v, powClose, 1, eps);
//        if (upperlevel - level > 1) {
//            if (upperlevel >= Volumes.steps-1)
//                upperlevel = Math.min(Volumes.steps-1,level + 10);
//            int upperlevel2 = VecUtils.goToChange(v, upperlevel + 2, 1, eps);
//            if (upperlevel2 >= Volumes.steps-1)
//                upperlevel2 = Math.min(Volumes.steps-1,upperlevel + 10);
//            int upperlevel3 = VecUtils.goToChange(v, upperlevel2 + 2, 1, eps);
//            if (upperlevel3 >= Volumes.steps-1)
//                upperlevel3 = Math.min(Volumes.steps-1,upperlevel2 + 10);
//            buyPrice = volumes.pow2price(level);
//            sellPrice = volumes.pow2price(upperlevel2);
//            sellPrice = buyPrice + (sellPrice - buyPrice) * 0.9;
//            if (sellPrice / buyPrice > 1.1) sellPrice = buyPrice * 1.1;
//            int lowerlevel = VecUtils.goToChange(v, level - 2, -1, eps);
//            stopLoss = volumes.pow2price(lowerlevel);
//            if (buyPrice / stopLoss > 1.1) stopLoss = buyPrice / 1.1;
//
//            data[0] = v[upperlevel] / v[powClose];
//            data[1] = v[upperlevel2] / v[powClose];
//            data[2] = v[upperlevel3] / v[powClose];
//            data[3] = level <= 0 ? 0 : v[level] / v[powClose];
//            data[4] = lowerlevel <= 0 ? 0 : v[lowerlevel] / v[powClose];
//            data[5] = upperlevel - powClose;
//            data[6] = upperlevel2 - powClose;
//            data[7] = upperlevel3 - powClose;
//            data[8] = level - powClose;
//            data[9] = lowerlevel - powClose;
//            int up1 = sheet.whenPriceWas(index,volumes.pow2price(upperlevel));
//            int up2 = sheet.whenPriceWas(index,volumes.pow2price(upperlevel2));
//            int up3 = sheet.whenPriceWas(index,volumes.pow2price(upperlevel3));
//            int down1 = sheet.whenPriceWas(index,buyPrice);
//            int down2 = sheet.whenPriceWas(index,volumes.pow2price(lowerlevel));
////            data[8] = 0;
////            data[9] = 0;
////            data[10] = 0;
////            data[11] = 0;
//            data[10] = up1>down1 && up1>down2?1:0;
//            data[11] = up2>down1 && up2>down2?1:0;
//            data[12] = up3>down1 && up3>down2?1:0;
//            data[13] = down1>up1 && down1>up2?1:0;
//            data[14] = down2>up1 && down2>up2?1:0;
//
////                        ml[8] = sheet.whenPriceWas(i,sellPrice);
////                        ml[9] = sheet.whenPriceWas(i,stoploss);
//
////                        stoploss = buyPrice + (stoploss-buyPrice)*1.1;
//        } else {
//            buyPrice = 0;
//        }
//
//
//    }
//
//    public void good() {
//        data[data.length - 1] = 1;
//    }
//
//    public void bad() {
//        data[data.length - 1] = 0;
//    }
//
//    public void addInstance(Instances mldata) {
//        if (PikesAnalyzer.check(data)) {
////            data[0] = 0;
////            data[2] = 0;
//            DenseInstance instance = new DenseInstance(1, data);
//            mldata.add(instance);
//        }
//    }
//
//    public boolean classify(Classifier classifier, Instances testset) {
//        if (classifier == null) return false;
//        if (!PikesAnalyzer.check(data)) return false;
//        try {
////            data[0] = 0;
////            data[2] = 0;
//            DenseInstance instance = new DenseInstance(1, data);
//            testset.clear();
//            instance.setDataset(testset);
//            double[] distr = classifier.distributionForInstance(instance);
////            System.out.println(Arrays.toString(distr));
//            return distr[1] > 0.55;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//}