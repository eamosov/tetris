package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.RecurrentValues;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.Volumes;
import ru.gustos.trading.book.indicators.*;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static ru.gustos.trading.book.ml.PikesPlayer.att;

public class PikesPlayer {


    public static Instances play(Sheet sheet) {
        Volumes volumes = new Volumes(sheet, false, false);
        Instances all = new Instances("data", makeAttributes(), 10);
        Instances mldata = new Instances("data", makeAttributes(), 10);
        Instances test = new Instances("data", makeAttributes(), 10);
        mldata.setClassIndex(mldata.numAttributes() - 1);
        test.setClassIndex(mldata.numAttributes() - 1);
        boolean money = true;
        double gotmoney = 1;
        double richgot = 1;
        int good = 0, bad = 0;
        int goodtotal = 0, badtotal = 0;
        Classifier classifier = null;
        LogisticImprover improver = null;
        double firstPrice = 0;
        double lastPrice = 0;

        RecurrentValues data = new RecurrentValues(sheet);
        ArrayList<PikePlayerMoment> moments = new ArrayList<>();
        double got = 0;

        PikePlayerMoment moment = null;
        int badbad = 0;

        for (int i = 0; i < sheet.size(); i++) {
            data.feed(i);
            volumes.calc(i);
            if (i < 5000) continue;
            XBar bar = sheet.bar(i);
            if (!(bar.getBeginTime().getMonthValue() >= 3 && bar.getBeginTime().getYear() >= 2018)) continue;
            for (PikePlayerMoment m : moments)
                m.correct(bar);
            for (PikePlayerMoment m : moments)
                m.checks(bar);


            if (moment != null && moment.buyPrice >= bar.getMinPrice() && moment.buyPrice <= bar.getMaxPrice()) {
                moments.add(moment);
                moment = null;
                if (firstPrice == 0)
                    firstPrice = moment.buyPrice;
            } else {
                moment = new PikePlayerMoment(sheet, volumes, i, all.numAttributes(), data);
                if (moments.size() > 0 && Math.abs(moment.buyPrice / moments.get(moments.size() - 1).buyPrice - 1) < 0.005)
                    moment = null;
            }


//            if (!money) {
//                double w = 1;
//                got = 0;
//                lastPrice = bar.getClosePrice();
//                if (moment.stopLoss >= bar.getMinPrice()) {
//                    if (moment.stopLoss > bar.getMaxPrice()) moment.stopLoss = bar.getMaxPrice();
//                    money = true;
//                    moment.bad();
//                    w = moment.buyPrice / moment.stopLoss - 1;
//                    if (classifiedOk && badbad >= 0) {
//                        got = moment.stopLoss / moment.buyPrice * 0.999;
//                        gotmoney *= got;
//                        bad++;
////                        System.out.println("stoploss "+(moment.stopLoss / moment.buyPrice * 0.999));
//                    }
//                    badbad = -2;
//                    badtotal++;
//                } else if (!justbuy && moment.sellPrice <= bar.getMaxPrice()) {
//                    if (moment.sellPrice < bar.getMinPrice())
//                        moment.sellPrice = bar.getMinPrice();
//                    money = true;
//                    moment.good();
//                    w = moment.sellPrice / moment.buyPrice - 1;
//                    if (classifiedOk && badbad >= 0) {
//                        got = moment.sellPrice / moment.buyPrice * 0.999;
//                        gotmoney *= got;
//                        good++;
//                    }
//                    badbad++;
//                    goodtotal++;
//                }
//                if (money) {
//                    richgot *= moment.richbackprofit;
//                    if (got != 0)
//                        System.out.println(gotmoney + " " + bar.getBeginTime() + " " + (bar.getEndTime().toEpochSecond() - moment.buyTime.toEpochSecond()) / 60 + " got: " + (got - 1) * 100 + "%");
//                    moment.dataForAnalyze();
//                    moment.addInstance(mldata, true, w);
//                    moment.addInstance(all, false, w);
//
////                    while (mldata.size()>62)
////                        mldata.remove(0);
//
//                    if (mldata.size() > 100)
//                        try {
////                            System.out.println("prepare classification");
//                            improver = new LogisticImprover(mldata, 1);
////                            improver.doIt();
//                            test = new Instances("test", improver.makeAttributes(), 1);
////                            classifier = new Logistic();
////                            classifier = new RandomForest();
////                            classifier.buildClassifier(improver.prepare());
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                    moment = null;
//
//                }


//            }


        }
        System.out.println(String.format("pertrade: %g, togrow: %g, trades: %d, good %g, total %g of %d", Math.pow(gotmoney, 1.0 / (good + bad)), Math.pow(gotmoney / (lastPrice / firstPrice), 1.0 / (good + bad)), good + bad, good * 1.0 / (good + bad), goodtotal * 1.0 / (goodtotal + badtotal), goodtotal + badtotal));
//        System.out.println("richgot "+richgot);
        Exporter.string2file("d:/tetrislibs/wekadata/pikes.arff", all.toString());
        return all;
    }

