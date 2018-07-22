package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.VecUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;

public class GustosVolumes extends PriceHistogram {
    double[] sigmahashpos;
    double[] sigmahashneg;

    double descendK = 1;//0.99999;
    double fee = 0.9995;
    double substK = 0.9;
    double globalSubstK = 0.05;
    double sigmaK = 0.2;
    double loosersGoAway = 0.2;
    double winnersCallFriends = 0.2;

    public GustosVolumes(Sheet sheet, boolean calc, boolean needByDays){
        super(sheet,needByDays);
        sigmahashpos = new double[steps];
        sigmahashneg = new double[steps];
        Arrays.fill(sigmahashpos,-1);
        Arrays.fill(sigmahashneg,-1);
        if (calc)
            calc(sheet.size() - 1);
    }

    @Override
    protected void process(int index, double[] v1, double[] v2) {
        modifyBaseAndAsset(v1,v2,sheet.bar(index));
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
        if (descendK!=1) {
            VecUtils.mul(base, descendK);
            VecUtils.mul(asset, descendK);
        }

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

    @Override
    public Pair<double[],double[]> get() {

        double[] ddBase = v1.clone();
        double[] ddAsset = v2.clone();

        for (int i = 0;i<ddBase.length;i++){
            double price = pow2price(i);
            ddBase[i] /= price;
        }

        return new Pair<>(ddBase,ddAsset);
    }

    @Override
    public Pair<double[],double[]> getMoment(ZonedDateTime time) {
        int day = dayn(time);
        double[] ddBase = byDays1.get(day).clone();
        double[] ddAsset = byDays2.get(day).clone();


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
}
