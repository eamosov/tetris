package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.indicators.VecUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class Volumes {

    private static final double step = 1.0002;

    Sheet sheet;

    public static final int steps = 20000;

    double[] globalBuy = new double[steps];
    double[] globalSell = new double[steps];
    ArrayList<double[]> byDaysBuy = new ArrayList<>();
    ArrayList<double[]> byDaysSell = new ArrayList<>();
    ArrayList<double[]> byDays2Base = new ArrayList<>();
    ArrayList<double[]> byDays2Asset = new ArrayList<>();
    final double logStep;
    final int minPow;

    double descendK = 1;//0.99999;
    double fee = 0.9995;
    double substK = 0.9;
    double globalSubstK = 0.05;
    double sigmaK = 0.2;
    double loosersGoAway = 0.2;
    double winnersCallFriends = 0.2;

    private double[] dayBase = new double[steps];
    private double[] dayAsset = new double[steps];
    private int calcDayN = -1;
    private int calcIndex = 0;

    public Volumes(Sheet sheet, boolean calc){
        this.sheet = sheet;
        logStep = Math.log(step);
        minPow = (int) (Math.log(sheet.totalBar.getMinPrice()) / logStep);
        initCalc();
        if (calc) {
            calc(sheet.size() - 1);

        }
    }

    private void initCalc(){
        Arrays.fill(globalBuy,0);
        Arrays.fill(globalSell,0);
        byDaysBuy.clear();
        byDaysSell.clear();
        dayBase = new double[steps];
        dayAsset = new double[steps];

    }

    public void calc(int to) {
        for (int i = calcIndex;i<=to;i++){
            XBar bar = sheet.bar(i);

            int cdayn = dayn(bar.getEndTime());
            while (cdayn>calcDayN){
                byDaysBuy.add(globalBuy.clone());
                byDaysSell.add(globalSell.clone());
                byDays2Base.add(dayBase);
                dayBase = dayBase.clone();
                byDays2Asset.add(dayAsset);
                dayAsset = dayAsset.clone();
                calcDayN++;
            }

            if (bar.isBullish())
                addVolume(globalBuy,bar, bar.getVolume());
            else
                addVolume(globalSell,bar, bar.getVolume());

//            modifyBaseAndAsset(dayBase,dayAsset,bar);
        }
        calcIndex = to+1;
    }

    private void addVolume(double[] v, XBar bar, double volume) {
            int powmin = price2pow(bar.getMinPrice());
            int powmax = price2pow(bar.getMaxPrice());
            volume = volume / (powmax - powmin + 1);
            for (int i = powmin; i <= powmax; i++)
                v[i] += volume;
    }

    double[] sigmahashpos = new double[steps];
    double[] sigmahashneg = new double[steps];
    {
        Arrays.fill(sigmahashpos,-1);
        Arrays.fill(sigmahashneg,-1);
    }
    private double sigma(int x, int c, double k){
        int d = -(x - c);
        if (k<0){
            d = -d;
            k = -k;
        }

//        return 1.0/(1+Math.exp(d *k))*0.95+0.05;

        if (d>=0) {
            double v= sigmahashpos[d];
            if (v<0){
                v = 1.0/(1+Math.exp(d *k))*0.9 + 0.1;
                sigmahashpos[d] = v;
            }
            return v;
        } else {
            double v = sigmahashneg[-d];
            if (v<0){
                v = 1.0/(1+Math.exp(d *k))*0.9 + 0.1;
                sigmahashneg[-d] = v;
            }
            return v;

        }
    }

    private double sigmasum(double[] v, int c, double k){
        double sum = 0;
        for (int i = 0;i<steps;i++)
            if (v[i]!=0)
                sum+=v[i]*sigma(i,c,k);
        return sum;
    }

    private Pair<Double,Double> subSigma(double[] v, int c, double k, double volume, double sigmasum) {
        double higherSum = 0;
        double lowerSum = 0;
        for (int i = 0;i<steps;i++) {
            if (v[i]!=0) {
                double toSub = v[i] * sigma(i, c, k) * volume * substK / sigmasum;
                if (i>c)
                    higherSum+=toSub;
                if (i<c)
                    lowerSum+=toSub;

                v[i] -= toSub;
                if (v[i] < sigmasum * 0.0000001)
                    v[i] = 0;
            }
        }
        return new Pair<>(lowerSum,higherSum);
    }

    private Pair<Double, Double> subVolume(double[] v, int c, double volume){
        double sum = 0;
        double higherSum = 0;
        double lowerSum = 0;
        for (int i1 = 0; i1 < steps; i1++)
            sum+= v[i1];
        for (int i = 0; i< steps; i++) {
            if (v[i]!=0) {
                double toSub = v[i] * volume * substK / Math.max(1, sum);
                if (toSub>v[i]) toSub = v[i];
                if (i>c)
                    higherSum+=toSub;
                if (i<c)
                    lowerSum+=toSub;
                v[i] -= toSub;
                if (v[i] < sum * 0.0001)
                    v[i] = 0;
            }
        }
        return new Pair<Double,Double>(lowerSum,higherSum);
    }


    private void modifyBaseAndAsset(double[] base, double[] asset, XBar bar) {
        double volume = bar.getVolume();
        int powmin = price2pow(bar.getMinPrice());
        int powmax = price2pow(bar.getMaxPrice());
//        volume = 50;
//        powmax = powmin;
        volume = volume/(powmax-powmin+1);
        VecUtils.mul(base, descendK);
        VecUtils.mul(asset, descendK);

        for (int i = powmin;i<=powmax;i++) {
            // тут надо посчитать, кто скорее всего продал ассет, а кто скорее всего потратил деньги. и именно им объемы вычесть
            // а потом добавить этот объем, выкинув лузеров и забрав комиссию биржи
            // в простейшем варианте: ассет хотя отдавать те, кто купил его дешевле.
            // деньги хотят отдать те, кто продал дороже
            // кому пришлось продать ассет по плохой цене - с какой-то вероятностью больше покупать не захотят. т.е. этот объем денег просто уходит нафиг.
            // а победители - наоборот позовут друзей
            double price = pow2price(i);
            double sigmasumBase = sigmasum(base, i+5, sigmaK);
            double sigmasumAsset = sigmasum(asset, i-5, -sigmaK);
//            System.out.println(sigmasumBase+" "+sigmasumAsset+" "+volume);
//            subVolume(toSub,c,volume* globalSubstK);
            subSigma(base, i+5, sigmaK, Math.min(volume*price, sigmasumBase), sigmasumBase);
            if (volume*price > sigmasumBase)
                subVolume(base, i+5, volume*price - sigmasumBase);

            double loosers, winners;
            Pair<Double, Double> pp = subSigma(asset, i -5, -sigmaK, Math.min(volume, sigmasumAsset), sigmasumAsset);
            winners = pp.getFirst();
            loosers = pp.getSecond();
//            System.out.println(loosers+" "+winners+" "+volume);
            if (volume > sigmasumAsset) {
                pp = subVolume(asset, i - 5, volume - sigmasumAsset);
                winners += pp.getFirst();
                loosers += pp.getSecond();
            }
//            subVolume(base,i+5,volume*globalSubstK);
//            subVolume(base,i+5,volume*globalSubstK);

            base[i] += Math.max(0,(volume*fee-loosers*loosersGoAway+winners*winnersCallFriends)*price);
            asset[i] += volume*fee;
        }

    }

//    private void addComplex(double[] toAdd, double[] toSub, boolean asset, XBar bar) {
//        double k = asset ? -sigmaK : sigmaK;
//        double volume = bar.getVolume();
//
//        int powmin = price2pow(bar.getMinPrice());
//        int powmax = price2pow(bar.getMaxPrice());
//        volume = volume/(powmax-powmin+1);
//        VecUtils.mul(toSub, descendK);
//        for (int i = powmin;i<=powmax;i++) {
//
//            int c = asset ? i + 2 : i - 2;
//            subVolume(toSub,c,volume* globalSubstK);
//            double sigmasum = sigmasum(toSub, c, k);
//            double loosers;
//            loosers = subSigma(toSub, c, k, Math.min(volume, sigmasum), sigmasum);
//            if (volume > sigmasum)
//                loosers+=subVolume(toSub, c, volume - sigmasum);
//
//            toAdd[i]+=asset?volume:Math.max(0,volume*fee-loosers*loosersGoAway);
////            addVolume(toAdd, bar, asset?volume:volume*fee);
//        }
////        toAdd[pow]+= volume;
//    }

    private int dayn(ZonedDateTime time){
        return (int)((time.toEpochSecond()-sheet.bar(0).getEndTime().toEpochSecond())/(24*3600));
    }

    public Pair<double[],double[]> getMomentVolumes(ZonedDateTime time){
        int day = dayn(time);
//        System.out.println("day: "+day);
        double[] ddbuy = byDaysBuy.get(day).clone();
        double[] ddsell = byDaysSell.get(day).clone();
        int i = sheet.getBarIndex(sheet.bar(0).getEndTime().plusDays(day));

//        double[] ones = dd.clone();
//        Arrays.fill(ones,1);
//        XBar b = sheet.bar(sheet.getBarIndex(time));
//        Arrays.fill(dd,0);
//        int c = price2pow(b.getClosePrice()) - 5;
//        System.out.println(c);
//        System.out.println(Arrays.stream(ones).sum());
//        subSigma(ones, c,-0.2,50,sigmasum(ones,c,-0.2));
//        System.out.println(Arrays.stream(ones).sum());
//        return ones;

//                for (int j = 0;j<dd.length;j++){
//            dd[j] = sigma(j,price2pow(b.getClosePrice())+15,-0.2);
////            dd[j] = sigmasum(ones,price2pow(b.getClosePrice()),0.2);
//        }

        while (i<sheet.size() && sheet.bar(i).getEndTime().isBefore(time)){
            XBar bar = sheet.bar(i);
            if (bar.isBullish())
                addVolume(ddbuy,bar,bar.getVolume());
            else
                addVolume(ddsell,bar,bar.getVolume());
            i++;
        }
        return new Pair<>(ddsell,ddbuy);

    }

    public int price2pow(double price){
        int r = (int) (Math.log(price) / logStep) - minPow;
        if (r>=steps) r = steps-1;
        return r;
    }

    public double pow2price(int pow){
        return Math.exp((pow+minPow)*logStep);
    }

    public Pair<double[],double[]> getVolumes() {
        return new Pair<>(globalSell,globalBuy);
    }

    public Pair<double[],double[]> getGustosVolumes() {

        double[] ddBase = dayBase.clone();
        double[] ddAsset = dayAsset.clone();

        for (int i = 0;i<ddBase.length;i++){
            double price = pow2price(i);
            ddBase[i] /= price;
        }

        return new Pair<>(ddBase,ddAsset);
    }
    public Pair<double[],double[]> getGustosVolumes(ZonedDateTime time) {
        int day = dayn(time);
        double[] ddBase = byDays2Base.get(day).clone();
        double[] ddAsset = byDays2Asset.get(day).clone();

//        XBar b = sheet.bar(sheet.getBarIndex(time));
//        Arrays.fill(ddBase,0);
//        Arrays.fill(ddBase,0,300,1);
//        Arrays.fill(ddAsset,0);
//        Arrays.fill(ddAsset,0,300,1);
//
//        modifyBaseAndAsset(ddBase,ddAsset,b);


        int i = sheet.getBarIndex(sheet.bar(0).getEndTime().plusDays(day));
        while (i<sheet.size() && sheet.bar(i).getEndTime().isBefore(time)){
            modifyBaseAndAsset(ddBase,ddAsset,sheet.bar(i));
            i++;
        }
        for (i = 0;i<ddBase.length;i++){
            double price = pow2price(i);
            ddBase[i] /= price;
        }

        return new Pair<>(ddBase,ddAsset);
    }

    public double lastDay() {
        int n = byDays2Asset.size()-1;
        return Arrays.stream(byDays2Asset.get(n)).sum()+Arrays.stream(byDays2Base.get(n)).sum()-(Arrays.stream(byDays2Asset.get(n-1)).sum()+Arrays.stream(byDays2Base.get(n-1)).sum());
    }
}