    static ArrayList<Attribute> attributes;

    public static int att(String name) {
        ArrayList<Attribute> attributes = makeAttributes();
        for (int i = 0; i < attributes.size(); i++)
            if (attributes.get(i).name().equals(name)) return i;
        return -1;
    }

    public static ArrayList<Attribute> makeAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
            attributes.add(new Attribute("upper"));
            attributes.add(new Attribute("upper2"));
            attributes.add(new Attribute("upper3"));
            attributes.add(new Attribute("lower"));
            attributes.add(new Attribute("lower2"));
            attributes.add(new Attribute("lower3"));
            attributes.add(new Attribute("toUpper"));
            attributes.add(new Attribute("toUpper2"));
            attributes.add(new Attribute("toUpper3"));
            attributes.add(new Attribute("toLower"));
            attributes.add(new Attribute("toLower2"));
            attributes.add(new Attribute("toLower3"));
            attributes.add(new Attribute("fromUp1"));
            attributes.add(new Attribute("fromUp2"));
            attributes.add(new Attribute("fromUp3"));
            attributes.add(new Attribute("fromDown1"));
            attributes.add(new Attribute("fromDown2"));
            attributes.add(new Attribute("fromDown3"));
            attributes.add(new Attribute("integralDown1"));
            attributes.add(new Attribute("integralDown2"));
            attributes.add(new Attribute("integralDown3"));
            attributes.add(new Attribute("integralUp1"));
            attributes.add(new Attribute("integralUp2"));
            attributes.add(new Attribute("integralUp3"));

            attributes.add(new Attribute("integralDownSum2"));
            attributes.add(new Attribute("integralDownSum3"));
            attributes.add(new Attribute("integralUpSum2"));
            attributes.add(new Attribute("integralUpSum3"));

            attributes.add(new Attribute("toAvg"));
            attributes.add(new Attribute("toAvg2"));
            attributes.add(new Attribute("toAvg3"));

            attributes.add(new Attribute("toGustosAvg"));
            attributes.add(new Attribute("toGustosAvg2"));

//        res.add(new Attribute("toGustosBigAvg"));
//        res.add(new Attribute("toGustosBigAvg2"));

            attributes.add(new Attribute("stoh1"));
            attributes.add(new Attribute("stoh2"));

            attributes.add(new Attribute("macd1"));
            attributes.add(new Attribute("macd2"));

            attributes.add(new Attribute("balancetolower1"));
            attributes.add(new Attribute("balancetolower2"));

            attributes.add(new Attribute("balancetoupper1"));
            attributes.add(new Attribute("balancetoupper2"));

            attributes.add(new Attribute("gustosupper"));
            attributes.add(new Attribute("gustosupper2"));
            attributes.add(new Attribute("gustosupper3"));
            attributes.add(new Attribute("gustoslower"));
            attributes.add(new Attribute("gustoslower2"));
            attributes.add(new Attribute("gustoslower3"));


            attributes.add(new Attribute("minprice"));
            attributes.add(new Attribute("minprice2"));
            attributes.add(new Attribute("maxprice"));
            attributes.add(new Attribute("maxprice2"));
            attributes.add(new Attribute("comeback"));
            attributes.add(new Attribute("comeback2"));

            attributes.add(new Attribute("profit", Arrays.asList("false", "true")));
        }

        return attributes;
    }


}

class PikePlayerMoment {
    private final Volumes volumes;
    double buyPrice;
    double sellPrice;
    double stopLoss;
    double initialStopLoss;
    double[] data;
    ZonedDateTime buyTime;
    double minprice;
    double maxprice;
    double maxprice2;
    int level, upperlevel, upperlevel2, upperlevel3, lowerlevel, lowerlevel2, lowerlevel3, lowerlevel4;
    int up1, up2, up3, down1, down2, down3;
    int[] levels;
    double[] integrals;
    double[] v;
    int levelIndex;
    int powClose;
    XBar bar;
    Sheet sheet;
    int index;

    double[] stopLosses, sellPrices;
    int[] beforeStopLoss;
    boolean[] stopLossRiched;

    public PikePlayerMoment(Sheet sheet, Volumes volumes, int index, int datasize, RecurrentValues pdata) {
        this.sheet = sheet;
        this.index = index;
        this.volumes = volumes;
        bar = sheet.bar(index);
        powClose = volumes.price2pow(bar.getClosePrice());
        Pair<double[], double[]> vv = volumes.get();
        v = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
        levels = VecUtils.listLevels(v, 36, 12);
        integrals = VecUtils.integrals.stream().mapToDouble(Double::doubleValue).toArray();
        levelIndex = VecUtils.findBaseInLevels(levels, powClose);
        buyTime = bar.getEndTime();
        if (levelIndex == 0) {
            buyPrice = 0;
            return;
        }

        level = levels[levelIndex];
        upperlevel = VecUtils.nextLevel(levels, levelIndex, 1, volumes.steps);
        upperlevel2 = VecUtils.nextLevel(levels, levelIndex, 2, volumes.steps);
        upperlevel3 = VecUtils.nextLevel(levels, levelIndex, 3, volumes.steps);
        lowerlevel = VecUtils.nextLevel(levels, levelIndex, -1, volumes.steps);
        lowerlevel2 = VecUtils.nextLevel(levels, levelIndex, -2, volumes.steps);
        lowerlevel3 = VecUtils.nextLevel(levels, levelIndex, -3, volumes.steps);
        lowerlevel4 = VecUtils.nextLevel(levels, levelIndex, -4, volumes.steps);
        // стоплос надо ставить на самом деле не на предыдущий там снизу уровень, а немного под ближним снизу
        up1 = sheet.whenPriceWas(index - 1, volumes.pow2price(upperlevel));
        down1 = sheet.whenPriceWas(index - 1, volumes.pow2price(level));
        up2 = sheet.whenPriceWas(index - 1, volumes.pow2price(upperlevel2));
        up3 = sheet.whenPriceWas(index - 1, volumes.pow2price(upperlevel3));
        down2 = sheet.whenPriceWas(index - 1, volumes.pow2price(lowerlevel));
        down3 = sheet.whenPriceWas(index - 1, volumes.pow2price(lowerlevel2));


//        if (integrals[levelIndex] == 0 && integrals[levelIndex+2] == 0/* && up1 > down1 && up1 > down2*/) {
        buyPrice = volumes.pow2price(level);
        minprice = buyPrice;
        maxprice = buyPrice;
//            sellPrice = volumes.pow2price(level+135);
        sellPrice = volumes.pow2price(upperlevel2 - 5);
        sellPrices = new double[6];
        sellPrices[0] = volumes.pow2price((level+upperlevel)/2-3);
        sellPrices[1] = volumes.pow2price(upperlevel-3);
        sellPrices[2] = volumes.pow2price((upperlevel+upperlevel2)/2-3);
        sellPrices[3] = volumes.pow2price(upperlevel2-3);
        sellPrices[4] = volumes.pow2price((upperlevel2+upperlevel3)/2-3);
        sellPrices[5] = volumes.pow2price(upperlevel3-3);
//            sellPriceMid = volumes.pow2price(upperlevel);
//                sellPrice = buyPrice + (sellPrice - buyPrice) * 0.9;

//        if (sellPrice / buyPrice > 1.1) sellPrice = buyPrice * 1.1;

        stopLosses = new double[6];
        stopLosses[0] = volumes.pow2price(level-3);
        stopLosses[1] = volumes.pow2price((lowerlevel+level)/2-3);
        stopLosses[2] = volumes.pow2price(lowerlevel-3);
        stopLosses[3] = volumes.pow2price((lowerlevel+lowerlevel2)/2-3);
        stopLosses[4] = volumes.pow2price(lowerlevel2-3);
        stopLosses[5] = volumes.pow2price((lowerlevel2+lowerlevel3)/2-3);

        beforeStopLoss = new int[6];
        stopLossRiched = new boolean[6];
//            stopLoss = volumes.pow2price(level-160);
//        stopLoss = volumes.pow2price(lowerlevel2 - 3);
////                stopLoss = volumes.pow2price((lowerlevel+level)/2);
//        if (buyPrice / stopLoss > 1.1) stopLoss = buyPrice / 1.1;
//        initialStopLoss = stopLoss;
        makeData(datasize, pdata, volumes);

    }

    public void makeData(int datasize, RecurrentValues pdata, Volumes volumes) {
        data = new double[datasize];
        data[att("upper")] = v[upperlevel] / v[powClose];
        data[att("upper2")] = v[upperlevel2] / v[powClose];
        data[att("upper3")] = v[upperlevel3] / v[powClose];
        data[att("lower")] = level <= 0 ? 0 : v[level] / v[powClose];
        data[att("lower2")] = lowerlevel <= 0 ? 0 : v[lowerlevel] / v[powClose];
        data[att("lower3")] = lowerlevel2 <= 0 ? 0 : v[lowerlevel2] / v[powClose];
        data[att("toUpper")] = upperlevel - powClose;
        data[att("toUpper2")] = upperlevel2 - powClose;
        data[att("toUpper3")] = upperlevel3 - powClose;
        data[att("toLower")] = level - powClose;
        data[att("toLower2")] = lowerlevel - powClose;
        data[att("toLower3")] = lowerlevel2 - powClose;
        data[att("fromUp1")] = up1 > down1 && up1 > down2 ? 1 : 0;
        data[att("fromUp2")] = up2 > down1 && up2 > down2 ? 1 : 0;
        data[att("fromUp3")] = up3 > down1 && up3 > down2 ? 1 : 0;
        data[att("fromDown1")] = down1 > up1 && down1 > up2 ? 1 : 0;
        data[att("fromDown2")] = down2 > up1 && down2 > up2 ? 1 : 0;
        data[att("fromDown3")] = down3 > up1 && down2 > up2 ? 1 : 0;
        data[att("integralDown1")] = integrals[levelIndex];
        data[att("integralDown2")] = levelIndex < 1 ? 0 : integrals[levelIndex - 1];
        data[att("integralDown3")] = levelIndex < 2 ? 0 : integrals[levelIndex - 2];
        data[att("integralUp1")] = levelIndex + 1 >= integrals.length ? 0 : integrals[levelIndex + 1];
        data[att("integralUp2")] = levelIndex + 2 >= integrals.length ? 0 : integrals[levelIndex + 2];
        data[att("integralUp3")] = levelIndex + 3 >= integrals.length ? 0 : integrals[levelIndex + 3];
        data[att("integralDownSum2")] = data[att("integralDown1")] + data[att("integralDown2")];
        data[att("integralDownSum3")] = data[att("integralDownSum2")] + data[att("integralDown3")];
        data[att("integralUpSum2")] = data[att("integralUp1")] + data[att("integralUp2")];
        data[att("integralUpSum3")] = data[att("integralUpSum2")] + data[att("integralUp3")];
        data[att("toAvg")] = (bar.getClosePrice() / pdata.ema1.value() - 1) * 10;
        data[att("toAvg2")] = (bar.getClosePrice() / pdata.ema2.value() - 1) * 10;
        data[att("toAvg3")] = (bar.getClosePrice() / pdata.ema3.value() - 1) * 10;
        data[att("toGustosAvg")] = (bar.getClosePrice() / pdata.gustosAvg.value() - 1) * 10;
        data[att("toGustosAvg2")] = (bar.getClosePrice() / pdata.gustosAvg2.value() - 1) * 10;
        data[att("stoh1")] = pdata.stoh1.percent();
        data[att("stoh2")] = pdata.stoh2.percent();
        data[att("macd1")] = pdata.macd1.value();
        data[att("macd2")] = pdata.macd2.value();
        data[att("balancetolower1")] = SheetUtils.volumesAroundLevel(sheet, buyPrice, index, 30);
        data[att("balancetolower2")] = SheetUtils.volumesAroundLevel(sheet, buyPrice, index, 150);
        data[att("balancetoupper1")] = SheetUtils.volumesAroundLevel(sheet, sellPrice, index, 30);
        data[att("balancetoupper2")] = SheetUtils.volumesAroundLevel(sheet, sellPrice, index, 150);
//        Pair<double[], double[]> v = volumes.getGustosVolumes();
//        double[] vv = VecUtils.add(v.getFirst(),v.getSecond(),1);
//        data[att("gustosupper")] = vv[upperlevel] / vv[powClose];
//        data[att("gustosupper2")] = vv[upperlevel2] / vv[powClose];
//        data[att("gustosupper3")] = vv[upperlevel3] / vv[powClose];
//        data[att("gustoslower")] = level <= 0 ? 0 : vv[level] / vv[powClose];
//        data[att("gustoslower2")] = lowerlevel <= 0 ? 0 : vv[lowerlevel] / vv[powClose];
//        data[att("gustoslower3")] = lowerlevel2 <= 0 ? 0 : vv[lowerlevel2] / vv[powClose];
    }

    public void good() {
        data[data.length - 1] = 1;
    }

    public void bad() {
        data[data.length - 1] = 0;
    }

    public static boolean check(double[] v) {
//        if (v[att("upper")]>1.8) return false;
//        if (v[att("upper2")]>2) return false;
//        if (v[att("upper3")]>2.6) return false;
//        if (v[att("lower")]>2) return false;
//        if (v[att("lower2")]>2) return false;
//        if (v[att("lower3")]>2.5) return false;
//        if (Math.abs(v[att("toAvg")])>0.7) return false;
//        if (Math.abs(v[att("toAvg2")])>1.2) return false;
//        if (Math.abs(v[att("toAvg3")])>3.2) return false;
//        if (Math.abs(v[att("macd1")])>0.11) return false;
//        if (Math.abs(v[att("macd2")])>0.33) return false;
//
//        if (v[att("gustosupper")]>2.2) return false;
//        if (v[att("gustosupper2")]>2.2) return false;
//        if (v[att("gustosupper3")]>3.5) return false;
//        if (v[att("gustoslower")]>2.2) return false;
//        if (v[att("gustoslower2")]>2.2) return false;
//        if (v[att("gustoslower3")]>3.5) return false;

        return true;
    }


    public void addInstance(Instances mldata, boolean filtered, double w) {
        if (check(data)) {
            DenseInstance instance = new DenseInstance(w, filtered ? filterData() : data.clone());
            mldata.add(instance);
        }
    }

    public boolean classify(Classifier classifier, LogisticImprover improver, Instances testset) {
        if (true) return true;
        if (classifier == null) return false;
        if (!check(data)) return false;
        try {

            DenseInstance instance = improver.prepareInstance(filterData());
//            DenseInstance instance = new DenseInstance(1,filterData());
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

    double[] filterData() {
        double[] dd = data.clone();
//        String s = "0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1";
//        dd[0] = 0;
//        dd[1] = 0;
//        dd[2] = 0;
//        dd[3] = 0;
//        dd[4] = 0;
//        dd[10] = 0;
//        dd[16] = 0;
//        dd[19] = 0;
//        dd[20] = 0;
        return dd;
    }


    void dataForAnalyze() {
        double lower = volumes.pow2price(lowerlevel);
        data[att("minprice")] = (minprice - lower) / (buyPrice - lower);
        data[att("minprice2")] = (minprice - volumes.pow2price(lowerlevel2)) / (lower - volumes.pow2price(lowerlevel2));
        data[att("maxprice")] = (maxprice - buyPrice) / (sellPrice - buyPrice);
        if (maxprice2 > 0)
            data[att("maxprice2")] = (maxprice2 - lower) / (buyPrice - lower);
        else
            data[att("maxprice2")] = -10;

//        if (maxprice2 > 0) {
//            if (maxprice2 > sellPriceMid)
//                data[att("comeback")] = 3;
//            else if (maxprice2 > buyPrice)
//                data[att("comeback")] = 2;
//            else
//                data[att("comeback")] = 1;
//        }
//        if (cameback) {
//            if (max2 > sellPrice)
//                data[att("comeback2")] = 4;
//            else if (max2 > sellPriceMid)
//                data[att("comeback2")] = 3;
//            else if (max2 > (sellPriceMid + buyPrice) / 2)
//                data[att("comeback2")] = 2;
//            else if (max2 > buyPrice)
//                data[att("comeback2")] = 1;
//            else
//                data[att("comeback2")] = 0;
//
//        } else
//            data[att("comeback2")] = -1;

    }

    boolean goneLow = false;
    boolean goneTooLow = false;
    boolean cameback = false;
    boolean richback = false;
    double max2;
    double richbackstoploss;
    double richbackprofit = 1;

    double got = 1;

    public void correct(XBar bar) {
        if (richbackprofit == 1) {
            if (richback) {
                if (bar.getMinPrice() < richbackstoploss)
                    richbackprofit = richbackstoploss / buyPrice * 0.999;
                else if (bar.getMaxPrice() > sellPrice)
                    richbackprofit = sellPrice / buyPrice * 0.999;
                else if (bar.getMinPrice() > buyPrice) {
                    richbackstoploss = buyPrice;
//                    System.out.println("change richbackstoploss");
                }
//                if (richbackprofit!=1)
//                    System.out.println("rich "+richbackprofit);
            }
            if (goneLow && !goneTooLow && bar.getMaxPrice() > buyPrice) {
                richback = true;
                richbackstoploss = volumes.pow2price(lowerlevel);
            }
        }


        minprice = Math.min(minprice, bar.getMinPrice());
        maxprice = Math.max(maxprice, bar.getMaxPrice());

        if (minprice < (buyPrice + volumes.pow2price(lowerlevel)) / 2)
            goneLow = true;

        if (minprice < volumes.pow2price(lowerlevel)) {
            maxprice2 = Math.max(maxprice2, bar.getMaxPrice());
            goneTooLow = true;
        }


    }

    public void checks(XBar bar) {
        for (int i = 0;i<stopLosses.length;i++)
            if (!stopLossRiched[i]){
                double sl = stopLosses[i];
                int before = this.beforeStopLoss[i];
                if (before>=sellPrices.length-1) continue;
                double sell = sellPrices[before+1];
                if (sl >= bar.getMinPrice())
                    stopLossRiched[i] = true;
                else if (sell <= bar.getMaxPrice())
                    beforeStopLoss[i]++;
            }
    }
}

//public class PikesPlayer {
//
//    public static Instances play(Sheet sheet) {
//        Volumes volumes = new Volumes(sheet, false);
//        Instances all = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
//        Instances mldata = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
//        Instances test = new Instances("data", PikesAnalyzer.makeAttributes(), 10);
//        mldata.setClassIndex(mldata.numAttributes() - 1);
//        test.setClassIndex(mldata.numAttributes() - 1);
//        boolean money = true;
//        double gotmoney = 1;
//        int good = 0, bad = 0;
//        int goodtotal = 0, badtotal = 0;
//        boolean classifiedOk = false;
//        Classifier classifier = null;
//
//        double got = 0;
//
//        PikePlayerMoment moment = null;
//
//        for (int i = 0; i < sheet.size(); i++) {
//            volumes.calc(i);
//            if (i<5000) continue;
//            XBar bar = sheet.bar(i);
//            if (!(bar.getBeginTime().getMonthValue()>=2 && bar.getBeginTime().getYear()>=2018))continue;
//            boolean justbuy = false;
//            if (money) {
//                if (moment != null && moment.buyPrice >= bar.getMinPrice() && moment.buyPrice <= bar.getMaxPrice()) {
//                    money = false;
//                    justbuy = true;
//                    classifiedOk = moment.classify(classifier, test) && bar.getBeginTime().getMonthValue()>=4 && bar.getBeginTime().getYear()>=2018;
//                } else {
//                    if (i%(60*24)==0)
//                        System.out.println(i);
//                    moment = new PikePlayerMoment(sheet, volumes, i);
//
//                }
//            }
//            if (!money){
//                if (moment.stopLoss >= bar.getMinPrice()) {
//                    if (moment.stopLoss>bar.getMaxPrice()) moment.stopLoss = bar.getMaxPrice();
//                    money = true;
//                    moment.bad();
//                    if (classifiedOk) {
//                        got = moment.stopLoss / moment.buyPrice * 0.999;
//                        gotmoney *= got;
//                        bad++;
////                        System.out.println("stoploss "+(moment.stopLoss / moment.buyPrice * 0.999));
//                    }
//                    badtotal++;
//                } else if (!justbuy && moment.sellPrice <= bar.getMaxPrice()) {
//                    if (moment.sellPrice<bar.getMinPrice())
//                        moment.sellPrice = bar.getMinPrice();
//                    money = true;
//                    moment.good();
//                    if (classifiedOk) {
//                        got = moment.sellPrice / moment.buyPrice * 0.999;
//                        gotmoney *= got;
//                        good++;
//                    }
//                    goodtotal++;
//                }
//                if (money) {
//                    if (classifiedOk)
//                        System.out.println(gotmoney+" "+bar.getBeginTime()+" "+(bar.getEndTime().toEpochSecond()-moment.buyTime.toEpochSecond())/60+" got: "+(got-1)*100+"%");
//                    moment.addInstance(mldata);
//                    moment.addInstance(all);
//
////                    while (mldata.size()>62)
////                        mldata.remove(0);
//
//                    if (mldata.size() > 30)
//                        try {
//                            classifier = new Logistic();
//                            classifier.buildClassifier(mldata);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                    moment = null;
//
//                } else if (!justbuy)
//                    moment.correct(bar);
//
//
//            }
//
//
//        }
//        System.out.println(String.format("pertrade: %g, trades: %d, good %g, total %g", Math.pow(gotmoney, 1.0 / (good + bad)), good + bad, good * 1.0 / (good + bad), goodtotal*1.0/(goodtotal+badtotal)));
//        Exporter.string2file("d:/tetrislibs/wekadata/pikes.arff",all.toString());
//        return all;
//    }
//}
//
//class PikePlayerMoment {
//    double buyPrice;
//    double sellPrice;
//    double sellPriceMid;
//    double stopLoss;
//    double initialStopLoss;
//    double[] data;
//    ZonedDateTime buyTime;
//    double minprice;
//    double maxprice;
//
//    public PikePlayerMoment(Sheet sheet, Volumes volumes, int index) {
//        XBar bar = sheet.bar(index);
//        int powClose = volumes.price2pow(bar.getClosePrice());
//        int powOpen = volumes.price2pow(bar.getOpenPrice());
//        int powMin = volumes.price2pow(bar.getMinPrice());
//        int powMax = volumes.price2pow(bar.getMaxPrice());
//        Pair<double[], double[]> vv = volumes.getVolumes();
//        double[] v = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
//        int[] levels = VecUtils.listLevels(v, 5);
//        ArrayList<Double> integrals = VecUtils.integrals;
//        int levelIndex = VecUtils.findBaseInLevels(levels,powClose);
//        buyTime = bar.getEndTime();
//        if (levelIndex==0){
//            buyPrice = 0;
//            return;
//        }
//        data = new double[22];
//
//        int level = levels[levelIndex];
//        int upperlevel = VecUtils.nextLevel(levels,levelIndex,1);
//            int upperlevel2 = VecUtils.nextLevel(levels,levelIndex,2);
//            int upperlevel3 = VecUtils.nextLevel(levels,levelIndex,3);
//            int lowerlevel = VecUtils.nextLevel(levels,levelIndex,-1);
//            int lowerlevel2 = VecUtils.nextLevel(levels,levelIndex,-2);
//            // стоплос надо ставить на самом деле не на предыдущий там снизу уровень, а немного под ближним снизу
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
//            int up1 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel));
//            int down1 = sheet.whenPriceWas(index-1,volumes.pow2price(level));
//            int up2 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel2));
//            int up3 = sheet.whenPriceWas(index-1,volumes.pow2price(upperlevel3));
//            int down2 = sheet.whenPriceWas(index-1,volumes.pow2price(lowerlevel));
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
//            data[15] = integrals.get(levelIndex);
//            data[16] = integrals.get(levelIndex-1);
//            data[17] = integrals.get(levelIndex+1);
//            data[18] = integrals.get(levelIndex+2);
//
//            if (data[15]==0 && data[18]==0){
////            if (up2<down2) {
//                buyPrice = volumes.pow2price(level);
//                minprice = buyPrice;
//                maxprice = buyPrice;
//                sellPrice = volumes.pow2price(upperlevel2);
//                sellPriceMid = volumes.pow2price(upperlevel);
////                sellPrice = buyPrice + (sellPrice - buyPrice) * 0.9;
//                if (sellPrice / buyPrice > 1.1) sellPrice = buyPrice * 1.1;
//                stopLoss = volumes.pow2price(lowerlevel2-70);
////                stopLoss = volumes.pow2price((lowerlevel+level)/2);
//                if (buyPrice / stopLoss > 1.1) stopLoss = buyPrice / 1.1;
//                initialStopLoss = stopLoss;
//            } else {
//                buyPrice = 0;
//            }
//
////                        ml[8] = sheet.whenPriceWas(i,sellPrice);
////                        ml[9] = sheet.whenPriceWas(i,stoploss);
//
////                        stoploss = buyPrice + (stoploss-buyPrice)*1.1;
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
//            filterData();
//            DenseInstance instance = new DenseInstance(1, data);
//            mldata.add(instance);
//        }
//    }
//
//    public boolean classify(Classifier classifier, Instances testset) {
//        if (classifier == null) return false;
//        if (!PikesAnalyzer.check(data)) return false;
//        try {
//            filterData();
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
//
//    private void filterData() {
//        data[0] = 0;
//        data[1] = 0;
//        data[2] = 0;
//        data[3] = 0;
//        data[4] = 0;
//        data[10] = 0;
//        data[16] = 0;
//        data[19] = 0;//(minprice-initialStopLoss)/(buyPrice-initialStopLoss);
//        data[20] = 0;//(maxprice-sellPriceMid)/(sellPriceMid-buyPrice);
//    }
//
//
//    public void correct(XBar bar) {
//        minprice = Math.min(minprice,bar.getMinPrice());
//        maxprice = Math.max(maxprice,bar.getMaxPrice());
//        if (bar.getMinPrice()>sellPriceMid) stopLoss = buyPrice;
//    }
//}
